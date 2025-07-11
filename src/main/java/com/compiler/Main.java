package com.compiler;

import com.compiler.frontend.SysYLexer;
import com.compiler.frontend.SysYParser;
import com.compiler.listeners.LexerListener;
import com.compiler.listeners.ParserListener;
import com.compiler.utils.Checker;
import com.compiler.ir.LLVisitor;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.llvm4j.optional.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    static String inputFile;

    static SysYLexer lexer;
    static LexerListener lexerListener;

    static SysYParser parser;
    static ParserListener parserListener;

    static ParseTree tree;

    public static void main(String[] args) throws IOException {
        Path inputDir = Paths.get("src/test/java/h_functional");

        // 找出所有 .sy 文件
        List<Path> syFiles = Files.walk(inputDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".sy"))
                .toList();

        // 构造对应的 .ll 输出路径
        List<Path> llFiles = syFiles.stream()
                .map(path -> {
                    String fileName = path.getFileName().toString().replaceAll("\\.sy$", ".ll");
                    return path.getParent().resolve(fileName);
                })
                .toList();

        for (int i = 0; i < syFiles.size(); i++) {
            Path inputPath = syFiles.get(i);
            inputFile = inputPath.toString();
            Path outputPath = llFiles.get(i);

            processLexer(inputPath.toString());
            processParser(lexer, inputPath.toString());
            irGen(tree, outputPath.toString());
            clear();
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

    private static void irGen(ParseTree tree, String outputPath) {
        try{
            LLVisitor llVisitor = new LLVisitor();
            llVisitor.visit(tree);
            llVisitor.dump(Option.of(new File(outputPath)));
        }catch (Exception e){
            System.err.println("exception in " + inputFile);
        }catch (Error e){
            System.err.println("error in " + inputFile);
        }
    }

    private static void clear(){

    }
}
