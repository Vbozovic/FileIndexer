package com.jetbrains.index.watcher;

import com.jetbrains.index.watcher.task.EventPublisherTask;
import com.jetbrains.index.watcher.task.WatcherTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class FileSystemWatcher implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(FileSystemWatcher.class);
    private static final Long PUBLISHER_TIMEOUT = 0L;

    private boolean started = false;
    private final List<String> paths;
    private Thread watcherThread;
    private Thread eventPublisher;
    private final List<FSListener> listeners = new ArrayList<>();

    public FileSystemWatcher(List<String> paths) {
        this.paths = paths;
    }

    public void start() {
        startGuard("Already started");
        log.info("Starting watcher");
        var messageBus = startEventPublisher();
        startWatcher(messageBus);
        this.started = true;
    }

    @Override
    public void close() throws Exception {
        log.info("Closing watcher");
        watcherThread.interrupt();
        eventPublisher.interrupt();
    }

    public synchronized void registerListener(FSListener listener) {
        startGuard("Listeners must be added before starting the watcher");
        listeners.add(listener);
    }

    private BlockingQueue<FileChangeEvent> startEventPublisher() {
        BlockingQueue<FileChangeEvent> eventQueue = new LinkedBlockingQueue<>();
        eventPublisher = new Thread(new EventPublisherTask(eventQueue::take,listeners));
        return eventQueue;
    }

    private void startWatcher(BlockingQueue<FileChangeEvent> eventQueue) {
        watcherThread = new Thread(new WatcherTask(paths, eventQueue::put));
        watcherThread.start();
    }

    private void startGuard(String message){
        if (started){
            throw new IllegalStateException(message);
        }
    }

}
