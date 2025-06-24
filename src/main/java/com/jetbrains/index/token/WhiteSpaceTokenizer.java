package com.jetbrains.index.token;

import com.jetbrains.index.token.factory.CachingTokenFactory;
import com.jetbrains.index.token.factory.TokenFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Simple tokenizer which tries to extract words from the content by
 * Delimiting them with common characters associated with ENGLISH language
 */
public class WhiteSpaceTokenizer implements Tokenizer {

    private TokenFactory tokenFactory;

    public WhiteSpaceTokenizer(TokenFactory tokenFactory) {
        this.tokenFactory = tokenFactory;
    }

    @Override
    public List<Token> tokenize(String content) {
        List<Token> tokens = new LinkedList<>();

        //Tokenizer with punctuation,line and space characters
        StringTokenizer tokenizer = new StringTokenizer(content, "\r\n\t,.?!\"':; ");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            tokens.add(tokenFactory.getToken(token));
        }
        return tokens;
    }
}
