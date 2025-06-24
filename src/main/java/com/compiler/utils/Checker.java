package com.compiler.utils;
import com.compiler.listeners.LexerListener;
import com.compiler.frontend.SysYLexer;
import org.antlr.v4.runtime.Token;

import java.util.List;

public class Checker {
    static boolean LexerChecker = false;

    public static void checkLexer(SysYLexer Lexer, LexerListener lexerListener, String fileName) {
        if (!LexerChecker) { return; }

        List<? extends Token> tokens = Lexer.getAllTokens();
        if (lexerListener.hasError) {
            System.err.println("Lexer Error in file: " + fileName);
        } else {
            System.out.println("Lexer OK");
        }
    }

}
