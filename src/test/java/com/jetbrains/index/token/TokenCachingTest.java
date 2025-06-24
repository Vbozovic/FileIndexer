package com.jetbrains.index.token;

import com.jetbrains.index.token.factory.CachingTokenFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

public class TokenCachingTest {

    /**
     * Verifies that same values produce the same object
     * from the heap
     */
    @Test
    void sameTokenObjectForSameTokenValue(){
        var token1 = CachingTokenFactory.getToken("Value1");
        var token2 = CachingTokenFactory.getToken("Value1");
        Assertions.assertNotNull(token1);
        Assertions.assertNotNull(token2);
        Assertions.assertEquals(token1, token2);
        Assertions.assertSame(token1, token2);
    }

    /**
     * Verifies that two different values do not
     * get cached to the same {@link SimpleToken}
     */
    @Test
    void differentObjectForDifferentTokenValue(){
        var token1 = CachingTokenFactory.getToken("Value1");
        var token2 = CachingTokenFactory.getToken("Value2");
        Assertions.assertNotNull(token1);
        Assertions.assertNotNull(token2);
        Assertions.assertNotEquals(token1, token2);
        Assertions.assertNotSame(token1, token2);
    }


    /**
     * Test if cache is working correctly even if references are lost on Tokens.
     * {@link System#identityHashCode(Object)} does not return an address
     * but it serves the purpose
     */
    @Test
    void removingTokenAndCausingGCCycleGivesDifferentAddress() throws InterruptedException {
        var token = CachingTokenFactory.getToken("Value");
        Assertions.assertNotNull(token);
        int tokenAddress = System.identityHashCode(token);
        //remove reference
        token = null;

        var start = Instant.now();
        Thread.sleep(1000);
        //Beg the Jvm to consider maybe cleaning up the heap multiple times
        while(Duration.between(start, Instant.now()).toSeconds() < 5) {
            System.gc();
            Thread.sleep(100);
        }
        token = CachingTokenFactory.getToken("Value");
        int newAddress = System.identityHashCode(token);
        Assertions.assertNotEquals(tokenAddress, newAddress);
        Assertions.assertNotNull(token);
    }

}
