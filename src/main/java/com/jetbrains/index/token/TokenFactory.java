package com.jetbrains.index.token;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory which caches non-mutable {@link SimpleToken} instances
 * Rationale behind this is that in the process of parsing documents there will inevitability
 * be tokens of the same value. Once the Tokens are removed from the index the weak reference
 * will not block cleanup of the associated memory.
 */
public class TokenFactory {
    private static final ConcurrentHashMap<String, WeakReference<Token>> tokenMap = new ConcurrentHashMap<>();

    private TokenFactory() {}


    public static Token getToken(String tokenValue) {

        return tokenMap.compute(tokenValue,(k,v)->{
            //in the case a token was removed from the index and then later re added
            if(v == null || v.get() == null) {
                return new WeakReference<>(new SimpleToken(k));
            }
            return v;
        }).get();
    }

}
