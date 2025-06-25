package com.jetbrains.index.index.functional;

import com.jetbrains.index.index.ConcurrentIndex;
import com.jetbrains.index.token.factory.SimpleTokenFactory;
import com.jetbrains.index.token.factory.TokenFactory;

/**
 * Tests the {@link ConcurrentIndex} implementation backed by {@link SimpleTokenFactory}
 */
public class ConcurrentIndexSimpleFactoryTest extends ConcurrentIndexFunctionalTest {
    @Override
    protected TokenFactory getTokenFactory() {
        return new SimpleTokenFactory();
    }
}
