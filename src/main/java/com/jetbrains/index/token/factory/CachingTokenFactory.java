package com.jetbrains.index.token.factory;

import com.jetbrains.index.token.SimpleToken;
import com.jetbrains.index.token.Token;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory which caches non-mutable {@link SimpleToken} instances
 * Rationale behind this is that in the process of parsing documents there will inevitability
 * be tokens of the same value. Once the Tokens are removed from the index the weak reference
 * will not block cleanup of the associated memory.
 */
public class CachingTokenFactory implements TokenFactory {
    private static final ConcurrentHashMap<String, WeakReference<Token>> tokenMap = new ConcurrentHashMap<>();
    private static final CachingTokenFactory instance = new CachingTokenFactory();

    private CachingTokenFactory() {
    }

    public static CachingTokenFactory getInstance() {
        return instance;
    }

    public static Token getToken(String tokenValue) {
        return tokenMap.compute(tokenValue, (k, v) -> {
            //in the case a token was removed from the index and then later re added
            if (v == null || v.get() == null) {
                return new WeakReference<>(new SimpleToken(k));
            }
            return v;
        }).get();
    }

    @Override
    public Token getToken(Object param) {
        if (!(param instanceof String)) {
            throw new IllegalArgumentException("Parameter must be of type String");
        }
        return getToken((String) param);
    }
}
