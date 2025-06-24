package com.jetbrains.index.index;

import com.jetbrains.index.BaseTemporaryDirectoryTest;
import com.jetbrains.index.token.factory.CachingTokenFactory;
import com.jetbrains.index.token.tokenizer.WhiteSpaceTokenizer;
import com.jetbrains.index.watcher.FileSystemWatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Full end-to-end tests for the task.
 * Start watching the filesystem and react to changes updating the index
 * while answering index searches
 */
public class SearchServiceTest extends BaseTemporaryDirectoryTest {

    /**
     * Happy path test with a secret word at a predetermined file
     */
    @Test
    void indexGetsPopulated() throws Exception {
        try (var watcher = new FileSystemWatcher(List.of(TEST_DIRECTORY_PATH.toString()))) {
            IndexSearchService svc = new IndexSearchService(new WhiteSpaceTokenizer(CachingTokenFactory.getInstance()));
            watcher.registerListener(svc);
            watcher.start();

            Files.copy(Path.of("src/test/resources/TestFile1.txt"), testFile("TestFile1.txt"));
            Files.copy(Path.of("src/test/resources/TestFile2.txt"), testFile("TestFile2.txt"));
            Files.copy(Path.of("src/test/resources/TestFile3.txt"), testFile("TestFile3.txt"));

            Thread.sleep(400);

            var hail = svc.findWord("Hail");
            var hydra = svc.findWord("Hydra");

            Assertions.assertEquals(1, hail.size());
            Assertions.assertEquals(1, hydra.size());

            Assertions.assertTrue(hail.contains(testFileString("TestFile2.txt")));
            Assertions.assertTrue(hydra.contains(testFileString("TestFile2.txt")));


            svc.close();
        }
    }

    /**
     * Verifies that the index is capable of finding predetermined word in more than one file
     */
    @Test
    void singleWordInMultipleFiles() throws IOException, InterruptedException {
        try (var watcher = new FileSystemWatcher(List.of(TEST_DIRECTORY_PATH.toString()))) {
            IndexSearchService svc = new IndexSearchService(new WhiteSpaceTokenizer(CachingTokenFactory.getInstance()));
            watcher.registerListener(svc);
            watcher.start();

            Files.copy(Path.of("src/test/resources/CthulhuPlot.txt"), testFile("CthulhuPlot.txt"));
            Files.copy(Path.of("src/test/resources/TestFile1.txt"), testFile("TestFile1.txt"));
            Files.copy(Path.of("src/test/resources/TestFile2.txt"), testFile("TestFile2.txt"));
            Files.copy(Path.of("src/test/resources/TestFile3.txt"), testFile("TestFile3.txt"));
            Thread.sleep(400);

            var files = svc.findWord("My");
            Assertions.assertEquals(2, files.size());
            Assertions.assertTrue(files.contains(testFileString("TestFile2.txt")));
            Assertions.assertTrue(files.contains(testFileString("TestFile3.txt")));

            svc.close();
        }
    }


    /**
     * Verifies that index reacts to deletions of files in the file system
     */
    @Test
    void removeFilesWhileIndexIsRunning() throws Exception {
        try (var watcher = new FileSystemWatcher(List.of(TEST_DIRECTORY_PATH.toString()))) {
            IndexSearchService svc = new IndexSearchService(new WhiteSpaceTokenizer(CachingTokenFactory.getInstance()));
            watcher.registerListener(svc);
            watcher.start();

            Files.copy(Path.of("src/test/resources/10Words.txt"), testFile("10Words.txt"));
            Files.copy(Path.of("src/test/resources/CthulhuPlot.txt"), testFile("CthulhuPlot.txt"));

            Thread.sleep(400);

            //Verify words are present in index
            var coordinatesSouth = svc.findWord("47°9′S");
            var coordinatesWidth = svc.findWord("126°43′W");

            Assertions.assertEquals(1, coordinatesSouth.size());
            Assertions.assertEquals(1, coordinatesWidth.size());

            Assertions.assertTrue(coordinatesSouth.contains(testFileString("CthulhuPlot.txt")));
            Assertions.assertTrue(coordinatesWidth.contains(testFileString("CthulhuPlot.txt")));

            //Remove file containing words
            Files.delete(testFile("CthulhuPlot.txt"));
            Thread.sleep(200);

            //Verify index no longer contains words
            coordinatesSouth = svc.findWord("47°9′S");
            coordinatesWidth = svc.findWord("126°43′W");

            Assertions.assertTrue(coordinatesSouth.isEmpty());
            Assertions.assertTrue(coordinatesWidth.isEmpty());

            svc.close();
        }
    }

    /**
     * Verifies that the index does not have any residual state
     * for a file that might affect its behavior
     */
    @Test
    void deleteFileFromIndexAndCreateOneWithSameNameButDifferentContent() throws InterruptedException, IOException {
        try (var watcher = new FileSystemWatcher(List.of(TEST_DIRECTORY_PATH.toString()))) {
            IndexSearchService svc = new IndexSearchService(new WhiteSpaceTokenizer(CachingTokenFactory.getInstance()));
            watcher.registerListener(svc);
            watcher.start();

            Files.copy(Path.of("src/test/resources/10Words.txt"), testFile("10Words.txt"));
            Files.copy(Path.of("src/test/resources/CthulhuPlot.txt"), testFile("CthulhuPlot.txt"));

            Thread.sleep(400);

            //Verify words are present in index
            var color = svc.findWord("greenish-black");
            Assertions.assertEquals(1, color.size());

            Assertions.assertTrue(color.contains(testFileString("CthulhuPlot.txt")));

            //Remove file containing words
            Files.delete(testFile("CthulhuPlot.txt"));
            Files.writeString(testFile("CthulhuPlot.txt"),"Unicorn",StandardOpenOption.CREATE_NEW);
            Thread.sleep(200);

            color = svc.findWord("greenish-black");
            Assertions.assertEquals(0, color.size());

            var corn = svc.findWord("Unicorn");
            Assertions.assertEquals(1, corn.size());
            Assertions.assertTrue(corn.contains(testFileString("CthulhuPlot.txt")));

            svc.close();
        }
    }

    /**
     * Verifies that the index reacts to changes in the files
     */
    @Test
    void appendToEndOfTheFile() throws Exception {
        try (var watcher = new FileSystemWatcher(List.of(TEST_DIRECTORY_PATH.toString()))) {
            IndexSearchService svc = new IndexSearchService(new WhiteSpaceTokenizer(CachingTokenFactory.getInstance()));
            watcher.registerListener(svc);
            watcher.start();

            Files.copy(Path.of("src/test/resources/CthulhuPlot.txt"), testFile("CthulhuPlot.txt"));
            Files.copy(Path.of("src/test/resources/TestFile1.txt"), testFile("TestFile1.txt"));
            Files.copy(Path.of("src/test/resources/TestFile2.txt"), testFile("TestFile2.txt"));
            Files.copy(Path.of("src/test/resources/TestFile3.txt"), testFile("TestFile3.txt"));
            Thread.sleep(400);

            //verify string wasn't present in CthulhuPlot.txt
            var vuk = svc.findWord("Vuk");
            Assertions.assertTrue(vuk.isEmpty());

            //Append string to the end of the file
            Files.writeString(testFile("CthulhuPlot.txt"), "Vuk", StandardOpenOption.APPEND);
            Thread.sleep(100);

            vuk = svc.findWord("Vuk");
            //Oh no...
            Assertions.assertFalse(vuk.isEmpty());

            svc.close();
        }
    }

}
