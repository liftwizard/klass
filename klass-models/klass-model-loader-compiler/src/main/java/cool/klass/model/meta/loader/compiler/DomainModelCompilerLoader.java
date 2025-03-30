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

package cool.klass.model.meta.loader.compiler;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationResult;
import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.KlassCompiler;
import cool.klass.model.converter.compiler.annotation.RootCompilerAnnotation;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import cool.klass.model.meta.loader.DomainModelLoader;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainModelCompilerLoader implements DomainModelLoader {

    public static final Pattern KLASS_FILE_EXTENSION = Pattern.compile(".*\\.klass");

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainModelCompilerLoader.class);

    @Nonnull
    private final ImmutableList<String> klassSourcePackages;

    @Nonnull
    private final ClassLoader classLoader;

    @Nonnull
    private final Consumer<RootCompilerAnnotation> compilerAnnotationHandler;

    @Nonnull
    private final AnsiColorScheme colorScheme;

    public DomainModelCompilerLoader(
        @Nonnull ImmutableList<String> klassSourcePackages,
        @Nonnull ClassLoader classLoader,
        @Nonnull Consumer<RootCompilerAnnotation> compilerAnnotationHandler,
        AnsiColorScheme colorScheme
    ) {
        this.klassSourcePackages = Objects.requireNonNull(klassSourcePackages);
        this.classLoader = Objects.requireNonNull(classLoader);
        this.compilerAnnotationHandler = Objects.requireNonNull(compilerAnnotationHandler);
        this.colorScheme = Objects.requireNonNull(colorScheme);
    }

    public static void logCompilerError(RootCompilerAnnotation compilerAnnotation) {
        if (compilerAnnotation.isError()) {
            LOGGER.error("{}", compilerAnnotation);
        }
    }

    public static void logCompilerAnnotation(RootCompilerAnnotation compilerAnnotation) {
        if (compilerAnnotation.isError()) {
            LOGGER.error("{}", compilerAnnotation);
        } else {
            LOGGER.warn("{}", compilerAnnotation);
        }
    }

    @Override
    @Nonnull
    public DomainModelWithSourceCode load() {
        LOGGER.info("Scanning source packages: {}", this.klassSourcePackages);
        Instant start = Instant.now();

        ImmutableList<CompilationUnit> compilationUnits = this.getCompilationUnits();

        KlassCompiler klassCompiler = new KlassCompiler(compilationUnits, this.colorScheme);
        CompilationResult compilationResult = klassCompiler.compile();
        DomainModelWithSourceCode domainModel = this.handleResult(compilationResult);

        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        String durationPrettyString = duration
            .toString()
            .substring(2)
            .replaceAll("(\\d[HMS])(?!$)", "$1 ")
            .toLowerCase();
        LOGGER.info("Domain model compilation completed in {}", durationPrettyString);

        return domainModel;
    }

    @Nonnull
    private ImmutableList<CompilationUnit> getCompilationUnits() {
        ImmutableList<URL> urls =
            this.klassSourcePackages.flatCollectWith(ClasspathHelper::forPackage, this.classLoader);
        FilterBuilder filterBuilder = new FilterBuilder();
        this.klassSourcePackages.forEach(filterBuilder::includePackage);
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
            .setScanners(new ResourcesScanner())
            .setUrls(urls.castToList())
            .filterInputsBy(filterBuilder);
        Reflections reflections = new Reflections(configurationBuilder);
        ImmutableList<String> klassLocations = Lists.immutable.withAll(reflections.getResources(KLASS_FILE_EXTENSION));

        LOGGER.debug("Found source files on classpath: {}", klassLocations);

        ImmutableList<CompilationUnit> compilationUnits = klassLocations.collectWithIndex(
            (each, index) -> CompilationUnit.createFromClasspathLocation(index, each)
        );

        if (compilationUnits.isEmpty()) {
            String message = "Could not find any files matching *.klass in urls: " + urls;
            throw new RuntimeException(message);
        }
        return compilationUnits;
    }

    @Nonnull
    private DomainModelWithSourceCode handleResult(@Nonnull CompilationResult compilationResult) {
        ImmutableList<RootCompilerAnnotation> compilerAnnotations = compilationResult.compilerAnnotations();
        for (RootCompilerAnnotation compilerAnnotation : compilerAnnotations) {
            this.compilerAnnotationHandler.accept(compilerAnnotation);
        }

        if (compilationResult.domainModelWithSourceCode().isEmpty()) {
            throw new RuntimeException("There were compiler errors.");
        }

        return compilationResult.domainModelWithSourceCode().get();
    }
}
