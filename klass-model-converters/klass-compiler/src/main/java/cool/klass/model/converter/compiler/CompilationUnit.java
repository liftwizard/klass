/*
 * Copyright 2025 Craig Motlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cool.klass.model.converter.compiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.google.common.base.Splitter;
import cool.klass.model.converter.compiler.parser.ThrowingErrorListener;
import cool.klass.model.converter.compiler.phase.AbstractCompilerPhase;
import cool.klass.model.converter.compiler.state.AntlrElement;
import cool.klass.model.meta.domain.AbstractElement.ElementBuilder;
import cool.klass.model.meta.domain.SourceCodeImpl.SourceCodeBuilderImpl;
import cool.klass.model.meta.grammar.KlassLexer;
import cool.klass.model.meta.grammar.KlassParser;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.block.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CompilationUnit {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompilationUnit.class);

    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\r?\\n");

    private final int ordinal;

    @Nonnull
    private final Optional<AntlrElement> macroElement;

    @Nonnull
    private final String sourceName;

    @Nonnull
    private final String sourceCodeText;

    @Nonnull
    private final BufferedTokenStream tokenStream;

    @Nonnull
    private final ParserRuleContext parserContext;

    private SourceCodeBuilderImpl sourceCodeBuilder;

    private CompilationUnit(
        int ordinal,
        @Nonnull Optional<AntlrElement> macroElement,
        @Nonnull String sourceName,
        @Nonnull String sourceCodeText,
        @Nonnull BufferedTokenStream tokenStream,
        @Nonnull ParserRuleContext parserRuleContext
    ) {
        this.ordinal = ordinal;
        this.macroElement = Objects.requireNonNull(macroElement);
        this.sourceName = Objects.requireNonNull(sourceName);
        this.sourceCodeText = Objects.requireNonNull(sourceCodeText);
        this.tokenStream = Objects.requireNonNull(tokenStream);
        this.parserContext = Objects.requireNonNull(parserRuleContext);

        if (macroElement.isPresent() && !sourceName.contains("macro")) {
            throw new AssertionError(sourceName);
        }
    }

    public int getOrdinal() {
        return this.ordinal;
    }

    @Nonnull
    public Optional<AntlrElement> getMacroElement() {
        return this.macroElement;
    }

    @Nonnull
    public ParserRuleContext getParserContext() {
        return this.parserContext;
    }

    @Nonnull
    public String getSourceName() {
        return this.sourceName;
    }

    @Nonnull
    public String getFullPathSourceName() {
        if (this.macroElement.isEmpty()) {
            List<String> split = Splitter.on('/').splitToList(this.sourceName);
            return split.get(split.size() - 1);
        }

        String fullPathSourceName = this.macroElement.flatMap(AntlrElement::getCompilationUnit)
            .map(CompilationUnit::getFullPathSourceName)
            .orElseThrow();

        var abstractElement = this.macroElement.orElseThrow();
        Token startToken = abstractElement.getElementContext().getStart();

        return "%s:%d:%d --> %s".formatted(
            fullPathSourceName,
            startToken.getLine(),
            startToken.getCharPositionInLine(),
            this.sourceName
        );
    }

    @Nonnull
    public String getSourceCodeText() {
        return this.sourceCodeText;
    }

    @Nonnull
    public BufferedTokenStream getTokenStream() {
        return this.tokenStream;
    }

    @Nonnull
    public static CompilationUnit createFromFile(int ordinal, @Nonnull File file) {
        String sourceCodeText = CompilationUnit.slurp(file);
        String sourceName = file.getAbsolutePath();
        return CompilationUnit.createFromText(ordinal, Optional.empty(), sourceName, sourceCodeText);
    }

    @Nonnull
    public static CompilationUnit createFromClasspathLocation(
        int ordinal,
        @Nonnull String classpathLocation,
        @Nonnull ClassLoader classLoader
    ) {
        String sourceCodeText = CompilationUnit.slurp(classpathLocation, classLoader);
        URL resource = classLoader.getResource(classpathLocation);
        String file = resource.getFile();
        String sourceName = classpathLocation.contains("jar!/") ? classpathLocation : file;
        return CompilationUnit.createFromText(ordinal, Optional.empty(), sourceName, sourceCodeText);
    }

    @Nonnull
    public static CompilationUnit createFromClasspathLocation(int ordinal, @Nonnull String classpathLocation) {
        return CompilationUnit.createFromClasspathLocation(
            ordinal,
            classpathLocation,
            CompilationUnit.class.getClassLoader()
        );
    }

    @Nonnull
    private static String slurp(File file) {
        try (Scanner scanner = new Scanner(file, StandardCharsets.UTF_8).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    private static String slurp(@Nonnull String classpathLocation, @Nonnull ClassLoader classLoader) {
        InputStream inputStream = classLoader.getResourceAsStream(classpathLocation);
        Objects.requireNonNull(inputStream);
        return CompilationUnit.slurp(inputStream);
    }

    @Nonnull
    private static String slurp(@Nonnull InputStream inputStream) {
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    @Nonnull
    public static CompilationUnit createFromText(
        int ordinal,
        @Nonnull Optional<AntlrElement> macroElement,
        @Nonnull String sourceName,
        @Nonnull String sourceCodeText
    ) {
        return CompilationUnit.createFromText(
            ordinal,
            macroElement,
            sourceName,
            sourceCodeText,
            KlassParser::compilationUnit
        );
    }

    @Nonnull
    private static CompilationUnit createFromText(
        int ordinal,
        @Nonnull Optional<AntlrElement> macroElement,
        @Nonnull String sourceName,
        @Nonnull String sourceCodeText,
        @Nonnull Function<KlassParser, ? extends ParserRuleContext> parserRule
    ) {
        String[] lines = NEWLINE_PATTERN.split(sourceCodeText);
        ANTLRErrorListener errorListener = new ThrowingErrorListener(sourceName, lines);
        CodePointCharStream charStream = CharStreams.fromString(sourceCodeText, sourceName);
        KlassLexer lexer = CompilationUnit.getKlassLexer(errorListener, charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        KlassParser parser = CompilationUnit.getParser(errorListener, tokenStream);
        ParserRuleContext parserRuleContext = parserRule.apply(parser);
        return new CompilationUnit(ordinal, macroElement, sourceName, sourceCodeText, tokenStream, parserRuleContext);
    }

    @Nonnull
    private static KlassLexer getKlassLexer(@Nonnull ANTLRErrorListener errorListener, CodePointCharStream charStream) {
        KlassLexer lexer = new KlassLexer(charStream);
        lexer.addErrorListener(errorListener);
        return lexer;
    }

    @Nonnull
    private static KlassParser getParser(@Nonnull ANTLRErrorListener errorListener, CommonTokenStream tokenStream) {
        KlassParser parser = new KlassParser(tokenStream);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return parser;
    }

    @Nonnull
    public static CompilationUnit getMacroCompilationUnit(
        int ordinal,
        @Nonnull AntlrElement macroElement,
        @Nonnull AbstractCompilerPhase macroExpansionCompilerPhase,
        @Nonnull String sourceCodeText,
        @Nonnull Function<KlassParser, ? extends ParserRuleContext> parserRule
    ) {
        String sourceName = macroExpansionCompilerPhase.getName() + " macro";
        CompilationUnit result = CompilationUnit.createFromText(
            ordinal,
            Optional.of(macroElement),
            sourceName,
            sourceCodeText,
            parserRule
        );
        LOGGER.debug("{}\n{}\n", result.getFullPathSourceName(), sourceCodeText);
        return result;
    }

    @Override
    public String toString() {
        return this.sourceName;
    }

    public SourceCodeBuilderImpl build() {
        if (this.sourceCodeBuilder == null) {
            Optional<SourceCodeBuilderImpl> macroSourceCodeBuilder = this.macroElement.flatMap(
                AntlrElement::getCompilationUnit
            ).map(CompilationUnit::build);

            this.sourceCodeBuilder = this.getSourceCodeBuilder(macroSourceCodeBuilder);
        }
        return this.sourceCodeBuilder;
    }

    public void build2() {
        Optional<ElementBuilder<?>> macroElementBuilder = this.macroElement.map(AntlrElement::getElementBuilder);
        this.sourceCodeBuilder.setMacroElement(macroElementBuilder);
    }

    private SourceCodeBuilderImpl getSourceCodeBuilder(Optional<SourceCodeBuilderImpl> macroSourceCodeBuilder) {
        return new SourceCodeBuilderImpl(
            this.sourceName,
            this.sourceCodeText,
            this.tokenStream,
            this.parserContext,
            macroSourceCodeBuilder
        );
    }
}
