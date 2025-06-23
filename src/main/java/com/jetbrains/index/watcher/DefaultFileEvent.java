package com.jetbrains.index.watcher;

import java.util.Objects;

/**
 * Standard implementation of a {@link FileChangeEvent}
 */
public class DefaultFileEvent implements FileChangeEvent{

    private final String filePath;
    private final ChangeType changeType;


    public DefaultFileEvent(String filePath, ChangeType changeType) {
        this.filePath = filePath;
        this.changeType = changeType;
    }

    @Override
    public String filePath() {
        return filePath;
    }

    @Override
    public ChangeType change() {
        return changeType;
    }

    @Override
    public String toString() {
        return "DefaultFileEvent{" +
               "filePath='" + filePath + '\'' +
               ", changeType=" + changeType +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultFileEvent that)) return false;
        return Objects.equals(filePath, that.filePath) && changeType == that.changeType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, changeType);
    }
}
