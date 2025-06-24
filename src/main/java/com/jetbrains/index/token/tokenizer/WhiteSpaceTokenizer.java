package com.jetbrains.index.token.tokenizer;

import com.jetbrains.index.token.Token;
import com.jetbrains.index.token.factory.TokenFactory;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

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
    public Iterable<Token> tokenize(InputStreamReader fileContent) {
        try {
            String content = IOUtils.toString(fileContent);

            List<Token> tokens = new LinkedList<>();
            //Tokenizer with punctuation,line and space characters
            StringTokenizer tokenizer = new StringTokenizer(content, "\r\n\t,.?!\":; ");
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                tokens.add(tokenFactory.getToken(token));
            }
            return tokens;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
