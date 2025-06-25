package com.jetbrains.index.index.functional;

import com.jetbrains.index.index.ConcurrentIndex;
import com.jetbrains.index.token.Token;
import com.jetbrains.index.token.factory.TokenFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/**
 * Abstract class containing test logic irrelevant of the TokenFactory used
 * Concrete implementation classes supply the {@link TokenFactory} which is applied in tests
 *
 */
public abstract class ConcurrentIndexFunctionalTest {

    /**
     * Empty index must not return anything
     */
    @Test
    void emptyIndex() {
        var index = new ConcurrentIndex<Token, String>();
        var result = index.search(token(1));
        Assertions.assertTrue(result.isEmpty());
    }

    /**
     * Verifies that for a given list of tokens and a single container
     * when searched for every element of original collection of tokens
     * the index returns correct result
     */
    @Test
    void ingestedTokensAreSearchable() {
        ConcurrentIndex<Token, String> index = new ConcurrentIndex<>();
        var tokens = List.of(token(1), token(2), token(3));
        var container = "/test/container1";

        //insert into index
        index.ingestTokens(tokens, container);

        //verify that index returns correct container for tokens
        indexHas(index, "/test/container1", token(1));
        indexHas(index, "/test/container1", token(2));
        indexHas(index, "/test/container1", token(3));
    }

    /**
     * Verifies that two files with common tokens are both returned
     * when being queried for the common token
     */
    @Test
    void ingestedTokensHaveCommonContainer() {
        var tokens1 = List.of(token(1), token(2), token(3));
        var tokens2 = List.of(token(3), token(4), token(5));
        var container1 = "/test/container1";
        var container2 = "/test/container2";

        //insert into index
        var index = new ConcurrentIndex<Token, String>();
        index.ingestTokens(tokens1, container1);
        index.ingestTokens(tokens2, container2);

        //verify 1st and 2nd container returned when index queried for common token
        var result = index.search(token(3));
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.contains("/test/container1"));
        Assertions.assertTrue(result.contains("/test/container2"));
    }

    /**
     * Verifies that when there is no intersection between tokens
     * searching for one set of tokens will not return the container of other
     * set of tokens
     */
    @Test
    void ingestedTokensHaveNoCommonContainer() {
        var tokens1 = List.of(token(1), token(2));
        var tokens2 = List.of(token(4), token(5));
        var container1 = "/test/container1";
        var container2 = "/test/container2";

        //Insert tokens
        var index = new ConcurrentIndex<Token, String>();
        index.ingestTokens(tokens1, container1);
        index.ingestTokens(tokens2, container2);

        //verify 2nd container does not contain params of 1st
        var result = index.search(token(1));
        Assertions.assertFalse(result.contains("/test/container2"));
        result = index.search(token(2));
        Assertions.assertFalse(result.contains("/test/container2"));

        //verify 1st container does not contain params of 2nd
        result = index.search(token(4));
        Assertions.assertFalse(result.contains("/test/container1"));
        result = index.search(token(5));
        Assertions.assertFalse(result.contains("/test/container1"));
    }

    /**
     * Verifies that removing a container removes all tokens associated with that container
     */
    @Test
    void removingContainer(){
        var tokens = List.of(token(1), token(2));
        var container = "/test/container1";
        var index = new ConcurrentIndex<Token, String>();
        //insert into index
        index.ingestTokens(tokens, container);
        //verify that tokens are inserted
        var result = index.search(token(1));
        Assertions.assertFalse(result.isEmpty());
        result = index.search(token(2));
        Assertions.assertFalse(result.isEmpty());

        //Remove from index
        var tokenResult = index.remove("/test/container1");
        Assertions.assertEquals(2,tokenResult.size());
        Assertions.assertTrue(tokenResult.contains(token(1)));
        Assertions.assertTrue(tokenResult.contains(token(2)));

        //verify index does not contain removed elements
        result = index.search(token(1));
        Assertions.assertTrue(result.isEmpty());
        result = index.search(token(2));
        Assertions.assertTrue(result.isEmpty());
    }

    /**
     * Verifies that given two containers with some common tokens
     * removing one container does not remove common tokens from the other
     */
    @Test
    void removingContainerWithCommonTokens(){
        var tokens1 = List.of(token(1), token(2),token(3));
        var tokens2 = List.of(token(3),token(4), token(5));
        var container1 = "/test/container1";
        var container2 = "/test/container2";

        //insert into index
        var index = new ConcurrentIndex<Token, String>();
        index.ingestTokens(tokens1, container1);
        index.ingestTokens(tokens2, container2);

        //Verify all tokens return correct containers
        indexHas(index, "/test/container1", token(1));
        indexHas(index, "/test/container1", token(2));

        var commonResult = index.search(token(3));
        Assertions.assertFalse(commonResult.isEmpty());
        Assertions.assertTrue(commonResult.contains("/test/container1"));
        Assertions.assertTrue(commonResult.contains("/test/container2"));

        indexHas(index, "/test/container2", token(4));
        indexHas(index, "/test/container2", token(5));

        //remove one container
        index.remove("/test/container1");

        //Verify common is still returned for other container
        indexHas(index, "/test/container2", token(3));

    }

    /**
     * Verifies that the container only contains updated tokens
     */
    @Test
    void updateIndexWithNewTokens(){
        var tokens = List.of(token(1), token(2),token(3));
        var index = new ConcurrentIndex<Token, String>();
        index.ingestTokens(tokens,"/test/container1");

        tokens = List.of(token(4),token(5),token(6));
        index.update(tokens,"/test/container1");

        //Verify new tokens are associated with container
        Assertions.assertEquals(1,index.search(token(4)).size());
        Assertions.assertEquals(1,index.search(token(5)).size());
        Assertions.assertEquals(1,index.search(token(6)).size());

        //verify old tokens are no longer associated with container
        Assertions.assertTrue(index.search(token(1)).isEmpty());
        Assertions.assertTrue(index.search(token(2)).isEmpty());
        Assertions.assertTrue(index.search(token(3)).isEmpty());
    }

    /**
     * Verifies that the container contains updated tokens
     * which partially overlap with current tokens
     */
    @Test
    void updateIndexWithOverlappingTokens(){
        var tokens = List.of(token(1), token(2),token(3));
        var index = new ConcurrentIndex<Token, String>();
        index.ingestTokens(tokens,"/test/container1");

        //Update with one overlapping token
        tokens = List.of(token(3),token(4),token(5));
        index.update(tokens,"/test/container1");

        //Verify new tokens as well as common token is present
        Assertions.assertEquals(1,index.search(token(3)).size());
        Assertions.assertEquals(1,index.search(token(4)).size());
        Assertions.assertEquals(1,index.search(token(5)).size());
    }

    /**
     * Verifies that tokens associated with multiple containers
     * Are correctly handled when updating one of those containers
     */
    @Test
    void updateIndexOfMultipleContainersWithOverlappingTokens(){
        var tokens1 = List.of(token(1), token(2),token(3));
        var tokens2 = List.of(token(3),token(4), token(5));
        var index = new ConcurrentIndex<Token, String>();
        index.ingestTokens(tokens1,"/test/container1");
        index.ingestTokens(tokens2,"/test/container2");

        //Verify both containers have common token
        var common = index.search(token(3));
        Assertions.assertEquals(2,common.size());
        Assertions.assertTrue(common.contains("/test/container1"));
        Assertions.assertTrue(common.contains("/test/container2"));

        //Update one of the containers removing the common token
        var updatedTokens = List.of(token(1),token(2));
        index.update(updatedTokens,"/test/container1");
        //Verify updated container no longer has the formerly common token
        Assertions.assertFalse(index.search(token(3)).contains("/test/container1"));

        //Verify the other container still has the formerly common token
        Assertions.assertEquals(1,index.search(token(3)).size());
        Assertions.assertTrue(index.search(token(3)).contains("/test/container2"));
    }

    private Token token(int param) {
        return getTokenFactory().getToken("Token" + param);
    }

    private void indexHas(ConcurrentIndex<Token,String> index, String container, Token token) {
        var result = index.search(token);
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.contains(container));
    }

    protected abstract TokenFactory getTokenFactory();

}
