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

package cool.klass.model.converter.compiler;

import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.annotation.AbstractCompilerAnnotation;
import cool.klass.model.converter.compiler.annotation.RootCompilerAnnotation;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(LogMarkerTestExtension.class)
class KlassCompilerTest
{
    @Test
    void stackOverflow()
    {
        this.assertNoCompilerErrors("/com/stackoverflow/stackoverflow.klass");
    }

    @Test
    void emoji()
    {
        this.assertNoCompilerErrors("emoji.klass");
    }

    @Test
    void projectionOnInterface()
    {
        this.assertNoCompilerErrors("projectionOnInterface.klass");
    }

    private void assertNoCompilerErrors(@Nonnull String sourceCodeName)
    {
        String sourceCodeText = FileSlurper.slurp(sourceCodeName, this.getClass());
        CompilationUnit compilationUnit = CompilationUnit.createFromText(
                0,
                Optional.empty(),
                sourceCodeName,
                sourceCodeText);
        KlassCompiler compiler = new KlassCompiler(compilationUnit);
        CompilationResult compilationResult = compiler.compile();
        ImmutableList<RootCompilerAnnotation> compilerAnnotations = compilationResult
                .compilerAnnotations()
                .select(AbstractCompilerAnnotation::isError);

        assertThat(compilerAnnotations).as(compilerAnnotations.makeString("\n")).isEqualTo(Lists.immutable.empty());

        assertThat(compilationResult.domainModelWithSourceCode()).isPresent();
    }
}
