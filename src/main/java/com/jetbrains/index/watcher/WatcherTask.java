package com.jetbrains.index.watcher;

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
 *
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
    private Instant lastInvocation = Instant.MIN;

    public WatcherTask(Collection<String> originalPaths,Consumer<FileChangeEvent> eventPublisher) {
        this.originalPaths = new HashSet<>(originalPaths);
        this.eventPublisher = eventPublisher;
        this.fileStatus = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        log.info("Started watcher task");
        while (!Thread.currentThread().isInterrupted()) {
            detectChanges();
            detectDeletedFiles();
            pause();
        }
    }

    private void detectDeletedFiles() {
        fileStatus.keySet()
                .stream()
                .filter(f -> !f.exists())
                .forEach(f -> {
                    fileStatus.remove(f);
                    this.publishDeletion(f.getAbsolutePath());
                });
    }


    /**
     * Detect if any of the files has changed by starting a virtual thread
     * for every file in the specified {@code originalPaths} recursively descending
     * into directories.
     */
    private void detectChanges() {
        try (var scope = new StructuredTaskScope<Inspection>()) {
            for (String path : originalPaths) {
                openPath(path, (file) -> scope.fork(() -> inspect(file)));
            }

            //wait for all tasks to execute before ending the loop
            scope.join();
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
            Thread.currentThread().interrupt();
        }
        this.lastInvocation = Instant.now();
    }

    private void pause() {
        try {
            Thread.sleep(SLEEP_TIME_MS);
        } catch (InterruptedException e) {
            log.error("Interrupted while sleeping", e);
            Thread.currentThread().interrupt();
        }
    }

    private void openPath(String path, Consumer<File> apply) {
        File filePath = Paths.get(path).toFile();
        if (filePath.isDirectory()) {
            var subPaths = filePath.listFiles();
            if(subPaths == null)
                return;

            for (File subPath : subPaths) {
                if (subPath.isDirectory()) {
                    openPath(subPath.getAbsolutePath(), apply);
                } else {
                    apply.accept(subPath);
                }
            }
        } else if (filePath.isFile()) {
            apply.accept(filePath);
        }
    }


    /**
     * Inspection of a given {@param path} for changes. The path must be an actual file.
     * Method derives an SHA-256 digest of the file contents, reading it in chunks of size
     * {@code FILE_CHUNK_SIZE}
     * @param path path to an actual file
     * @return {@link Inspection} containing the digest and a {@link File}
     */
    private Inspection inspect(File path) {
        var file = Objects.requireNonNull(path);
        if (!file.isFile()) {
            log.error("Not a file: {} in inspection", path);
            throw new IllegalArgumentException(String.format("Path: %s is not a file", path));
        }

        //Skip files which were not updated since last invocation
        if (!Instant.ofEpochMilli(file.lastModified()).isAfter(lastInvocation)) {
            log.trace("Skip file {}", path);
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
            var inspection = new Inspection(messageDigest.digest(), file);
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
                this.fileStatus.put(file, inspection);
                publishFileUpdate(file.getAbsolutePath());
            }
        } else {
            publishNewFile(file.getAbsolutePath());
        }
    }

    private void publishNewFile(String path) {
       publishEvent(path,ChangeType.CREATE);
    }

    public void publishFileUpdate(String path){
        publishEvent(path,ChangeType.UPDATE);
    }

    private void publishDeletion(String path) {
        publishEvent(path,ChangeType.DELETE);
    }

    private void publishEvent(String path,ChangeType changeType) {
        DefaultFileEvent event = new DefaultFileEvent(path, changeType);
        log.info("Publishing event {}",event);
        eventPublisher.accept(event);
    }

    record Inspection(byte[] digest, File file) {
    }

}
