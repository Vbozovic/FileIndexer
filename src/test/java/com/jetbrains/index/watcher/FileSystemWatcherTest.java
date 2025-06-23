package com.jetbrains.index.watcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

public class FileSystemWatcherTest {

    private static Path TEST_DIRECTORY_PATH;

    @BeforeEach
    void createTestDirectory() throws IOException {
        TEST_DIRECTORY_PATH = Files.createTempDirectory("test");
    }

    @AfterEach
    void deleteAllTestFiles() throws IOException {
        Files.walkFileTree(TEST_DIRECTORY_PATH, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        Files.deleteIfExists(TEST_DIRECTORY_PATH);
    }

    @Test
    void watcherThreadTerminatesDuringSleep() throws InterruptedException {
        Thread t = new Thread(new WatcherTask(List.of(TEST_DIRECTORY_PATH.toString()), System.out::println));
        t.start();
        Thread.sleep(200);
        Assertions.assertTrue(t.isAlive());
        t.interrupt();
        Thread.sleep(100);
        Assertions.assertFalse(t.isAlive());
        Assertions.assertTrue(t.isInterrupted());
        t.join(Duration.ofMillis(100));
    }

    @Test
    void watcherReactsToCreatedFiles() throws IOException, InterruptedException {
        var events = new ConcurrentLinkedDeque<>();
        var thread = oneDirectoryWatcher(TEST_DIRECTORY_PATH,events::add);
        thread.setName("Watcher test trd");
        thread.start();

        Thread.sleep(200);
        createTestFile("test1.txt");
        createTestFile("test2.txt");
        createTestFile("test3.txt");
        Thread.sleep(200);

        Assertions.assertEquals(3,events.size());
        Assertions.assertTrue(events.contains(event(testFileString("test1.txt"),ChangeType.CREATE)));
        Assertions.assertTrue(events.contains(event(testFileString("test2.txt"),ChangeType.CREATE)));
        Assertions.assertTrue(events.contains(event(testFileString("test3.txt"),ChangeType.CREATE)));

        thread.interrupt();
        thread.join(Duration.ofMillis(200));
    }

    @Test
    void watcherReactsToFileWrites() throws IOException, InterruptedException {
        createTestFile("test1.txt");
        createTestFile("test2.txt");
        createTestFile("test3.txt");

        var events = new ConcurrentLinkedDeque<>();
        var thread = oneDirectoryWatcher(TEST_DIRECTORY_PATH,events::add);
        Thread.sleep(100);
        thread.start();
        Thread.sleep(100);
        updateFile(testFile("test1.txt"));
        updateFile(testFile("test2.txt"));
        updateFile(testFile("test3.txt"));
        Thread.sleep(100);

        Assertions.assertEquals(6,events.size());
        Assertions.assertTrue(events.contains(event(testFileString("test1.txt"),ChangeType.CREATE)));
        Assertions.assertTrue(events.contains(event(testFileString("test2.txt"),ChangeType.CREATE)));
        Assertions.assertTrue(events.contains(event(testFileString("test3.txt"),ChangeType.CREATE)));

        Assertions.assertTrue(events.contains(event(testFileString("test1.txt"),ChangeType.UPDATE)));
        Assertions.assertTrue(events.contains(event(testFileString("test2.txt"),ChangeType.UPDATE)));
        Assertions.assertTrue(events.contains(event(testFileString("test3.txt"),ChangeType.UPDATE)));

        thread.interrupt();
        thread.join(Duration.ofMillis(200));
    }

    @Test
    void watcherReactsToFileDeletes() throws IOException, InterruptedException {
        createTestFile("test1.txt");
        createTestFile("test2.txt");
        createTestFile("test3.txt");

        var events = new ConcurrentLinkedDeque<>();
        var thread = oneDirectoryWatcher(TEST_DIRECTORY_PATH,events::add);
        Thread.sleep(100);
        thread.start();
        Thread.sleep(100);
        deleteFile(testFile("test1.txt"));
        deleteFile(testFile("test2.txt"));
        deleteFile(testFile("test3.txt"));
        Thread.sleep(100);

        Assertions.assertEquals(6,events.size());
        Assertions.assertTrue(events.contains(event(testFileString("test1.txt"),ChangeType.CREATE)));
        Assertions.assertTrue(events.contains(event(testFileString("test2.txt"),ChangeType.CREATE)));
        Assertions.assertTrue(events.contains(event(testFileString("test3.txt"),ChangeType.CREATE)));

        Assertions.assertTrue(events.contains(event(testFileString("test1.txt"),ChangeType.DELETE)));
        Assertions.assertTrue(events.contains(event(testFileString("test2.txt"),ChangeType.DELETE)));
        Assertions.assertTrue(events.contains(event(testFileString("test3.txt"),ChangeType.DELETE)));

        thread.interrupt();
        thread.join(Duration.ofMillis(200));
    }

    private static FileChangeEvent event(String path,ChangeType type){
        return new DefaultFileEvent(path,type);
    }

    private static Path testFile(String fileName){
        return TEST_DIRECTORY_PATH.resolve(fileName);
    }

    private static void createTestFile(String fileName) throws IOException {
        Files.createFile(testFile(fileName));
    }

    private static String testFileString(String fileName){
        return testFile(fileName).toString();
    }

    private static Thread oneDirectoryWatcher(Path path, Consumer<FileChangeEvent> consumer){
        var thread = new Thread(new WatcherTask(List.of(path.toString()),consumer));
        thread.setName("Watcher test trd");
        return thread;
    }

    private static void updateFile(Path path) throws IOException {
        Files.write(path,"Test insert".getBytes(), StandardOpenOption.WRITE);
    }

    private static void deleteFile(Path path) throws IOException {
        Files.delete(path);
    }

}
