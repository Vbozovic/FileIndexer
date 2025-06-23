package com.jetbrains.index.watcher.task;


/**
 * Interface indicating that a message can be concurrently received
 * while providing detection for thread termination events such as {@link InterruptedException}
 * @param <T> type of Message that is received
 */
@FunctionalInterface
public interface MessageReceiver<T> {
    T receive() throws InterruptedException;
}

