package com.jetbrains.index.watcher;

import java.io.FileInputStream;

public interface FileChangeEvent {
    String filePath();
    ChangeType change();
}
