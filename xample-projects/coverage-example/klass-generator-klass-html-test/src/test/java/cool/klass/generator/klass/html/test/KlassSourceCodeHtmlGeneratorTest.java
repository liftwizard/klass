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

package cool.klass.generator.klass.html.test;

import java.util.Optional;

import cool.klass.generator.klass.html.KlassSourceCodeHtmlGenerator;
import cool.klass.model.converter.compiler.CompilationResult;
import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.KlassCompiler;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(LogMarkerTestExtension.class)
public class KlassSourceCodeHtmlGeneratorTest {

    @RegisterExtension
    final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

    @Test
    void smokeTest() {
        String sourceCodeText = FileSlurper.slurp("/com/stackoverflow/stackoverflow.klass", this.getClass());
        CompilationUnit compilationUnit = CompilationUnit.createFromText(
            0,
            Optional.empty(),
            "example.klass",
            sourceCodeText
        );
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("dark");
        KlassCompiler compiler = new KlassCompiler(compilationUnit, colorScheme);
        CompilationResult compilationResult = compiler.compile();
        if (compilationResult.domainModelWithSourceCode().isEmpty()) {
            String message = compilationResult.compilerAnnotations().makeString("\n");
            fail(message);
        }

        DomainModelWithSourceCode domainModel = compilationResult.domainModelWithSourceCode().get();
        assertThat(domainModel).isNotNull();

        for (SourceCode sourceCode : domainModel.getSourceCodes()) {
            String fullPathSourceName = sourceCode.getFullPathSourceName();

            String html = KlassSourceCodeHtmlGenerator.getSourceCode(domainModel, sourceCode);

            String resourceClassPathLocation = fullPathSourceName + ".html";
            this.fileMatchExtension.assertFileContents(resourceClassPathLocation, html);
        }
    }
}
