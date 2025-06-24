package com.jetbrains.index.index;

import java.util.List;

public interface SearchIndex {
    List<String> findWord(String word);
}
