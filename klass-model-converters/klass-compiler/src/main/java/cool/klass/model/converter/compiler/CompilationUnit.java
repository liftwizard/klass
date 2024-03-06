package cool.klass.model.converter.compiler;

import java.io.InputStream;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Pattern;

import cool.klass.model.meta.grammar.KlassLexer;
import cool.klass.model.meta.grammar.KlassParser;
import cool.klass.model.meta.grammar.KlassParser.CompilationUnitContext;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;

public final class CompilationUnit
{
    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\r?\\n");

    private final String[]               lines;
    private final CharStream             charStream;
    private final TokenStream            tokenStream;
    private final CompilationUnitContext compilationUnitContext;

    public CompilationUnit(
            String[] lines,
            CharStream charStream,
            TokenStream tokenStream,
            CompilationUnitContext compilationUnitContext)
    {
        this.lines = Objects.requireNonNull(lines);
        this.charStream = Objects.requireNonNull(charStream);
        this.tokenStream = Objects.requireNonNull(tokenStream);
        this.compilationUnitContext = Objects.requireNonNull(compilationUnitContext);
    }

    public static CompilationUnit getCompilationUnit(String classpathLocation)
    {
        String                 sourceCodeText         = CompilationUnit.slurp(classpathLocation);
        String[]               lines                  = NEWLINE_PATTERN.split(sourceCodeText);
        ANTLRErrorListener     errorListener          = new ThrowingErrorListener(classpathLocation, lines);
        CodePointCharStream    charStream             = CharStreams.fromString(sourceCodeText, classpathLocation);
        CommonTokenStream      tokenStream            = CompilationUnit.getTokenStream(charStream, errorListener);
        KlassParser            parser                 = CompilationUnit.getParser(errorListener, tokenStream);
        CompilationUnitContext compilationUnitContext = parser.compilationUnit();
        return new CompilationUnit(
                lines,
                charStream,
                tokenStream,
                compilationUnitContext);
    }

    private static String slurp(String classpathLocation)
    {
        InputStream inputStream = KlassCompiler.class.getClassLoader().getResourceAsStream(classpathLocation);
        Objects.requireNonNull(inputStream);
        return CompilationUnit.slurp(inputStream);
    }

    private static CommonTokenStream getTokenStream(
            CodePointCharStream charStream,
            ANTLRErrorListener errorListener)
    {
        KlassLexer lexer = new KlassLexer(charStream);
        lexer.addErrorListener(errorListener);
        return new CommonTokenStream(lexer);
    }

    private static KlassParser getParser(ANTLRErrorListener errorListener, CommonTokenStream tokenStream)
    {
        KlassParser parser = new KlassParser(tokenStream);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return parser;
    }

    private static String slurp(InputStream inputStream)
    {
        try (Scanner scanner = new Scanner(inputStream).useDelimiter("\\A"))
        {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    public String[] getLines()
    {
        return this.lines;
    }

    public CharStream getCharStream()
    {
        return this.charStream;
    }

    public TokenStream getTokenStream()
    {
        return this.tokenStream;
    }

    public CompilationUnitContext getCompilationUnitContext()
    {
        return this.compilationUnitContext;
    }

    @Override
    public String toString()
    {
        return this.charStream.getSourceName();
    }
}
