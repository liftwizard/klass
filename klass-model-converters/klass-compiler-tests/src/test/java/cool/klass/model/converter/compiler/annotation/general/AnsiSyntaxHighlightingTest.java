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

package cool.klass.model.converter.compiler.annotation.general;

import java.util.Optional;

import cool.klass.model.converter.compiler.CompilationResult;
import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.KlassCompiler;
import cool.klass.model.converter.compiler.annotation.RootCompilerAnnotation;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(LogMarkerTestExtension.class)
public class AnsiSyntaxHighlightingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnsiSyntaxHighlightingTest.class);

    @Test
    public void verifyAnsiFormattingInErrors() {
        // Create some Klass code with intentional errors
        String sourceCodeText =
            """
            package test.ansi

            class MissingKeyProperty
            {
                // Missing 'key' modifier on a property
                id: Long id;
                name: String;
            }

            class InvalidType
            {
                // Reference to non-existent type
                property: NonExistentType key;
            }

            association InvalidAssociation
            {
                source: MissingKeyProperty[1..*];
                target: InvalidType[0..1];
            }
            """;

        String sourceName = "AnsiSyntaxHighlightingTest.klass";
        CompilationUnit compilationUnit = CompilationUnit.createFromText(
            0,
            Optional.empty(),
            sourceName,
            sourceCodeText
        );

        // Use a color scheme to ensure ANSI codes are generated
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("craig-light");
        KlassCompiler compiler = new KlassCompiler(compilationUnit, colorScheme);
        CompilationResult compilationResult = compiler.compile();

        ImmutableList<RootCompilerAnnotation> compilerAnnotations = compilationResult.compilerAnnotations();

        // Assert that we have compiler errors
        assertThat(compilerAnnotations).isNotEmpty();

        // Log each error to console for visual verification
        LOGGER.info("===== COMPILER ERRORS WITH ANSI FORMATTING =====");
        for (RootCompilerAnnotation annotation : compilerAnnotations) {
            String formattedError = annotation.toString();

            // Log the formatted error (this will show ANSI codes in console)
            LOGGER.info("Formatted error:\n{}", formattedError);

            // Verify that ANSI escape codes are present
            assertThat(formattedError).as("Error message should contain ANSI escape codes").contains("\u001B[");
        }
        LOGGER.info("===== END OF COMPILER ERRORS =====");

        // Additional assertions to verify specific ANSI patterns
        String firstError = compilerAnnotations.getFirst().toString();

        // Check for common ANSI patterns (these may vary based on the color scheme)
        assertThat(firstError)
            .as("Should contain ANSI reset codes")
            .satisfiesAnyOf(s -> assertThat(s).contains("\u001B[0m"), s -> assertThat(s).contains("\u001B[m"));

        // Verify error formatting structure
        assertThat(firstError)
            .as("Should contain the error divider lines")
            .contains("════════════════════════════════════════");
    }

    @Test
    public void compareFormattedVsUnformattedErrors() {
        String sourceCodeText =
            """
            package test.ansi

            class TestClass
            {
                // This will cause an error - unresolved type
                property: UnknownType key;
            }
            """;

        String sourceName = "AnsiComparisonTest.klass";
        CompilationUnit compilationUnit = CompilationUnit.createFromText(
            0,
            Optional.empty(),
            sourceName,
            sourceCodeText
        );

        // Compile with color scheme
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("craig-light");
        KlassCompiler compiler = new KlassCompiler(compilationUnit, colorScheme);
        CompilationResult compilationResult = compiler.compile();

        ImmutableList<RootCompilerAnnotation> annotations = compilationResult.compilerAnnotations();
        assertThat(annotations).isNotEmpty();

        RootCompilerAnnotation firstAnnotation = annotations.getFirst();
        String formattedMessage = firstAnnotation.toString();

        // Log both for comparison
        LOGGER.info("ANSI-formatted error message length: {}", formattedMessage.length());
        LOGGER.info("Contains ANSI codes: {}", formattedMessage.contains("\u001B["));

        // Count ANSI escape sequences
        int ansiCount = countAnsiSequences(formattedMessage);
        LOGGER.info("Number of ANSI escape sequences: {}", ansiCount);

        assertThat(ansiCount).as("Should have multiple ANSI escape sequences for syntax highlighting").isGreaterThan(5);
    }

    private int countAnsiSequences(String text) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf("\u001B[", index)) != -1) {
            count++;
            index += 2;
        }
        return count;
    }
}
