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

package cool.klass.model.converter.compiler.annotation;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationResult;
import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.KlassCompiler;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(LogMarkerTestExtension.class)
public abstract class AbstractKlassCompilerErrorTestCase
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractKlassCompilerErrorTestCase.class);

    @RegisterExtension
    final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

    @Test
    public void smokeTest()
    {
        this.assertCompilerErrors();
    }

    @Nonnull
    private String getTestName()
    {
        return this.getClass().getSimpleName();
    }

    @Nonnull
    private String getSourceName()
    {
        return this.getTestName() + ".klass";
    }

    private String getSourceCodeText()
    {
        return FileSlurper.slurp(this.getSourceName(), this.getClass());
    }

    @Nonnull
    private CompilationResult compile(String sourceCodeText)
    {
        String sourceName = this.getSourceName();
        CompilationUnit compilationUnit = CompilationUnit.createFromText(
                0,
                Optional.empty(),
                sourceName,
                sourceCodeText);
        var compiler = new KlassCompiler(compilationUnit);
        return compiler.compile();
    }

    private void assertCompilerAnnotationsExist(
            CompilationResult compilationResult,
            String testName,
            String sourceCodeText)
    {
        ImmutableList<RootCompilerAnnotation> compilerAnnotations = compilationResult.compilerAnnotations();
        assertThat(compilerAnnotations)
                .as("Expected a compile error but found:\n" + sourceCodeText)
                .isNotEmpty();

        for (RootCompilerAnnotation compilerAnnotation : compilerAnnotations)
        {
            String annotationSourceName = "%s-%s-%d-%s.log".formatted(
                    testName,
                    compilerAnnotation.getLines().toReversed().makeString("_"),
                    compilerAnnotation.getCharPositionInLine(),
                    compilerAnnotation.getAnnotationCode());

            this.fileMatchExtension.assertFileContents(
                    annotationSourceName,
                    compilerAnnotation.toString());
        }

        ImmutableListMultimap<Object, RootCompilerAnnotation> annotationsByKey =
                compilerAnnotations.groupBy(this::getAnnotationKey);
        annotationsByKey.forEachKeyMultiValues((key, compilerAnnotationsForKey) ->
        {
            if (compilerAnnotationsForKey.size() > 1)
            {
                for (RootCompilerAnnotation compilerAnnotation : compilerAnnotationsForKey)
                {
                    LOGGER.warn("Found compiler annotation:\n{}", compilerAnnotation);
                }
                fail("Found multiple compiler annotations for key: " + key);
            }
        });
    }

    private ImmutableList<Object> getAnnotationKey(RootCompilerAnnotation rootCompilerAnnotation)
    {
        String filenameWithoutDirectory = rootCompilerAnnotation.getFilenameWithoutDirectory();
        int line = rootCompilerAnnotation.getLine();
        int charPositionInLine = rootCompilerAnnotation.getCharPositionInLine();
        String annotationCode = rootCompilerAnnotation.getAnnotationCode();
        ImmutableList<Object> result = Lists.immutable.with(
                filenameWithoutDirectory,
                line,
                charPositionInLine,
                annotationCode);
        return result;
    }

    private void assertCompilerAnnotationsDoNotExist(CompilationResult compilationResult, String sourceCodeText)
    {
        ImmutableList<RootCompilerAnnotation> compilerAnnotations = compilationResult.compilerAnnotations();
        assertThat(compilerAnnotations)
                .as("Expected no compiler errors but found:\n" + sourceCodeText)
                .isEmpty();
    }

    private void assertCompilerAnnotationFilesDoNotExist()
    {
        String packagePath = this.getClass().getPackage().getName().replace('.', '/');
        File resourcesDir = new File("src/test/resources/" + packagePath);

        if (resourcesDir.exists() && resourcesDir.isDirectory())
        {
            String errorPattern = "-\\d+(_\\d+)*-\\d+-ERR_.*\\.log";
            FilenameFilter errorFileFilter = (dir, name) -> name.matches(this.getTestName() + errorPattern);
            File[] errorFiles = resourcesDir.listFiles(errorFileFilter);

            assertThat(errorFiles)
                    .as(
                            "No error files should exist for %s in %s",
                            this.getTestName(),
                            "src/test/resources/" + packagePath)
                    .isEmpty();
        }
    }

    private void assertDomainModelDoesNotExist(CompilationResult compilationResult, String sourceCodeText)
    {
        if (compilationResult.domainModelWithSourceCode().isPresent())
        {
            fail("Expected a compile error but found:\n" + sourceCodeText);
        }
    }

    private void assertDomainModelExists(CompilationResult compilationResult)
    {
        Optional<DomainModelWithSourceCode> domainModelWithSourceCode = compilationResult.domainModelWithSourceCode();
        assertThat(domainModelWithSourceCode).isPresent();
    }

    protected void assertCompilerErrors()
    {
        String sourceCodeText = this.getSourceCodeText();
        CompilationResult compilationResult = this.compile(sourceCodeText);

        this.assertCompilerAnnotationsExist(compilationResult, this.getTestName(), sourceCodeText);
        this.assertDomainModelDoesNotExist(compilationResult, sourceCodeText);
    }

    protected void assertNoCompilerErrors()
    {
        String sourceCodeText = this.getSourceCodeText();
        CompilationResult compilationResult = this.compile(sourceCodeText);

        this.assertCompilerAnnotationsDoNotExist(compilationResult, sourceCodeText);
        this.assertCompilerAnnotationFilesDoNotExist();
        this.assertDomainModelExists(compilationResult);
    }

    protected void assertOnlyCompilerWarnings()
    {
        String sourceCodeText = this.getSourceCodeText();
        CompilationResult compilationResult = this.compile(sourceCodeText);

        this.assertCompilerAnnotationsExist(compilationResult, this.getTestName(), sourceCodeText);
        this.assertDomainModelExists(compilationResult);
    }
}
