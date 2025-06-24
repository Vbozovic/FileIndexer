package com.jetbrains.index.token;

/**
 * Interface describing the public facing API.
 * Since we are dealing with UTF-8 text files all Tokens can
 * be compared using their original text value.
 * Implementations of this interface must implement
 * {@link Object#hashCode()} and {@link Object#equals(Object)} correctly
 */
public interface Token {
    String value();
}
