package com.jetbrains.index.token.factory;

import com.jetbrains.index.token.Token;

/**
 * A singular place where the creation of {@link Token} can be controlled
 * Two implementations exist {@link SimpleTokenFactory} and {@link CachingTokenFactory}
 */
public interface TokenFactory {
    Token getToken(Object param);
}
