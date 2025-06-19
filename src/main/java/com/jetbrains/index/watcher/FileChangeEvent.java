package com.jetbrains.index.watcher;

import java.time.LocalDateTime;

public interface FileChangeEvent {
    byte[] content();
    ChangeType change();
}
