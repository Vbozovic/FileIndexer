package com.jetbrains.index.token.factory;

import com.jetbrains.index.token.SimpleToken;
import com.jetbrains.index.token.Token;

/**
 * Simple factory which creates a new {@link SimpleToken}
 * object on every invocation of {@link TokenFactory#getToken(Object)}
 */
public class SimpleTokenFactory implements TokenFactory {

    public SimpleTokenFactory() {
    }

    @Override
    public Token getToken(Object param) {
        if (!(param instanceof String)) {
            throw new IllegalArgumentException("SimpleTokenFactory requires a string");
        }
        return new SimpleToken((String) param);
    }
}
