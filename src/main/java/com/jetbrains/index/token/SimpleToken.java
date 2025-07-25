package com.jetbrains.index.token;

import java.util.Objects;

/**
 * Basic implementation of a {@link Token} which
 * delegates everything to the {@link String} class
 * And serves as a wrapper.
 */
public final class SimpleToken implements Token {

    private String value;

    public SimpleToken(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleToken that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
