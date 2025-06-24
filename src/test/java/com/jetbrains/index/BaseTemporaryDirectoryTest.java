package com.jetbrains.index;

import com.jetbrains.index.watcher.ChangeType;
import com.jetbrains.index.watcher.DefaultFileEvent;
import com.jetbrains.index.watcher.FileChangeEvent;
import com.jetbrains.index.watcher.task.WatcherTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Consumer;

public abstract class BaseTemporaryDirectoryTest {

    protected static Path TEST_DIRECTORY_PATH;

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


    protected static FileChangeEvent event(String path, ChangeType type){
        return new DefaultFileEvent(path,type);
    }

    protected static Path testFile(String fileName){
        return TEST_DIRECTORY_PATH.resolve(fileName);
    }

    protected static void createTestFile(String fileName) throws IOException {
        createTestFile(fileName,null);
    }

    protected static void createTestFile(String filename,String content) throws IOException {
        var file = Files.createFile(testFile(filename));
        if(content != null && !content.isEmpty()){
            Files.write(file, content.getBytes());
        }
    }

    protected static void writeFile(String filename, String content) throws IOException {
        Files.write(testFile(filename), content.getBytes());
    }

    protected static String testFileString(String fileName){
        return testFile(fileName).toString();
    }

    protected static Thread oneDirectoryWatcher(Path path, Consumer<FileChangeEvent> consumer){
        var thread = new Thread(new WatcherTask(List.of(path.toString()),consumer));
        thread.setName("Watcher test trd");
        return thread;
    }

    protected static void updateFile(Path path) throws IOException {
        Files.write(path,"Test insert".getBytes(), StandardOpenOption.WRITE);
    }

    protected static void deleteFile(Path path) throws IOException {
        Files.delete(path);
    }

}
