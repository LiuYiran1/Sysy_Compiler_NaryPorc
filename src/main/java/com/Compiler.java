package com;

import com.compiler.listeners.LexerListener;
import com.compiler.frontend.SysYLexer;
import com.compiler.frontend.SysYParser;
import com.compiler.listeners.ParserListener;
import com.compiler.utils.Checker;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Compiler {
    static SysYLexer lexer;
    static LexerListener lexerListener;

    static SysYParser parser;
    static ParserListener parserListener;

    public static void main(String[] args) throws IOException {
        Path inputDir = Paths.get("src/test/java/RISCV_performance");

        // 找出所有.sy文件
        List<Path> syFiles = Files.walk(inputDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".sy"))
                .toList();

//        for (Path inputFile : syFiles) {
//            String fileName = "src/test/java/RISCV_performance/" + inputFile.getFileName().toString();
//            //System.out.println("Input file: " + fileName);
//
//            processLexer(inputFile.toString());
//            Checker.checkLexer(lexer, lexerListener,fileName);
//        }

        for (Path inputFile : syFiles) {
            processLexer(inputFile.toString());
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            parser = new SysYParser(tokens);
            parser.removeErrorListeners();
            parserListener = new ParserListener();
            parser.addErrorListener(parserListener);

            ParseTree tree = parser.program();

            if (parserListener.hasError){
                System.out.println("Parse error");
            } else{
                System.out.println("Parse success");
            }
        }



    }

    private static void processLexer(String inputPath) {
        CharStream input = null;
        try{
            input = CharStreams.fromFileName(inputPath);
        }catch (IOException e){
            System.err.println("can not get input");
        }
        lexer = new SysYLexer(input);
        lexerListener = new LexerListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(lexerListener);
    }
}
