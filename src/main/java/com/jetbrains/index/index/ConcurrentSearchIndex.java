package com.jetbrains.index.index;

import com.jetbrains.index.token.FileTokenizer;
import com.jetbrains.index.watcher.FSListener;
import com.jetbrains.index.watcher.FileChangeEvent;

import java.io.File;
import java.util.List;

public class ConcurrentSearchIndex implements SearchIndex, FSListener {

    private FileTokenizer tokenizer;



    @Override
    public List<String> findWord(String word) {
        return List.of();
    }

    @Override
    public List<File> findWordFile(String word) {
        return List.of();
    }

    @Override
    public void onFileChanged(FileChangeEvent fileChangeEvent) {

    }
}
