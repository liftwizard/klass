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
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.functional.AnsiSyntaxHighlighter;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import org.fusesource.jansi.Ansi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(LogMarkerTestExtension.class)
class SyntaxHighlighterListenerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyntaxHighlighterListenerTest.class);

    static String[] colorSchemeProvider() {
        return new String[] { "light", "light-rgb", "dark", "dark-rgb", "dark-cube", "craig-light", "craig-dark" };
    }

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
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("craig-light");
        this.testColorScheme(colorScheme);
    }

    private void testColorScheme(AnsiColorScheme colorScheme) {
        String sourceCodeText = FileSlurper.slurp("/com/stackoverflow/stackoverflow.klass", this.getClass());
        String sourceName = "example.klass";

        Stopwatch stopwatch = Stopwatch.createStarted();
        Ansi ansi = AnsiSyntaxHighlighter.highlightSourceCode(sourceCodeText, sourceName, colorScheme);
        stopwatch.stop();
        Duration elapsed = stopwatch.elapsed();
        LOGGER.info("elapsed = {}", elapsed);

        LOGGER.info("highlightedText =\n{}", ansi);
    }

    @ParameterizedTest
    @MethodSource("colorSchemeProvider")
    void colorScheme(String schemeName) {
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName(schemeName);
        String sourceCodeText = FileSlurper.slurp("/com/stackoverflow/stackoverflow.klass", this.getClass());
        String sourceName = "example.klass";

        Stopwatch stopwatch = Stopwatch.createStarted();
        Ansi ansi = AnsiSyntaxHighlighter.highlightSourceCode(sourceCodeText, sourceName, colorScheme);
        stopwatch.stop();
        Duration elapsed = stopwatch.elapsed();
        LOGGER.info("elapsed = {}", elapsed);

        LOGGER.info("highlightedText =\n{}", ansi);
    }
}
