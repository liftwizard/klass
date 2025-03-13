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

package cool.klass.generator.service.test;

import java.util.Optional;

import cool.klass.generator.service.ServiceResourceGenerator;
import cool.klass.model.converter.compiler.CompilationResult;
import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.KlassCompiler;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import cool.klass.model.meta.domain.api.service.ServiceGroup;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(LogMarkerTestExtension.class)
public class ServiceResourceGeneratorTest
{
    @RegisterExtension
    final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

    @Test
    void stackOverflow()
    {
        String sourceCodeText = FileSlurper.slurp("/com/stackoverflow/stackoverflow.klass", this.getClass());

        CompilationUnit compilationUnit = CompilationUnit.createFromText(
                0,
                Optional.empty(),
                "example.klass",
                sourceCodeText);
        KlassCompiler     compiler          = new KlassCompiler(compilationUnit, ColorSchemeProvider.getByName("dark"));
        CompilationResult compilationResult = compiler.compile();

        if (compilationResult.domainModelWithSourceCode().isEmpty())
        {
            String message = compilationResult.compilerAnnotations().makeString("\n");
            fail(message);
        }
        else
        {
            DomainModelWithSourceCode domainModel = compilationResult.domainModelWithSourceCode().get();
            assertThat(domainModel).isNotNull();

            ServiceResourceGenerator serviceResourceGenerator = new ServiceResourceGenerator(
                    domainModel,
                    "StackOverflow",
                    "com.stackoverflow");

            ServiceGroup serviceGroup           = domainModel.getServiceGroups().getOnly();
            String       serviceGroupSourceCode = serviceResourceGenerator.getServiceGroupSourceCode(serviceGroup);

            this.fileMatchExtension.assertFileContents(
                    this.getClass().getSimpleName() + ".java",
                    serviceGroupSourceCode);
        }
    }
}
