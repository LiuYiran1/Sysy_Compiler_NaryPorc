package com.compiler.frontend;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;

import java.util.*;

public class RenamingSysYLexer extends SysYLexer {
    private final Map<String, String> renamedMap = new HashMap<>();
    private int counter = 0;

    public RenamingSysYLexer(CharStream input) {
        super(input);
    }

    public Map<String, String> getRenamedMap() {
        return renamedMap;
    }

    @Override
    public Token emit() {
        if (_type == SysYLexer.IDENT) {
            String text = _input.getText(Interval.of(_tokenStartCharIndex, getCharIndex() - 1));
            if (text.length() > 32) {
                String newName = renamedMap.computeIfAbsent(text, k -> "__longvar_" + counter++);
                setText(newName);
            }
        }
        return super.emit();
    }

}
