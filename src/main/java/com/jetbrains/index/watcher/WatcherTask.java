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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Consumer;

public class WatcherTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(WatcherTask.class);
    private static final int FILE_CHUNK_SIZE = 1024 * 1024 * 100; //100 Kb
    private static final int SLEEP_TIME_MS = 100;

    private final List<String> originalPaths;
    private Instant lastInvocation = Instant.MIN;
    private ConcurrentHashMap<File, Inspection> fileStatus;

    public WatcherTask(List<String> originalPaths) {
        this.originalPaths = originalPaths;
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


    private Inspection inspect(File path) {
        var file = Objects.requireNonNull(path);
        if (!file.isFile()) {
            log.error("Not a file: {}", path);
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
            publishInspection(inspection);
            return inspection;
        } catch (FileNotFoundException | NoSuchAlgorithmException e) {
            log.error("Unable to inspect file {}", path, e);
        } catch (IOException e) {
            log.error("Error reading file chunk {}", path, e);
        }
        return null;
    }


    private void publishInspection(Inspection inspection) {
        var file = inspection.file;
        var alreadyPresent = this.fileStatus.putIfAbsent(file, inspection);

        if (alreadyPresent != null) {
            //Only update the file status if digests do not match
            int result = Arrays.compare(alreadyPresent.digest, inspection.digest);
            if (result != 0) {
                log.info("Updating file {}", file.getAbsoluteFile());
                this.fileStatus.put(file, inspection);
            }
        } else {
            log.info("Adding file {}", file.getAbsoluteFile());
        }
    }

    private void publishDeletion(String path) {
        log.info("Deleting file {}", path);
    }


    record Inspection(byte[] digest, File file) {
    }

}
