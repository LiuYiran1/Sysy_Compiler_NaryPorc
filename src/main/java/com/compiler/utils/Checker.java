package com.compiler.utils;
import com.compiler.listeners.LexerListener;
import com.compiler.frontend.SysYLexer;
import com.compiler.frontend.SysYParser;
import com.compiler.listeners.ParserListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;

import java.util.List;

public class Checker {
    static boolean LexerChecker = false;
    static boolean ParserChecker = false;

    public static void checkLexer(SysYLexer Lexer, LexerListener lexerListener, String fileName) {
        if (!LexerChecker) { return; }

        List<? extends Token> tokens = Lexer.getAllTokens();
        for (Token token : tokens) {
            String text = token.getText();
            System.out.print(text + " ");
        }
        System.out.println();
        if (lexerListener.hasError) {
            System.err.println("Lexer Error in file: " + fileName);
        } else {
            System.out.println("Lexer OK");
        }
    }

    public static void checkParser(SysYParser parser, ParserListener parserListener, String fileName) {
        if (!ParserChecker) { return; }

        if (parserListener.hasError) {
            System.err.println("Parser Error in file: " + fileName);
        } else {
            System.out.println("Parser OK");
        }
    }

}
