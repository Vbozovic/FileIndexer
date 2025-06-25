package com.jetbrains.index.index;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic index responsible for providing:
 * 1) searching via token {@code T}
 * 2) ingesting new searchable tokens with their container {@code C}
 * 3) removal of all tokens {@code T} associated with a container {@code C}
 * <p>
 * The index maintains two relationships between {@code T} and {@code C}
 * 1) {@code T} 1---->N {@code C} under the {@code reverseIndex}
 * 2) {@code C} 1---->N {@code T} under the {@code index}
 * <p>
 * The former relation is used for efficiently searching occurrences
 * of a particular Token, while the latter is used for removing Tokens
 * associated with a container
 *
 * @param <T> Tokens found in a particular identifiable container
 * @param <C> Container which has a {@link Collection} of tokens
 */
public class ConcurrentIndex<T, C> {

    private final ConcurrentHashMap<T, Collection<C>> reverseIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<C, Collection<T>> index = new ConcurrentHashMap<>();

    /**
     * Insert tokens for the given container into the index
     *
     * @param tokens    tokens
     * @param container owning container
     */
    public void ingestTokens(Iterable<T> tokens, C container) {
        HashSet<T> tokenSet = new HashSet<>();
        tokens.forEach(token -> {
            tokenSet.add(token);
            ingestSingleToken(container, token);
        });
        index.put(container, tokenSet);
    }

    private void ingestSingleToken(C container, T token) {
        reverseIndex.compute(token, (k, v) -> {
            var present = v;
            if (present == null) {
                present = reverseMapping(container);
                return present;
            }
            present.add(container);
            return present;
        });
    }

    /**
     * Return all containers associated with the given token
     *
     * @param token for which the search done
     * @return {@link Collection} containing all occurrences of the Token
     */
    public Collection<C> search(T token) {
        var result = reverseIndex.get(token);
        if (result == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(result);
    }


    /**
     * Delete all tokens for a particular container,
     * since the deletion of tokens is a compound operation
     * a exclusive write lock is needed
     *
     * @param container container
     */
    public Collection<T> remove(C container) {
        var tokens = index.remove(container);
        if (tokens == null) {
            return Collections.emptyList();
        }
        tokens.forEach(token -> {
            reverseIndex.computeIfPresent(token, (_, v) -> {
                v.remove(container);
                return v;
            });
        });
        return tokens;
    }

    /**
     * Update method which updates the index based on the difference
     * of existing tokens and newly supplied tokens
     * {@param tokens} new tokens which may overlap with existing ones
     * {@param container} container containing new tokens
     */
    public void update(Iterable<T> tokens, C container) {
        index.computeIfPresent(container,(_,indexTokens)->{
            var newTokens = new HashSet<T>();
            for (T newToken : tokens) {
                newTokens.add(newToken);
                ingestSingleToken(container, newToken);
                //indexTokens will now contain the difference
                //between new updated tokens and existing tokens
                indexTokens.remove(newToken);
            }
            for (T toRemove: indexTokens) {
                reverseIndex.computeIfPresent(toRemove,(_,associatedFiles)->{
                    associatedFiles.remove(container);
                    //If no files are associated with the token remove the mapping from the reverse index
                    if(associatedFiles.isEmpty()) {
                        return null;
                    }
                    return associatedFiles;
                });

            }
            return newTokens;
        });
    }



    /**
     * Helper method for creating a {@link Collection} used
     * in the {@code reverseIndex} to organize the former part of
     * the relationship.
     *
     * @param container to be inserted
     * @return expandable collection of {@code C}
     */
    private Collection<C> reverseMapping(C container) {
        var set = new HashSet<C>();
        set.add(container);
        return set;
    }

}
