package com.jetbrains.index.watcher;

import com.jetbrains.index.watcher.task.WatcherTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FileSystemWatcher implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(FileSystemWatcher.class);

    private boolean started = false;
    private final List<String> paths;
    private Thread watcherThread;
    private final List<FSListener> listeners = new ArrayList<>();

    public FileSystemWatcher(List<String> paths) {
        this.paths = paths;
    }

    public void start() {
        startGuard("Already started");
        log.info("Starting watcher");
        startWatcher();
        this.started = true;
    }

    @Override
    public void close(){
        log.info("Closing watcher");
        watcherThread.interrupt();
    }

    public synchronized void registerListener(FSListener listener) {
        startGuard("Listeners must be added before starting the watcher");
        listeners.add(listener);
    }

    private void startWatcher() {
        watcherThread = new Thread(new WatcherTask(paths, this::invokeListeners));
        watcherThread.start();
    }

    private void invokeListeners(FileChangeEvent event){
        this.listeners.forEach((l)-> l.onFileChanged(event));
    }

    private void startGuard(String message){
        if (started){
            throw new IllegalStateException(message);
        }
    }

}
