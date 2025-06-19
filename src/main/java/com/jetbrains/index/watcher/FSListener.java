package com.jetbrains.index.watcher;

/**
 *
 */
public interface FSListener {
    void onFileChanged(FileChangeEvent fileChangeEvent);
}
