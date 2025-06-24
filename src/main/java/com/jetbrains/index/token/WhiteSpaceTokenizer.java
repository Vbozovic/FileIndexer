package com.jetbrains.index.token;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Simple tokenizer which tries to extract words from the content by
 * Delimiting them with common characters associated with ENGLISH language
 */
public class WhiteSpaceTokenizer implements Tokenizer {
    @Override
    public List<Token> tokenize(String content) {
        List<Token> tokens = new LinkedList<>();

        //Tokenizer with punctuation,line and space characters
        StringTokenizer tokenizer = new StringTokenizer(content, "\r\n\t,.?!\"':; ");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            tokens.add(new SimpleToken(token));
        }
        return tokens;
    }
}
