package com.jetbrains.index.index.functional;

import com.jetbrains.index.token.factory.CachingTokenFactory;
import com.jetbrains.index.token.factory.TokenFactory;

/**
 * Runs tests with the memory optimized {@link CachingTokenFactory}
 */
public class ConcurrentIndexCachedFactoryTest extends ConcurrentIndexFunctionalTest {
    @Override
    protected TokenFactory getTokenFactory() {
        return CachingTokenFactory.getInstance();
    }
}
