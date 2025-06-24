package com.jetbrains.index.token.tokenizer;

import com.jetbrains.index.token.Token;

import java.io.InputStreamReader;
import java.util.Collection;

/**
 * Interface describing the API a single tokenizer implements
 * The return type is as abstract as possible for flexibility purposes.
 * Tokenizer is responsible for handling reading of the source.
 */
public interface Tokenizer {
    /**
     * Takes the content (from a file) and produces a {@link Collection} of tokens
     *
     * @param content logical unit ready to be tokenized
     * @return something that can be iterated over in order to collect all the tokens
     */
    Iterable<Token> tokenize(InputStreamReader content);

}
