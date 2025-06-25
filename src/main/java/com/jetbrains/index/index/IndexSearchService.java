package com.jetbrains.index.index;

import com.jetbrains.index.token.Token;
import com.jetbrains.index.token.factory.SimpleTokenFactory;
import com.jetbrains.index.token.factory.TokenFactory;
import com.jetbrains.index.token.tokenizer.Tokenizer;
import com.jetbrains.index.watcher.FSListener;
import com.jetbrains.index.watcher.FileChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Encapsulates the full logic of "realtime" updating of the index.
 * Acts as an orchestrator between the {@link ConcurrentIndex}, {@link com.jetbrains.index.watcher.FileSystemWatcher}
 * Also is responsible for acquiring and releasing resources.
 */
public class IndexSearchService implements StringSearch, FSListener, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(IndexSearchService.class);

    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private TokenFactory tokenFactory = new SimpleTokenFactory();

    private final Tokenizer tokenizer;
    private final ConcurrentIndex<Token, String> index;


    public IndexSearchService(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.index = new ConcurrentIndex<>();
    }


    @Override
    public Collection<String> findWord(String word) {
        var searchToken = tokenFactory.getToken(word);
        return index.search(searchToken);
    }


    @Override
    public void onFileChanged(FileChangeEvent fileChangeEvent) {
        switch (fileChangeEvent.change()) {
            case CREATE -> CompletableFuture.supplyAsync(() -> {
                addFileToIndex(fileChangeEvent.filePath());
                return null;
            }, executor);
            case DELETE -> CompletableFuture.supplyAsync(() -> {
                deleteFileFromIndex(fileChangeEvent.filePath());
                return null;
            }, executor);
            case UPDATE -> CompletableFuture.supplyAsync(() -> {
                updateFileInIndex(fileChangeEvent.filePath());
                return null;
            }, executor);
            case null, default -> throw new IllegalStateException("Unexpected value: " + fileChangeEvent.change());
        }
    }

    private void addFileToIndex(String path) {
        log.trace("Inserting into index {}", path);
        var tokens = extractTokens(path);
        index.ingestTokens(tokens,path);
    }

    private void deleteFileFromIndex(String filePath) {
        log.trace("Deleting from index {}", filePath);
        index.remove(filePath);
    }

    private void updateFileInIndex(String filePath) {
        log.trace("Updating index {}", filePath);
        var updatedTokens = extractTokens(filePath);
        index.update(updatedTokens,filePath);
    }

    /**
     * Method extracts tokens from the given path
     * @param path to a file
     * @return
     */
    private Iterable<Token> extractTokens(String path){
        try {
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException(path);
            }
            try (var reader = new FileReader(path, StandardCharsets.UTF_8)) {
                return tokenizer.tokenize(reader);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
