package com.jetbrains.index.watcher;

/**
 * An event describing which kind of {@link ChangeType} happened
 * to a given file.
 */
public interface FileChangeEvent {
    String filePath();
    ChangeType change();
}
