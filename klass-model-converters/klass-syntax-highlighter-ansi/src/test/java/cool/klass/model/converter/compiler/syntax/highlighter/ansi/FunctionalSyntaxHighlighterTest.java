/*
 * Copyright 2024 Craig Motlin
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

package cool.klass.model.converter.compiler.syntax.highlighter.ansi;

import java.util.Arrays;

import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import cool.klass.model.converter.compiler.token.categories.TokenCategory;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(LogMarkerTestExtension.class)
class FunctionalSyntaxHighlighterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionalSyntaxHighlighterTest.class);

    static String[] colorSchemeProvider() {
        return new String[] { "light", "light-rgb", "dark", "dark-rgb", "dark-cube", "craig-light", "craig-light" };
    }

    @Test
    void basicHighlighting() {
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("craig-light");

        var keywordToken = new CommonToken(1, "class");
        var identifierToken = new CommonToken(2, "Example");

        MapIterable<Token, TokenCategory> tokenCategoriesFromParser = Maps.mutable.with(
            keywordToken,
            TokenCategory.KEYWORD_CLASS,
            identifierToken,
            TokenCategory.CLASS_NAME
        );

        var highlighter = new FunctionalSyntaxHighlighter(
            colorScheme,
            Maps.immutable.empty(),
            tokenCategoriesFromParser
        );

        Ansi ansi = highlighter.highlightTokens(Lists.immutable.with(keywordToken, identifierToken));

        String result = ansi.toString();
        LOGGER.info("Basic highlighting test result: {}", result);

        assertThat(result).contains("class");
        assertThat(result).contains("Example");
    }

    @Test
    void uncategorizedTokens() {
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("craig-light");

        var uncategorizedToken = new CommonToken(1, "uncategorized");

        var highlighter = new FunctionalSyntaxHighlighter(colorScheme, Maps.immutable.empty(), Maps.immutable.empty());

        Ansi ansi = highlighter.highlightTokens(Lists.immutable.with(uncategorizedToken));

        String result = ansi.toString();
        LOGGER.info("Uncategorized token test result: {}", result);

        assertThat(result).contains("uncategorized");
    }

    @Test
    void emptyInput() {
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("craig-light");

        var highlighter = new FunctionalSyntaxHighlighter(colorScheme, Maps.immutable.empty(), Maps.immutable.empty());

        Ansi ansi = highlighter.highlightTokens(Lists.immutable.empty());

        String result = ansi.toString();
        LOGGER.info("Empty input test result: {}", result);

        assertThat(result).isNotBlank(); // Should have at least the ANSI background color setting
        assertThat(result).contains(Attribute.RESET.toString()); // Should contain the reset code
    }

    @Test
    void resetStyle() {
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("craig-light");

        var keywordToken1 = new CommonToken(1, "keyword1");
        var keywordToken2 = new CommonToken(2, "keyword2");

        MapIterable<Token, TokenCategory> tokenCategories = Maps.mutable.with(
            keywordToken1,
            TokenCategory.KEYWORD,
            keywordToken2,
            TokenCategory.KEYWORD
        );

        var highlighter = new FunctionalSyntaxHighlighter(colorScheme, Maps.immutable.empty(), tokenCategories);

        Ansi ansi1 = highlighter.highlightTokens(Lists.immutable.with(keywordToken1));
        String result1 = ansi1.toString();
        LOGGER.info("Before reset: {}", result1);

        highlighter.resetStyle();

        Ansi ansi2 = highlighter.highlightTokens(Lists.immutable.with(keywordToken2));
        String result2 = ansi2.toString();
        LOGGER.info("After reset: {}", result2);

        assertThat(result1).contains("keyword1");
        assertThat(result2).contains("RESET");
    }

    @Test
    void applyFinalReset() {
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("craig-light");

        var highlighter = new FunctionalSyntaxHighlighter(colorScheme, Maps.immutable.empty(), Maps.immutable.empty());

        Ansi ansi = Ansi.ansi();
        highlighter.applyFinalReset(ansi);
        String result = ansi.toString();

        LOGGER.info("Apply final reset result: {}", result);

        assertThat(result).contains("RESET");
    }

    @Test
    void styleTransitions() {
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("craig-light");

        var keywordToken = new CommonToken(1, "class");
        var identifierToken = new CommonToken(2, "Example");
        var punctuationToken = new CommonToken(3, "{");

        MapIterable<Token, TokenCategory> tokenCategories = Maps.mutable.with(
            keywordToken,
            TokenCategory.KEYWORD_CLASS,
            identifierToken,
            TokenCategory.CLASS_NAME,
            punctuationToken,
            TokenCategory.PUNCTUATION
        );

        var highlighter = new FunctionalSyntaxHighlighter(colorScheme, Maps.immutable.empty(), tokenCategories);

        Ansi ansi = highlighter.highlightTokens(Arrays.asList(keywordToken, identifierToken, punctuationToken));

        String result = ansi.toString();
        LOGGER.info("Style transition test result: {}", result);

        assertThat(result).contains("class");
        assertThat(result).contains("Example");
        assertThat(result).contains("{");
    }

    @ParameterizedTest
    @MethodSource("colorSchemeProvider")
    void allColorSchemes(String schemeName) {
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName(schemeName);

        var keywordToken = new CommonToken(1, "class");
        var identifierToken = new CommonToken(2, "Example");
        var whitespaceToken = new CommonToken(3, "   ");
        var newlineToken = new CommonToken(4, "\n");

        MapIterable<Token, TokenCategory> lexerTokenCategories = Maps.mutable.with(
            whitespaceToken,
            TokenCategory.WHITESPACE,
            newlineToken,
            TokenCategory.NEWLINE
        );

        MapIterable<Token, TokenCategory> parserTokenCategories = Maps.mutable.with(
            keywordToken,
            TokenCategory.KEYWORD_CLASS,
            identifierToken,
            TokenCategory.CLASS_NAME
        );

        var highlighter = new FunctionalSyntaxHighlighter(colorScheme, lexerTokenCategories, parserTokenCategories);

        Ansi ansi = highlighter.highlightTokens(
            Lists.immutable.with(keywordToken, whitespaceToken, identifierToken, newlineToken)
        );

        String result = ansi.toString();
        LOGGER.info("{} color scheme test result: {}", schemeName, result);

        assertThat(result).contains("class");
        assertThat(result).contains("···"); // whitespace replaced with middle dots
        assertThat(result).contains("Example");
        assertThat(result).contains("¶"); // newline replaced with pilcrow
    }

    @Test
    void lexerAndParserTokenPriority() {
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("craig-light");

        var token = new CommonToken(1, "test");

        MapIterable<Token, TokenCategory> lexerTokenCategories = Maps.mutable.with(token, TokenCategory.KEYWORD);
        MapIterable<Token, TokenCategory> parserTokenCategories = Maps.mutable.with(token, TokenCategory.CLASS_NAME);

        var highlighter = new FunctionalSyntaxHighlighter(colorScheme, lexerTokenCategories, parserTokenCategories);

        Ansi ansi = highlighter.highlightTokens(Lists.immutable.with(token));

        String result = ansi.toString();
        LOGGER.info("Lexer and parser token priority test result: {}", result);

        assertThat(result).contains("test");
    }
}
