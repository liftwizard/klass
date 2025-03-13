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

package cool.klass.model.converter.compiler.annotation;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.google.common.collect.Comparators;
import cool.klass.model.converter.compiler.CompilationResult;
import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.KlassCompiler;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
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
        this.assertCompilationSucceeds(false);
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

    private ImmutableList<String> assertCompilerAnnotationsExist(
            CompilationResult compilationResult,
            String testName)
    {
        ImmutableList<RootCompilerAnnotation> compilerAnnotations = compilationResult.compilerAnnotations();
        MutableList<String> expectedLogFileNames = Lists.mutable.empty();

        for (RootCompilerAnnotation compilerAnnotation : compilerAnnotations)
        {
            String annotationSourceName = "%s-%s-%d-%s.log".formatted(
                    testName,
                    compilerAnnotation.getLines().toReversed().makeString("_"),
                    compilerAnnotation.getCharPositionInLine(),
                    compilerAnnotation.getAnnotationCode());

            expectedLogFileNames.add(annotationSourceName);

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

        return expectedLogFileNames.toImmutable();
    }

    private ImmutableList<Object> getAnnotationKey(RootCompilerAnnotation rootCompilerAnnotation)
    {
        String filenameWithoutDirectory = rootCompilerAnnotation.getFilenameWithoutDirectory();
        int line = rootCompilerAnnotation.getLine();
        int charPositionInLine = rootCompilerAnnotation.getCharPositionInLine();
        String annotationCode = rootCompilerAnnotation.getAnnotationCode();
        return Lists.immutable.with(
                filenameWithoutDirectory,
                line,
                charPositionInLine,
                annotationCode);
    }

    private ImmutableList<String> findCompilerAnnotationFiles()
    {
        String packagePath  = this.getClass().getPackage().getName().replace('.', '/');
        File   resourcesDir = new File("src/test/resources/" + packagePath);

        if (!resourcesDir.exists() || !resourcesDir.isDirectory())
        {
            return Lists.immutable.empty();
        }

        String annotationFilePattern = "-\\d+(_\\d+)*-\\d+-([A-Z]{3})(_[A-Z]{3}){2}\\.log";
        FilenameFilter annotationFileFilter = (dir, name) -> name.matches(this.getTestName() + annotationFilePattern);
        File[] annotationFiles = resourcesDir.listFiles(annotationFileFilter);

        return Lists.immutable.with(annotationFiles).collect(File::getName);
    }

    private void assertNoExtraAnnotationFilesExist(ImmutableList<String> expectedFileNames)
    {
        ImmutableList<String> actualFileNames = this.findCompilerAnnotationFiles();

        assertThat(actualFileNames.toSortedListBy(CompilerAnnotationKey::parseAnnotationFilename))
                .as(
                        "Extra or missing annotation log files found in src/test/resources/%s",
                        this.getClass().getPackage().getName().replace('.', '/'))
                .isEqualTo(expectedFileNames.toSortedListBy(CompilerAnnotationKey::parseAnnotationFilename));
    }

    protected void assertCompilationSucceeds(boolean expectDomainModel)
    {
        String sourceCodeText = FileSlurper.slurp(this.getSourceName(), this.getClass());
        String sourceName = this.getSourceName();
        CompilationUnit compilationUnit = CompilationUnit.createFromText(
                0,
                Optional.empty(),
                sourceName,
                sourceCodeText);
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("dark");
        var compiler = new KlassCompiler(compilationUnit, colorScheme);
        CompilationResult compilationResult = compiler.compile();

        ImmutableList<String> expectedLogFileNames = this.assertCompilerAnnotationsExist(
                compilationResult,
                this.getTestName());
        this.assertNoExtraAnnotationFilesExist(expectedLogFileNames);

        if (expectDomainModel)
        {
            Optional<DomainModelWithSourceCode> domainModelWithSourceCode = compilationResult.domainModelWithSourceCode();
            assertThat(domainModelWithSourceCode).isPresent();
        }
        else if (compilationResult.domainModelWithSourceCode().isPresent())
        {
            fail("Expected a compile error but found:\n" + sourceCodeText);
        }
    }

    private record CompilerAnnotationKey(
            ImmutableList<Integer> lineNumbers,
            int columnNumber,
            String errorCode,
            String filename)
            implements Comparable<CompilerAnnotationKey>
    {
        private static final Comparator<Iterable<Integer>> LEXICOGRAPHICAL = Comparators.lexicographical(Comparator.<Integer>naturalOrder());
        private static final Comparator<CompilerAnnotationKey> COMPARATOR = Comparator
                .comparing(CompilerAnnotationKey::lineNumbers, LEXICOGRAPHICAL)
                .thenComparingInt(CompilerAnnotationKey::columnNumber)
                .thenComparing(CompilerAnnotationKey::errorCode);

        public static CompilerAnnotationKey parseAnnotationFilename(String annotationFilename)
        {
            // TestName-LineNumbers-ColumnNumber-ErrorCode.log
            // Where LineNumbers can be a single number or multiple numbers joined by underscores
            String regex = ".*?-(\\d+(?:_\\d+)*?)-(\\d+)-([A-Z]{3}_[A-Z]{3}_[A-Z]{3})\\.log";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(annotationFilename);

            if (!matcher.matches())
            {
                throw new AssertionError("Filename does not match expected pattern: " + annotationFilename);
            }

            String lineNumbersStr = matcher.group(1);
            int columnNumber = Integer.parseInt(matcher.group(2));
            String errorCode = matcher.group(3);

            String[] lineNumbersArray = lineNumbersStr.split("_");
            MutableList<Integer> lineNumbers = Lists.mutable.empty();
            for (String lineNumber : lineNumbersArray)
            {
                lineNumbers.add(Integer.parseInt(lineNumber));
            }

            return new CompilerAnnotationKey(lineNumbers.toImmutable(), columnNumber, errorCode, annotationFilename);
        }

        @Override
        public int compareTo(CompilerAnnotationKey other)
        {
            return COMPARATOR.compare(this, other);
        }
    }
}
