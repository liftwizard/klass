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

package cool.klass.model.converter.compiler.syntax.highlighter.ansi.functional;

import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import org.fusesource.jansi.Ansi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(LogMarkerTestExtension.class)
class FunctionalSyntaxHighlighterExampleTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionalSyntaxHighlighterExampleTest.class);

    @Test
    void functionalDarkCubeColorScheme() {
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("craig-light");
        this.applyFunctionalColoring(colorScheme);
    }

    private void applyFunctionalColoring(AnsiColorScheme colorScheme) {
        String sourceCodeText = FileSlurper.slurp("/com/stackoverflow/stackoverflow.klass", this.getClass());
        String sourceName = "example.klass";

        Ansi ansi = AnsiSyntaxHighlighter.highlightSourceCode(sourceCodeText, sourceName, colorScheme);

        LOGGER.info("functionalText =\n{}", ansi);
    }
}
