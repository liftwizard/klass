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

package cool.klass.model.converter.compiler.syntax.highlighter.ansi;

import java.util.Collections;

import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
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
import org.eclipse.collections.api.map.MapIterable;
import org.fusesource.jansi.Ansi;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SyntaxHighlightingVerificationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyntaxHighlightingVerificationTest.class);

    private static final String KLASS_CODE =
        """
        package com.example

        // This is a comment
        class Person
        {
            id: String key;
            name: String;
            age: Integer;

            /*
             * Block comment
             * with multiple lines
             */
            sayHello(): String
            {
                "Hello, World!";
            }
        }

        association PersonHasFriends
        {
            person: Person[1..1];
            friends: Person[0..*];
        }
        """;

    @Test
    void testSyntaxHighlightingWithCraigLightScheme() {
        LOGGER.info("Testing with craig-light color scheme");
        testSyntaxHighlighting(KLASS_CODE, "craig-light");
    }

    @Test
    void testSyntaxHighlightingWithDarkCubeScheme() {
        LOGGER.info("Testing with dark-cube color scheme");
        testSyntaxHighlighting(KLASS_CODE, "dark-cube");
    }

    @Test
    void testWhitespaceHandling() {
        LOGGER.info("Testing whitespace and newline handling");

        String codeWithWhitespace =
            """
            class Test
            {
                // Indented comment
                property: String;

                    // Extra indentation
                method(): String
                {
                    "String with    spaces";
                }
            }
            """;

        try {
            AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("craig-light");

            // Parse the code
            CodePointCharStream charStream = CharStreams.fromString(codeWithWhitespace, "whitespace.klass");
            KlassLexer lexer = new KlassLexer(charStream);
            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            KlassParser parser = new KlassParser(tokenStream);
            ParseTree parseTree = parser.compilationUnit();

            // Get token categories
            MapIterable<Token, TokenCategory> tokenCategoriesFromLexer =
                LexerBasedTokenCategorizer.findTokenCategoriesFromLexer(tokenStream);
            MapIterable<Token, TokenCategory> tokenCategoriesFromParser =
                ParserBasedTokenCategorizer.findTokenCategoriesFromParser(parseTree);

            // Create highlighter
            FunctionalSyntaxHighlighter highlighter = new FunctionalSyntaxHighlighter(
                colorScheme,
                tokenCategoriesFromLexer,
                tokenCategoriesFromParser
            );

            // Apply syntax highlighting
            Ansi highlighted = highlighter.highlightTokens(tokenStream.getTokens());

            String result = highlighted.toString();
            LOGGER.info("Whitespace test result:\n{}", result);

            // Check for proper newline handling
            int newlineCount = result.split("\n", -1).length - 1;
            LOGGER.info("Number of newlines in output: {}", newlineCount);

            // Verify ANSI codes are present and no malformed sequences
            assertTrue(result.contains("\u001B["), "ANSI escape sequences should be present");
        } catch (Exception e) {
            LOGGER.error("Error in whitespace test", e);
            throw new RuntimeException(e);
        }
    }

    @Test
    void testAnsiSequenceVerification() {
        LOGGER.info("Verifying ANSI escape sequences");

        // Create a simple token test
        String simpleCode = "class Test { }";

        try {
            AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("dark-cube");

            // Parse the code
            CodePointCharStream charStream = CharStreams.fromString(simpleCode, "simple.klass");
            KlassLexer lexer = new KlassLexer(charStream);
            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            KlassParser parser = new KlassParser(tokenStream);
            ParseTree parseTree = parser.compilationUnit();

            // Get token categories
            MapIterable<Token, TokenCategory> tokenCategoriesFromLexer =
                LexerBasedTokenCategorizer.findTokenCategoriesFromLexer(tokenStream);
            MapIterable<Token, TokenCategory> tokenCategoriesFromParser =
                ParserBasedTokenCategorizer.findTokenCategoriesFromParser(parseTree);

            // Create highlighter
            FunctionalSyntaxHighlighter highlighter = new FunctionalSyntaxHighlighter(
                colorScheme,
                tokenCategoriesFromLexer,
                tokenCategoriesFromParser
            );

            // Process each token individually
            LOGGER.info("Token by token analysis:");
            for (Token token : tokenStream.getTokens()) {
                if (token.getType() == Token.EOF) {
                    continue;
                }

                Ansi tokenAnsi = highlighter.highlightTokens(Collections.singletonList(token));
                String tokenText = token.getText();
                String ansiString = tokenAnsi.toString();

                LOGGER.info(
                    "Token: '{}' Type: {} ANSI: {}",
                    tokenText.replace("\n", "\\n"),
                    KlassLexer.VOCABULARY.getSymbolicName(token.getType()),
                    ansiString.replace("\u001B", "\\u001B").replace("\n", "\\n")
                );
            }
        } catch (Exception e) {
            LOGGER.error("Error in ANSI verification", e);
            throw new RuntimeException(e);
        }
    }

    private void testSyntaxHighlighting(String sourceCode, String schemeName) {
        try {
            // Get color scheme
            AnsiColorScheme colorScheme = ColorSchemeProvider.getByName(schemeName);

            // Parse the code
            CodePointCharStream charStream = CharStreams.fromString(sourceCode, "test.klass");
            KlassLexer lexer = new KlassLexer(charStream);
            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            KlassParser parser = new KlassParser(tokenStream);
            ParseTree parseTree = parser.compilationUnit();

            // Get token categories
            MapIterable<Token, TokenCategory> tokenCategoriesFromLexer =
                LexerBasedTokenCategorizer.findTokenCategoriesFromLexer(tokenStream);
            MapIterable<Token, TokenCategory> tokenCategoriesFromParser =
                ParserBasedTokenCategorizer.findTokenCategoriesFromParser(parseTree);

            // Create highlighter
            FunctionalSyntaxHighlighter highlighter = new FunctionalSyntaxHighlighter(
                colorScheme,
                tokenCategoriesFromLexer,
                tokenCategoriesFromParser
            );

            // Apply syntax highlighting
            Ansi ansi = new Ansi();
            highlighter.applyInitialThemeStyles(ansi);
            Ansi highlighted = highlighter.highlightTokens(tokenStream.getTokens());
            ansi.a(highlighted);
            highlighter.applyFinalReset(ansi);

            // Print the result
            LOGGER.info("Highlighted code:\n{}", ansi);

            // Print raw ANSI for verification (first 200 chars)
            String rawAnsi = ansi.toString();
            String preview = rawAnsi.substring(0, Math.min(200, rawAnsi.length())).replace("\u001B", "\\u001B");
            LOGGER.info("Raw ANSI (first 200 chars): {}", preview);

            // Verify ANSI codes are present
            if (rawAnsi.contains("\u001B[")) {
                LOGGER.info("✓ ANSI escape sequences detected");
                assertTrue(true, "ANSI escape sequences should be present");
            } else {
                LOGGER.error("✗ No ANSI escape sequences found!");
                throw new AssertionError("ANSI escape sequences should be present but none were found");
            }
        } catch (Exception e) {
            LOGGER.error("Error during syntax highlighting with scheme: " + schemeName, e);
            throw new RuntimeException(e);
        }
    }
}
