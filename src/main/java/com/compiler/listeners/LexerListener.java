package com.compiler.listeners;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class LexerListener extends BaseErrorListener {
    public boolean hasError = false;

    static String dealMsg(String msg){
        String[] temList = msg.split(" ");
        String newVar5 = temList[temList.length - 1].substring(1, temList[temList.length - 1].length() - 1);
        return newVar5;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        this.hasError = true;
        System.err.println("Error type A at Line " + line + ": Mysterious character \"" + dealMsg(msg) + "\"" + '.');
    }

}