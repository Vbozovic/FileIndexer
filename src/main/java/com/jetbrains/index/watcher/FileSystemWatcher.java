package com.jetbrains.index.watcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FileSystemWatcher implements AutoCloseable{
    private static final Logger log = LoggerFactory.getLogger(FileSystemWatcher.class);

    private final List<String> paths;
    private final WatcherTask watcherTask;
    private Thread watcherThread;
    private List<FSListener> listeners = new ArrayList<>();

    public FileSystemWatcher(List<String> paths) {
        this.paths = paths;
        this.watcherTask = new WatcherTask(paths);
    }


    public void start(){
        log.info("Starting watcher");
        watcherThread = new Thread(watcherTask);
        watcherThread.start();
    }

    @Override
    public void close() throws Exception {
        log.info("Closing watcher");
        watcherThread.interrupt();
    }

    public void registerListener(FSListener listener) {
        listeners.add(listener);
    }
}
