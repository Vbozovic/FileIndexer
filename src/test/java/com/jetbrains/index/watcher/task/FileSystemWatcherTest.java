package com.jetbrains.index.watcher.task;


import com.jetbrains.index.watcher.ChangeType;
import com.jetbrains.index.watcher.FSListener;
import com.jetbrains.index.watcher.FileSystemWatcher;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;

public class FileSystemWatcherTest extends BaseTest{

    @Test
    void watcherEmitsCreatedEvent() throws InterruptedException {
        var listener = Mockito.mock(FSListener.class);
        try(FileSystemWatcher watcher = new FileSystemWatcher(Collections.singletonList(TEST_DIRECTORY_PATH.toString()))){
            watcher.registerListener(listener);
            watcher.start();

            Thread.sleep(200);
            createTestFile("test1.txt");
            createTestFile("test2.txt");
            createTestFile("test3.txt");

            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()->Mockito.verify(listener,Mockito.times(1)).onFileChanged(event(testFileString("test1.txt"), ChangeType.CREATE)));
            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()->Mockito.verify(listener,Mockito.times(1)).onFileChanged(event(testFileString("test2.txt"), ChangeType.CREATE)));
            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()->Mockito.verify(listener,Mockito.times(1)).onFileChanged(event(testFileString("test3.txt"), ChangeType.CREATE)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void watcherEmitsDeletedEvent() throws IOException, InterruptedException {
        var listener = Mockito.mock(FSListener.class);

        createTestFile("deleteTest1.txt");
        createTestFile("deleteTest2.txt");
        createTestFile("deleteTest3.txt");


        try(FileSystemWatcher watcher = new FileSystemWatcher(Collections.singletonList(TEST_DIRECTORY_PATH.toString()))){
            watcher.registerListener(listener);
            watcher.start();
            Thread.sleep(200);

            deleteFile(testFile("deleteTest1.txt"));
            deleteFile(testFile("deleteTest2.txt"));
            deleteFile(testFile("deleteTest3.txt"));

            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()->Mockito.verify(listener,Mockito.times(1)).onFileChanged(event(testFileString("deleteTest1.txt"), ChangeType.CREATE)));
            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()->Mockito.verify(listener,Mockito.times(1)).onFileChanged(event(testFileString("deleteTest2.txt"), ChangeType.CREATE)));
            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()->Mockito.verify(listener,Mockito.times(1)).onFileChanged(event(testFileString("deleteTest3.txt"), ChangeType.CREATE)));

            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()->Mockito.verify(listener,Mockito.times(1)).onFileChanged(event(testFileString("deleteTest1.txt"), ChangeType.DELETE)));
            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()->Mockito.verify(listener,Mockito.times(1)).onFileChanged(event(testFileString("deleteTest2.txt"), ChangeType.DELETE)));
            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()->Mockito.verify(listener,Mockito.times(1)).onFileChanged(event(testFileString("deleteTest3.txt"), ChangeType.DELETE)));

        }
    }

    @Test
    void watcherEmitsUpdatedEventOnFileUpdatedWithNewContent() throws InterruptedException, IOException {
        var listener = Mockito.mock(FSListener.class);

        createTestFile("updateTest1.txt","File content1");
        createTestFile("updateTest2.txt","File content2");
        createTestFile("updateTest3.txt","File content3");
        try(var watcher = new FileSystemWatcher(Collections.singletonList(TEST_DIRECTORY_PATH.toString()))){
            watcher.registerListener(listener);
            watcher.start();
            Thread.sleep(200);

            writeFile("updateTest1.txt","Some other content1");
            writeFile("updateTest2.txt","Some other content2");
            writeFile("updateTest3.txt","Some other content3");

            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()-> Mockito.verify(listener,Mockito.times(1)).onFileChanged(event(testFileString("updateTest1.txt"), ChangeType.UPDATE)));
            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()-> Mockito.verify(listener,Mockito.times(1)).onFileChanged(event(testFileString("updateTest2.txt"), ChangeType.UPDATE)));
            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()-> Mockito.verify(listener,Mockito.times(1)).onFileChanged(event(testFileString("updateTest3.txt"), ChangeType.UPDATE)));

        }

    }

    @Test
    void watcherDoesNotEmitUpdateEventOnFileContentBeingUpdatedWithSameContent() throws InterruptedException, IOException {
        var listener = Mockito.mock(FSListener.class);

        createTestFile("updateTest1.txt","File content1");
        createTestFile("updateTest2.txt","File content2");
        createTestFile("updateTest3.txt","File content3");
        try(var watcher = new FileSystemWatcher(Collections.singletonList(TEST_DIRECTORY_PATH.toString()))){
            watcher.registerListener(listener);
            watcher.start();
            Thread.sleep(200);

            writeFile("updateTest1.txt","File content1");
            writeFile("updateTest2.txt","File content2");
            writeFile("updateTest3.txt","File content3");

            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()-> Mockito.verify(listener,Mockito.times(1)).onFileChanged(event(testFileString("updateTest1.txt"), ChangeType.CREATE)));
            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()-> Mockito.verify(listener,Mockito.times(1)).onFileChanged(event(testFileString("updateTest2.txt"), ChangeType.CREATE)));
            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()-> Mockito.verify(listener,Mockito.times(1)).onFileChanged(event(testFileString("updateTest3.txt"), ChangeType.CREATE)));

            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()-> Mockito.verify(listener,Mockito.times(0)).onFileChanged(event(testFileString("updateTest1.txt"), ChangeType.UPDATE)));
            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()-> Mockito.verify(listener,Mockito.times(0)).onFileChanged(event(testFileString("updateTest2.txt"), ChangeType.UPDATE)));
            Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(()-> Mockito.verify(listener,Mockito.times(0)).onFileChanged(event(testFileString("updateTest3.txt"), ChangeType.UPDATE)));

        }
    }

}
