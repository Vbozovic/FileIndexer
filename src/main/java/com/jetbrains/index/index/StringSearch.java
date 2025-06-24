package com.jetbrains.index.index;

import java.util.Collection;

/**
 * Base API for querying the index which is being
 * updated in "realtime"
 */
public interface StringSearch {
    /**
     * Returns the list with the paths to files
     * which contain the given word
     * @param word to be searched for in the index
     * @return {@link Collection} of files which contain the given word
     */
    Collection<String> findWord(String word);
}
