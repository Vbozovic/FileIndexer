package com.jetbrains.index.watcher;

import com.jetbrains.index.watcher.task.WatcherTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The File system watcher is a container of resources required for monitoring the given
 * paths that are found on the file system. It starts and  manages the {@link WatcherTask}
 * and forwards the file system events to registered listeners.
 */
public class FileSystemWatcher implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(FileSystemWatcher.class);

    private AtomicBoolean started = new AtomicBoolean(false);
    private final Collection<String> paths;
    private Thread watcherThread;
    private final List<FSListener> listeners = new ArrayList<>();

    public FileSystemWatcher(Collection<String> paths) {
        this.paths = new ArrayList<>(paths);
    }

    public void start() {
        if (this.started.compareAndSet(false, true)) {
            log.info("Starting watcher");
            startWatcher();
        }
    }

    @Override
    public void close() {
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

    /**
     * Forward the event to all listeners. The listeners themselves are responsible
     * for handling concurrency
     */
    private void invokeListeners(FileChangeEvent event) {
        try {
            this.listeners.forEach((l) -> l.onFileChanged(event));
        }catch (Throwable e){
            log.error("Error invoking listeners", e);
        }
    }

    private void startGuard(String message) {
        if (started.get()) {
            throw new IllegalStateException(message);
        }
    }

}
