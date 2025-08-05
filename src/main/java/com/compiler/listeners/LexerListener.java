package com.compiler.listeners;

import com.compiler.frontend.SysYLexer;
import org.antlr.v4.runtime.*;

import java.util.*;

public class LexerListener extends BaseErrorListener {
    public boolean hasError = false;

    private final Map<String, String> renamedIdentifiers = new HashMap<>();
    private int renameCounter = 0;

    // 对外暴露的重命名映射
    public Map<String, String> getRenamedIdentifiers() {
        return renamedIdentifiers;
    }

    static String dealMsg(String msg){
        String[] temList = msg.split(" ");
        String newVar5 = temList[temList.length - 1].substring(1, temList[temList.length - 1].length() - 1);
        return newVar5;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine,
                            String msg, RecognitionException e) {
        this.hasError = true;
        System.err.println("Error type A at Line " + line + ": Mysterious character \"" + dealMsg(msg) + "\"" + '.');
    }

}
