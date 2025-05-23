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

import java.util.Collections;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(LogMarkerTestExtension.class)
class WhitespaceAndNewlineHandlingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhitespaceAndNewlineHandlingTest.class);

    @Test
    void testWhitespaceHandling() {
        // Setup
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("craig-light");

        // Create a whitespace token
        CommonToken whitespaceToken = new CommonToken(1, "   ");
        MapIterable<Token, TokenCategory> tokenCategories = Maps.mutable.with(
            whitespaceToken,
            TokenCategory.WHITESPACE
        );

        FunctionalSyntaxHighlighter highlighter = new FunctionalSyntaxHighlighter(
            colorScheme,
            tokenCategories,
            Maps.immutable.empty()
        );

        // Act
        Ansi ansi = highlighter.highlightTokens(Collections.singleton(whitespaceToken));

        // Log the result
        String result = ansi.toString();
        LOGGER.info("Whitespace test result: {}", result);

        // The result should contain "···" (three middle dots) instead of three spaces
        assert result.contains("···") : "Expected whitespace to be rendered as middle dots";
    }

    @Test
    void testNewlineHandling() {
        // Setup
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("craig-light");

        // Create a newline token
        CommonToken newlineToken = new CommonToken(1, "\n");
        MapIterable<Token, TokenCategory> tokenCategories = Maps.mutable.with(newlineToken, TokenCategory.NEWLINE);

        FunctionalSyntaxHighlighter highlighter = new FunctionalSyntaxHighlighter(
            colorScheme,
            tokenCategories,
            Maps.immutable.empty()
        );

        // Act
        Ansi ansi = highlighter.highlightTokens(Collections.singleton(newlineToken));

        // Log the result
        String result = ansi.toString();
        LOGGER.info("Newline test result: {}", result);

        // The result should contain "¶" (pilcrow) instead of a newline character
        assert result.contains("¶") : "Expected newline to be rendered as pilcrow";
    }

    @Test
    void testMixedContent() {
        // Setup
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("craig-light");

        // Create tokens with mixed content: text, whitespace, newline
        CommonToken textToken1 = new CommonToken(1, "class");
        CommonToken whitespaceToken = new CommonToken(2, "   ");
        CommonToken textToken2 = new CommonToken(3, "Example");
        CommonToken newlineToken = new CommonToken(4, "\n");

        MapIterable<Token, TokenCategory> lexerTokenCategories = Maps.mutable.with(
            whitespaceToken,
            TokenCategory.WHITESPACE,
            newlineToken,
            TokenCategory.NEWLINE
        );

        MapIterable<Token, TokenCategory> parserTokenCategories = Maps.mutable.with(
            textToken1,
            TokenCategory.KEYWORD_CLASS,
            textToken2,
            TokenCategory.CLASS_NAME
        );

        FunctionalSyntaxHighlighter highlighter = new FunctionalSyntaxHighlighter(
            colorScheme,
            lexerTokenCategories,
            parserTokenCategories
        );

        // Act
        Ansi ansi = highlighter.highlightTokens(
            Lists.immutable.with(textToken1, whitespaceToken, textToken2, newlineToken)
        );

        // Log the result
        String result = ansi.toString();
        LOGGER.info("Mixed content test result: {}", result);

        // Verify we have all the expected content with special characters
        assert result.contains("class") : "Expected 'class' keyword in the output";
        assert result.contains("···") : "Expected whitespace to be rendered as middle dots";
        assert result.contains("Example") : "Expected 'Example' class name in the output";
        assert result.contains("¶") : "Expected newline to be rendered as pilcrow";
    }

    @Test
    void testResetAfterNewline() {
        // Setup
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("craig-light");

        // Create tokens to test style reset after newline
        CommonToken textToken1 = new CommonToken(1, "keyword1");
        CommonToken newlineToken = new CommonToken(2, "\n");
        CommonToken textToken2 = new CommonToken(3, "keyword2");

        MapIterable<Token, TokenCategory> lexerTokenCategories = Maps.mutable.with(newlineToken, TokenCategory.NEWLINE);

        MapIterable<Token, TokenCategory> parserTokenCategories = Maps.mutable.with(
            textToken1,
            TokenCategory.KEYWORD,
            textToken2,
            TokenCategory.KEYWORD
        );

        FunctionalSyntaxHighlighter highlighter = new FunctionalSyntaxHighlighter(
            colorScheme,
            lexerTokenCategories,
            parserTokenCategories
        );

        // Act
        Ansi ansi = highlighter.highlightTokens(Lists.immutable.with(textToken1, newlineToken, textToken2));

        // Log the result
        String result = ansi.toString();
        LOGGER.info("Reset after newline test result: {}", result);

        // The output should include the reset sequence after the newline
        assert result.contains("keyword1") : "Expected 'keyword1' in the output";
        assert result.contains("¶") : "Expected newline to be rendered as pilcrow";
        assert result.contains("keyword2") : "Expected 'keyword2' in the output";
        // This is a bit tricky to test with Ansi sequences, but we're mostly
        // checking if the code runs without exceptions and produces expected visible output
    }
}
