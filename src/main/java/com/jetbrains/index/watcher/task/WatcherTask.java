package com.jetbrains.index.watcher.task;

import com.jetbrains.index.watcher.ChangeType;
import com.jetbrains.index.watcher.DefaultFileEvent;
import com.jetbrains.index.watcher.FileChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Consumer;

/**
 * Task responsible for reacting to file system changes.
 * Given a set of paths, this task will produce events that indicate when
 * 1) a new file is created
 * 2) a file has changed
 * 3) a file has been deleted
 * <p>
 * Detection if a file has been changed is done with a combination of FS last modified time
 * and a digest of the contents of the file (a user can just save a file without updating it)
 */
public class WatcherTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(WatcherTask.class);
    private static final int FILE_CHUNK_SIZE = 1024 * 1024 * 100; //100 Kb
    private static final int SLEEP_TIME_MS = 100;

    private final Set<String> originalPaths;
    private final Consumer<FileChangeEvent> eventPublisher;
    private final ConcurrentHashMap<File, Inspection> fileStatus;
    private volatile Instant lastInvocation = Instant.MIN;

    public WatcherTask(Collection<String> originalPaths, Consumer<FileChangeEvent> eventPublisher) {
        this.originalPaths = new HashSet<>(originalPaths);
        this.eventPublisher = eventPublisher;
        this.fileStatus = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        log.info("Started watcher task");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                detectChanges();
                detectDeletedFiles();
            } catch (InterruptedException e) {
                log.warn("Watcher task loop interrupted", e);
            }
            pause();
        }
        log.info("Final paths: {}", fileStatus.keySet());
        log.info("Original paths: {}", originalPaths);
    }

    /**
     * Check if any of the files which were previously encountered
     * got deleted
     *
     * @throws InterruptedException if interrupted while publishing {@link ChangeType#DELETE}
     */
    private void detectDeletedFiles() throws InterruptedException {
        for (File f : fileStatus.keySet()) {
            if (!f.exists()) {
                log.trace("Deleting file {}", f);
                fileStatus.remove(f);
                this.publishDeletion(f.getAbsolutePath());
            }
        }
    }


    /**
     * Detect if any of the files has changed by starting a virtual thread
     * for every file in the specified {@code originalPaths} recursively descending
     * into directories.
     */
    private void detectChanges() {
        try (var scope = new StructuredTaskScope<Inspection>()) {
            for (String path : originalPaths) {
                //Since reading files from Filesystem is IO operation
                //Use virtual threads
                openPath(path, (file) -> scope.fork(() -> inspect(file)));
            }

            //wait for all tasks to execute before ending the loop
            scope.join();
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            this.lastInvocation = Instant.now();
        }

    }

    private void pause() {
        try {
            Thread.sleep(SLEEP_TIME_MS);
        } catch (InterruptedException e) {
            log.error("Interrupted while sleeping", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Based on the provided {@param path} either apply the {@link Consumer} to it if
     * it is an actual file or recursively iterate all sub paths of the directory
     *
     * @param path  on the file system which resolves either to a directory or a file
     * @param apply a function which is applied to every file
     */
    private void openPath(String path, Consumer<File> apply) {
        File filePath = Paths.get(path).toFile();
        if (filePath.isDirectory()) {
            var subPaths = filePath.listFiles();
            if (subPaths == null)
                return;
            log.trace("Opening directory {}", filePath);
            for (File subPath : subPaths) {
                if (subPath.isDirectory()) {
                    openPath(subPath.getAbsolutePath(), apply);
                } else {
                    log.trace("Opening sub file {}", subPath);
                    apply.accept(subPath);
                }
            }
        } else if (filePath.isFile()) {
            log.trace("Opening file {}", filePath);
            apply.accept(filePath);
        }
    }


    /**
     * Inspection of a given {@param path} for changes. The path must be an actual file.
     * Method derives an SHA-256 digest of the file contents, reading it in chunks of size
     * {@code FILE_CHUNK_SIZE}
     *
     * @param path path to an actual file
     * @return {@link Inspection} containing the digest and a {@link File}
     */
    private Inspection inspect(File path) {
        var file = Objects.requireNonNull(path);
        if (!file.isFile()) {
            log.error("Not a file: {} in inspection", path);
            throw new IllegalArgumentException(String.format("Path: %s is not a file", path));
        }


        var existingFile = fileStatus.get(file);
        if (existingFile != null && !Instant.ofEpochMilli(existingFile.file.lastModified()).isAfter(existingFile.lastInspection)) {
            //Skip files which were not updated since last invocation
            return null;
        }

        log.trace("Inspecting {}", path.getAbsolutePath());

        try {

            byte[] buffer = new byte[FILE_CHUNK_SIZE];
            int bytesRead;
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file)) {
                while ((bytesRead = fis.read(buffer)) != -1) {
                    messageDigest.update(buffer, 0, bytesRead);
                }
            }
            var inspection = new Inspection(messageDigest.digest(), file, Instant.now());
            checkFile(inspection);
            return inspection;
        } catch (FileNotFoundException | NoSuchAlgorithmException e) {
            log.error("Unable to inspect file {}", path, e);
        } catch (IOException e) {
            log.error("Error reading file chunk {}", path, e);
        }
        return null;
    }


    /**
     * Method saves a new file, and it's digest, or determines if an already existing
     * file should be updated by comparing the digest outputs. If a file
     *
     * @param inspection {@link Inspection}
     */
    private void checkFile(Inspection inspection) {
        var file = inspection.file;
        var alreadyPresent = this.fileStatus.putIfAbsent(file, inspection);

        if (alreadyPresent != null) {
            //Only update the file status if digests do not match
            int result = Arrays.compare(alreadyPresent.digest, inspection.digest);
            if (result != 0) {
                log.info("Updating file {}", inspection.file);
                this.fileStatus.put(file, inspection);
                publishFileUpdate(file.getAbsolutePath());
            }
        } else {
            log.info("Adding file {}", inspection.file);
            publishNewFile(file.getAbsolutePath());
        }
    }

    /**
     * Specialized publishing of {@link ChangeType#CREATE}
     *
     * @param path of the created file
     */
    private void publishNewFile(String path) {
        publishEvent(path, ChangeType.CREATE);
    }

    /**
     * Specialized publishing of {@link ChangeType#UPDATE}
     *
     * @param path of the updated file
     */
    public void publishFileUpdate(String path) {
        publishEvent(path, ChangeType.UPDATE);
    }

    /**
     * Specialized publishing of {@link ChangeType#DELETE}
     *
     * @param path of the deleted file
     */
    private void publishDeletion(String path) {
        publishEvent(path, ChangeType.DELETE);
    }

    /**
     * Generic publishing ov events
     *
     * @param path       associated file
     * @param changeType one of {@link ChangeType}
     */
    private void publishEvent(String path, ChangeType changeType) {
        DefaultFileEvent event = new DefaultFileEvent(path, changeType);
        eventPublisher.accept(event);
    }

    record Inspection(byte[] digest, File file, Instant lastInspection) {
    }

}
