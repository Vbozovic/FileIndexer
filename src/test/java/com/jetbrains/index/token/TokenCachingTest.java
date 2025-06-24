package com.jetbrains.index.token;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokenCachingTest {

    /**
     * Verifies that same values produce the same object
     * from the heap
     */
    @Test
    void sameTokenObjectForSameTokenValue(){
        var token1 = TokenFactory.getToken("Value1");
        var token2 = TokenFactory.getToken("Value1");
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
        var token1 = TokenFactory.getToken("Value1");
        var token2 = TokenFactory.getToken("Value2");
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
        var token = TokenFactory.getToken("Value");
        Assertions.assertNotNull(token);
        int tokenAddress = System.identityHashCode(token);
        //remove reference
        token = null;
        //Beg the Jvm to consider maybe cleaning up the heap
        System.gc();
        //Just in case
        Thread.sleep(1000);
        token = TokenFactory.getToken("Value");
        int newAddress = System.identityHashCode(token);
        Assertions.assertNotEquals(tokenAddress, newAddress);
        Assertions.assertNotNull(token);
    }

}
