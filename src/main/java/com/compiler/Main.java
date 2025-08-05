package com.compiler;

import com.compiler.frontend.RenamingSysYLexer;
import com.compiler.frontend.SysYLexer;
import com.compiler.frontend.SysYParser;
import com.compiler.ir2.LLVisitor;
import com.compiler.listeners.LexerListener;
import com.compiler.listeners.ParserListener;
import com.compiler.ll.Module;
import com.compiler.mir.MIRModule;
import com.compiler.utils.Checker;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;


public class Main {
    static String inputFile;

    static RenamingSysYLexer lexer;
    static LexerListener lexerListener;

    static SysYParser parser;
    static ParserListener parserListener;

    static ParseTree tree;

    static boolean needPass = false;

    public static void main(String[] args) throws IOException {
        String outputFile = null;
        boolean generateAssembly = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-S":
                    generateAssembly = true;
                    break;
                case "-o":
                    if (i + 1 < args.length) {
                        outputFile = args[++i];
                    } else {
                        System.err.println("Error: -o must be followed by a filename");
                        return;
                    }
                    break;
                case "-O1":
                    needPass = true;
                    break;
                default:
                    if (args[i].endsWith(".sy")) {
                        inputFile = args[i];
                    } else {
                        System.err.println("Unrecognized argument: " + args[i]);
                        return;
                    }
                    break;
            }
        }

        if (inputFile == null || outputFile == null || !generateAssembly) {
            System.err.println("Usage: compiler -S -o <output.s> <input.sy> [-O1]");
            return;
        }

        // 词法分析
        processLexer(inputFile);

        // 语法分析
        processParser(lexer, inputFile);

        // IR -> 汇编
        irGen2(tree, outputFile);
    }


    private static void processLexer(String inputPath) {
        CharStream input = null;
        try{
            input = CharStreams.fromFileName(inputPath);
        }catch (IOException e){
            System.err.println("can not get input");
        }
        lexer = new RenamingSysYLexer(input);
        lexerListener = new LexerListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(lexerListener);

        Checker.checkLexer(lexer, lexerListener,inputPath);
    }

    private static void processParser(SysYLexer lexer, String inputPath) {
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        parser = new SysYParser(tokens);
        parser.removeErrorListeners();
        parserListener = new ParserListener();
        parser.addErrorListener(parserListener);

        tree = parser.program();

        Checker.checkParser(parser, parserListener,inputPath);
    }

    private static void irGen2(ParseTree tree, String outputPath) {
        LLVisitor llVisitor2 = new LLVisitor();
        llVisitor2.visit(tree);
        // Module mod = llVisitor2.dump(needPass);
        Module mod = llVisitor2.dump(true);
        MIRModule mirModule = llVisitor2.mirGen(mod);
        llVisitor2.riscvGen(mirModule,outputPath);
    }
}