package com.jetbrains.index.watcher;

import java.io.FileInputStream;

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
}
