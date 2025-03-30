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

package cool.klass.model.converter.compiler.syntax.highlighter;

import java.time.Duration;

import com.google.common.base.Stopwatch;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.AnsiTokenColorizer;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import cool.klass.model.converter.compiler.token.categories.TokenCategory;
import cool.klass.model.converter.compiler.token.categorizing.lexer.LexerBasedTokenCategorizer;
import cool.klass.model.converter.compiler.token.categorizing.parser.ParserBasedTokenCategorizer;
import cool.klass.model.meta.grammar.KlassLexer;
import cool.klass.model.meta.grammar.KlassParser;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.collections.api.map.MapIterable;
import org.fusesource.jansi.Ansi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(LogMarkerTestExtension.class)
class SyntaxHighlighterListenerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyntaxHighlighterListenerTest.class);

    @Test
    void lightColorScheme() {
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("light");
        this.testColorScheme(colorScheme);
    }

    @Test
    void darkColorScheme() {
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("dark");
        this.testColorScheme(colorScheme);
    }

    @Test
    void darkCubeColorScheme() {
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("dark");
        this.testColorScheme(colorScheme);
    }

    @Test
    void rgbColorScheme() {
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("dark");
        this.testColorScheme(colorScheme);
    }

    private void testColorScheme(AnsiColorScheme colorScheme) {
        Stopwatch lexerStopwatch = Stopwatch.createStarted();
        String sourceCodeText = FileSlurper.slurp("/com/stackoverflow/stackoverflow.klass", this.getClass());
        String sourceName = "example.klass";
        CodePointCharStream charStream = CharStreams.fromString(sourceCodeText, sourceName);
        KlassLexer lexer = new KlassLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        lexerStopwatch.stop();
        Duration elapsedLexer = lexerStopwatch.elapsed();
        LOGGER.info("elapsedLexer = {}", elapsedLexer);

        Stopwatch parserStopwatch = Stopwatch.createStarted();
        KlassParser parser = new KlassParser(tokenStream);
        ParseTree parseTree = parser.compilationUnit();
        parserStopwatch.stop();
        Duration elapsedParser = parserStopwatch.elapsed();
        LOGGER.info("elapsedParser = {}", elapsedParser);

        Stopwatch tokenCategorizerStopwatch = Stopwatch.createStarted();
        MapIterable<Token, TokenCategory> tokenCategoriesFromLexer =
            LexerBasedTokenCategorizer.findTokenCategoriesFromLexer(tokenStream);
        MapIterable<Token, TokenCategory> tokenCategoriesFromParser =
            ParserBasedTokenCategorizer.findTokenCategoriesFromParser(parseTree);
        tokenCategorizerStopwatch.stop();
        Duration elapsedTokenCategorizer = tokenCategorizerStopwatch.elapsed();
        LOGGER.info("elapsedTokenCategorizer = {}", elapsedTokenCategorizer);

        Stopwatch rewriteStopwatch = Stopwatch.createStarted();
        AnsiTokenColorizer ansiTokenColorizer = new AnsiTokenColorizer(
            colorScheme,
            tokenCategoriesFromLexer,
            tokenCategoriesFromParser
        );

        Ansi ansi = Ansi.ansi();
        colorScheme.background(ansi);

        tokenStream.getTokens().forEach(token -> ansiTokenColorizer.colorizeText(ansi, token));
        ansi.reset();

        rewriteStopwatch.stop();
        Duration elapsedRewrite = rewriteStopwatch.elapsed();
        LOGGER.info("elapsedRewrite = {}", elapsedRewrite);

        LOGGER.info("rewriteText =\n{}", ansi);
    }
}
