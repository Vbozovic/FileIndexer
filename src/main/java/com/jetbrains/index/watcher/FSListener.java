package com.jetbrains.index.watcher;

/**
 * A listener interface which is called whenever a {@link FileChangeEvent}
 * is detected (emitted) by the underlying {@link FileSystemWatcher}
 */
public interface FSListener {
    /**
     * Implementations of this method must not do as little work as possible
     * ideally just store the event for further processing as this might slow down
     * the speed of emitting events.
     */
    void onFileChanged(FileChangeEvent fileChangeEvent);
}
