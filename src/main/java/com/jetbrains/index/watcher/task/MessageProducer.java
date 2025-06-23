package com.jetbrains.index.watcher.task;

/**
 * A complement to {@link MessageReceiver} which denotes
 * an operation which is blocking and which can be interrupted via  {@link InterruptedException}
 */
public interface MessageProducer<T> {
    void send(T message) throws InterruptedException;
}
