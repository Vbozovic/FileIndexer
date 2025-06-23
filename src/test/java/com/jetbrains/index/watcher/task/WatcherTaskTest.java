package com.jetbrains.index.watcher.task;

import com.jetbrains.index.watcher.ChangeType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class WatcherTaskTest extends BaseTest {

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
        var thread = oneDirectoryWatcher(TEST_DIRECTORY_PATH, events::add);
        thread.setName("Watcher test trd");
        thread.start();
        Thread.sleep(200);

        createTestFile("test1.txt");
        createTestFile("test2.txt");
        createTestFile("test3.txt");

        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()-> Assertions.assertEquals(3, events.size()));
        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()-> Assertions.assertTrue(events.contains(event(testFileString("test1.txt"), ChangeType.CREATE))));
        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()-> Assertions.assertTrue(events.contains(event(testFileString("test2.txt"), ChangeType.CREATE))));
        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()-> Assertions.assertTrue(events.contains(event(testFileString("test3.txt"), ChangeType.CREATE))));

        thread.interrupt();
        thread.join(Duration.ofMillis(200));
    }

    @Test
    void watcherReactsToFileWrites() throws IOException, InterruptedException {
        createTestFile("test1.txt");
        createTestFile("test2.txt");
        createTestFile("test3.txt");

        var events = new ConcurrentLinkedDeque<>();
        var thread = oneDirectoryWatcher(TEST_DIRECTORY_PATH, events::add);
        thread.start();
        Thread.sleep(200);

        updateFile(testFile("test1.txt"));
        updateFile(testFile("test2.txt"));
        updateFile(testFile("test3.txt"));

        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()-> Assertions.assertEquals(6, events.size()));
        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()-> Assertions.assertTrue(events.contains(event(testFileString("test1.txt"), ChangeType.UPDATE))));
        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()-> Assertions.assertTrue(events.contains(event(testFileString("test2.txt"), ChangeType.UPDATE))));
        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()-> Assertions.assertTrue(events.contains(event(testFileString("test3.txt"), ChangeType.UPDATE))));

        thread.interrupt();
        thread.join(Duration.ofMillis(200));
    }

    @Test
    void watcherReactsToFileDeletes() throws IOException, InterruptedException {
        createTestFile("test1.txt");
        createTestFile("test2.txt");
        createTestFile("test3.txt");

        var events = new ConcurrentLinkedDeque<>();
        var thread = oneDirectoryWatcher(TEST_DIRECTORY_PATH, events::add);
        thread.start();
        Thread.sleep(200);

        deleteFile(testFile("test1.txt"));
        deleteFile(testFile("test2.txt"));
        deleteFile(testFile("test3.txt"));


        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()->Assertions.assertEquals(6, events.size()));
        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()->Assertions.assertTrue(events.contains(event(testFileString("test1.txt"), ChangeType.CREATE))));
        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()->Assertions.assertTrue(events.contains(event(testFileString("test2.txt"), ChangeType.CREATE))));
        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()->Assertions.assertTrue(events.contains(event(testFileString("test3.txt"), ChangeType.CREATE))));

        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()->Assertions.assertTrue(events.contains(event(testFileString("test1.txt"), ChangeType.DELETE))));
        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()->Assertions.assertTrue(events.contains(event(testFileString("test2.txt"), ChangeType.DELETE))));
        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()->Assertions.assertTrue(events.contains(event(testFileString("test3.txt"), ChangeType.DELETE))));

        thread.interrupt();
        thread.join(Duration.ofMillis(200));
    }

}
