package com.jetbrains.index.token;

import com.jetbrains.index.token.factory.SimpleTokenFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class WhiteSpaceTokenizerTest {

    @Test
    void stringContainingSpace(){
        String input = "This is a sentence";
        var result = tokenize(input).stream().map(Token::value).toList();
        Assertions.assertEquals(4,result.size());
        Assertions.assertTrue(result.contains("This"));
        Assertions.assertTrue(result.contains("is"));
        Assertions.assertTrue(result.contains("a"));
        Assertions.assertTrue(result.contains("sentence"));
    }

    @Test
    void stringContainingSpaceAndPunctuation(){
        String input = "This is a more complicated example: because it has more delimiters?As well as more edge cases";
        var res = tokenize(input).stream().map(Token::value).toList();
        Assertions.assertEquals(17,res.size());
        Assertions.assertTrue(res.contains("This"));
        Assertions.assertTrue(res.contains("is"));
        Assertions.assertTrue(res.contains("a"));
        Assertions.assertTrue(res.contains("more"));
        Assertions.assertTrue(res.contains("complicated"));
        Assertions.assertTrue(res.contains("example"));
        Assertions.assertTrue(res.contains("because"));
        Assertions.assertTrue(res.contains("it"));
        Assertions.assertTrue(res.contains("has"));
        Assertions.assertTrue(res.contains("more"));
        Assertions.assertTrue(res.contains("delimiters"));
        Assertions.assertTrue(res.contains("As"));
        Assertions.assertTrue(res.contains("well"));
        Assertions.assertTrue(res.contains("as"));
        Assertions.assertTrue(res.contains("more"));
        Assertions.assertTrue(res.contains("edge"));
        Assertions.assertTrue(res.contains("cases"));
    }

    @Test
    void stringContainsLineAndTabulationCharacters(){
        String input = "First sentence!\nSecond sentence;\rThird sentence?";
        var res = tokenize(input).stream().map(Token::value).toList();
        Assertions.assertEquals(6,res.size());
        Assertions.assertTrue(res.contains("First"));
        Assertions.assertTrue(res.contains("Second"));
        Assertions.assertTrue(res.contains("Third"));
        var sCount = res.stream().filter((e)->e.equals("sentence")).count();
        Assertions.assertEquals(3L,sCount);

    }

    private static List<Token> tokenize(String input) {
        WhiteSpaceTokenizer whiteSpaceTokenizer = new WhiteSpaceTokenizer(new SimpleTokenFactory());
        return whiteSpaceTokenizer.tokenize(input);
    }

}
