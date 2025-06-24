package com.jetbrains.index.token;

import java.util.List;

public interface Tokenizer {
    List<Token> tokenize(String content);
}
