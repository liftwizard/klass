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

package cool.klass.model.converter.compiler.syntax.highlighter.ansi.functional;

import java.time.Duration;

import javax.annotation.Nonnull;

import com.google.common.base.Stopwatch;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.functional.AnsiStreamWriter.StyledToken;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.dto.StyleSettings;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.util.StyleConversionUtils;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.util.TokenFormatUtils;
import cool.klass.model.converter.compiler.token.categories.TokenCategory;
import cool.klass.model.converter.compiler.token.categorizing.lexer.LexerBasedTokenCategorizer;
import cool.klass.model.converter.compiler.token.categorizing.parser.ParserBasedTokenCategorizer;
import cool.klass.model.meta.grammar.KlassLexer;
import cool.klass.model.meta.grammar.KlassParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AnsiSyntaxHighlighter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnsiSyntaxHighlighter.class);

    private AnsiSyntaxHighlighter() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    @Nonnull
    public static Ansi highlightSourceCode(
        @Nonnull String sourceCodeText,
        @Nonnull String sourceName,
        @Nonnull AnsiColorScheme colorScheme
    ) {
        // Get theme defaults for foreground and background
        StyleSettings foregroundSettings = colorScheme.getStyleSettings(TokenCategory.IDENTIFIER);
        StyleSettings backgroundSettings = colorScheme.getStyleSettings(null);

        Object defaultForeground = foregroundSettings.foreground();
        Object defaultBackground = backgroundSettings.background();

        return highlightSourceCode(sourceCodeText, sourceName, colorScheme, defaultForeground, defaultBackground);
    }

    @Nonnull
    public static Ansi highlightSourceCode(
        @Nonnull String sourceCodeText,
        @Nonnull String sourceName,
        @Nonnull AnsiColorScheme colorScheme,
        @Nonnull Object defaultForeground,
        @Nonnull Object defaultBackground
    ) {
        Stopwatch lexerStopwatch = Stopwatch.createStarted();
        CodePointCharStream charStream = CharStreams.fromString(sourceCodeText, sourceName);
        KlassLexer lexer = new KlassLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        lexerStopwatch.stop();
        Duration elapsedLexer = lexerStopwatch.elapsed();
        LOGGER.debug("elapsedLexer = {}", elapsedLexer);

        Stopwatch parserStopwatch = Stopwatch.createStarted();
        KlassParser parser = new KlassParser(tokenStream);
        ParseTree parseTree = parser.compilationUnit();
        parserStopwatch.stop();
        Duration elapsedParser = parserStopwatch.elapsed();
        LOGGER.debug("elapsedParser = {}", elapsedParser);

        Stopwatch tokenCategorizerStopwatch = Stopwatch.createStarted();
        MapIterable<Token, TokenCategory> tokenCategoriesFromLexer =
            LexerBasedTokenCategorizer.findTokenCategoriesFromLexer(tokenStream);
        MapIterable<Token, TokenCategory> tokenCategoriesFromParser =
            ParserBasedTokenCategorizer.findTokenCategoriesFromParser(parseTree);
        tokenCategorizerStopwatch.stop();
        Duration elapsedTokenCategorizer = tokenCategorizerStopwatch.elapsed();
        LOGGER.debug("elapsedTokenCategorizer = {}", elapsedTokenCategorizer);

        Stopwatch functionalStopwatch = Stopwatch.createStarted();

        MutableList<StyledToken> styledTokens = Lists.mutable.empty();

        for (Token token : tokenStream.getTokens()) {
            TokenCategory category = TokenFormatUtils.getTokenCategory(
                token,
                tokenCategoriesFromLexer,
                tokenCategoriesFromParser
            ).orElse(null);

            AnsiColorStyle style = category != null
                ? convertCategoryToStyle(category, colorScheme)
                : AnsiColorStyle.EMPTY;

            String formattedText = TokenFormatUtils.formatTokenText(token, category);

            styledTokens.add(new StyledToken(style, formattedText));
        }

        Ansi ansi = AnsiStreamWriter.write(styledTokens.toImmutable(), defaultForeground, defaultBackground);

        functionalStopwatch.stop();
        Duration elapsedFunctional = functionalStopwatch.elapsed();
        LOGGER.debug("elapsedFunctional = {}", elapsedFunctional);

        return ansi;
    }

    private static AnsiColorStyle convertCategoryToStyle(TokenCategory category, AnsiColorScheme colorScheme) {
        StyleSettings styleSettings = colorScheme.getStyleSettings(category);
        return StyleConversionUtils.convertToAnsiColorStyle(styleSettings);
    }
}
