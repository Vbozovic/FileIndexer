package com.jetbrains.index.token.tokenizer;

import com.jetbrains.index.token.Token;

import java.util.Collection;

/**
 * Interface describing the API a single tokenizer implements
 * The return type is as abstract as possible for flexibility purposes.
 */
public interface Tokenizer {
    /**
     * Takes the content (from a file) and produces a {@link Collection} of tokens
     * @param content logical unit ready to be tokenized
     * @return all the tokens parsed
     */
    Iterable<Token> tokenize(String content);
}
