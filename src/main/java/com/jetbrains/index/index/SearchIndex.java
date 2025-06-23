package com.jetbrains.index.index;

import java.io.File;
import java.util.List;

public interface SearchIndex {
    List<String> findWord(String word);
    List<File> findWordFile(String word);
}
