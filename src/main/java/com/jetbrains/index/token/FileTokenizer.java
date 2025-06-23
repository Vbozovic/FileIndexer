package com.jetbrains.index.token;

import java.io.File;
import java.util.List;

public interface FileTokenizer {
    List<Token> tokenize(File f);
}
