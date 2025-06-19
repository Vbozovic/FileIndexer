package com.jetbrains.index;

import com.jetbrains.index.watcher.FileSystemWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        FileSystemWatcher watcher = new FileSystemWatcher(List.of("/Users/vbozovic/usavrsavanje/FileIndexer/src/main/resources"));
        watcher.start();
        while (true) {
            Thread.sleep(0);
        }
    }

}
