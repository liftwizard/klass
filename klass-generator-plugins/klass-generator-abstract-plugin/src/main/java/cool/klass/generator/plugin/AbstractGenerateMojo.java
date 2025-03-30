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

package cool.klass.generator.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationResult;
import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.KlassCompiler;
import cool.klass.model.converter.compiler.annotation.RootCompilerAnnotation;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import cool.klass.model.meta.loader.compiler.DomainModelCompilerLoader;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.fusesource.jansi.AnsiConsole;

public abstract class AbstractGenerateMojo extends AbstractMojo {

    public static final Pattern KLASS_FILE_EXTENSION = Pattern.compile(".*\\.klass");

    @Parameter(property = "klassSourcePackages", required = true)
    protected List<String> klassSourcePackages;

    @Parameter(property = "logCompilerAnnotations")
    protected boolean logCompilerAnnotations;

    @Parameter(property = "klass.logGitHubAnnotations", defaultValue = "false")
    protected boolean logGitHubAnnotations;

    @Parameter(property = "colorScheme")
    protected String colorScheme;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject mavenProject;

    @Nonnull
    protected DomainModelWithSourceCode getDomainModelFromFiles() throws MojoExecutionException {
        CompilationResult compilationResult = this.getCompilationResultFromFiles();

        this.handleErrorsCompilationResult(compilationResult);

        return compilationResult.domainModelWithSourceCode().get();
    }

    @Nonnull
    private CompilationResult getCompilationResultFromFiles() throws MojoExecutionException {
        MutableList<File> klassLocations = this.loadFiles();

        if (klassLocations.isEmpty()) {
            String message =
                "Could not find any files matching %s in: %s".formatted(
                        KLASS_FILE_EXTENSION,
                        this.mavenProject.getResources()
                    );
            throw new MojoExecutionException(message);
        }

        ImmutableList<CompilationUnit> compilationUnits = this.getCompilationUnits(klassLocations.toImmutable());

        AnsiColorScheme ansiColorScheme = ColorSchemeProvider.getByName(this.colorScheme);
        if (this.colorScheme != null) {
            this.getLog().info("Using configured color scheme: " + this.colorScheme);
        } else {
            this.getLog().info("No color scheme configured, using default");
        }
        // TODO: We should use an abstract DomainModelFactory here, not necessarily the compiler.
        KlassCompiler klassCompiler = new KlassCompiler(compilationUnits, ansiColorScheme);
        return klassCompiler.compile();
    }

    @Nonnull
    protected DomainModelWithSourceCode getDomainModel() throws MojoExecutionException {
        if (this.klassSourcePackages.isEmpty()) {
            String message =
                "" +
                "Klass maven plugins must be configured with at least one klassSourcePackage. For example:\n" +
                "<klassSourcePackages>\n" +
                "    <klassSourcePackage>klass.model.meta.domain</klassSourcePackage>\n" +
                "    <klassSourcePackage>${app.rootPackageName}</klassSourcePackage>\n" +
                "</klassSourcePackages>";
            throw new MojoExecutionException(message);
        }

        AnsiColorScheme ansiColorScheme = ColorSchemeProvider.getByName(this.colorScheme);
        // TODO: We should use an abstract DomainModelFactory here, not necessarily the compiler.
        var loader = new DomainModelCompilerLoader(
            Lists.immutable.withAll(this.klassSourcePackages),
            this.getClassLoader(),
            this::logCompilerAnnotation,
            ansiColorScheme
        );
        return loader.load();
    }

    private MutableList<File> loadFiles() {
        MutableList<String> adaptedKlassSourcePackages = ListAdapter.adapt(this.klassSourcePackages);

        MutableList<File> klassLocations = Lists.mutable.empty();
        for (Resource resource : this.mavenProject.getResources()) {
            this.loadfiles(klassLocations, adaptedKlassSourcePackages, resource);
        }
        return klassLocations;
    }

    private void loadfiles(
        MutableList<File> resultKlassLocations,
        ListIterable<String> klassSourcePackages,
        Resource resource
    ) {
        String directory = resource.getDirectory();
        String message =
            "Scanning source packages: %s in directory: %s".formatted(klassSourcePackages.makeString(), directory);
        this.getLog().info(message);

        klassSourcePackages
            .asLazy()
            .collect(klassSourcePackage -> klassSourcePackage.replaceAll("\\.", "/"))
            .collect(relativeDirectory -> new File(directory, relativeDirectory))
            .forEach(file -> {
                File[] files = file.listFiles();
                if (files == null) {
                    this.getLog().warn("Could not find directory: " + file.getAbsolutePath());
                }
            });

        // list all files in sourceDirectory
        klassSourcePackages
            .asLazy()
            .collect(klassSourcePackage -> klassSourcePackage.replaceAll("\\.", "/"))
            .collect(relativeDirectory -> new File(directory, relativeDirectory))
            .collect(File::listFiles)
            .reject(Objects::isNull)
            .collect(ArrayAdapter::adapt)
            .forEach(
                files ->
                    files
                        .asLazy()
                        .select(file -> KLASS_FILE_EXTENSION.matcher(file.getAbsolutePath()).matches())
                        .into(resultKlassLocations)
            );
    }

    protected void handleErrorsCompilationResult(CompilationResult compilationResult) throws MojoExecutionException {
        for (RootCompilerAnnotation compilerAnnotation : compilationResult.compilerAnnotations()) {
            this.logCompilerAnnotation(compilerAnnotation);
        }

        if (compilationResult.domainModelWithSourceCode().isEmpty()) {
            throw new MojoExecutionException("There were compiler errors.");
        }
    }

    private void logCompilerAnnotation(RootCompilerAnnotation compilerAnnotation) {
        AnsiConsole.systemInstall();

        if (compilerAnnotation.isError()) {
            if (this.logGitHubAnnotations) {
                this.getLog().info("\n" + compilerAnnotation.toGitHubAnnotation());
            }
            this.getLog().error("\n" + compilerAnnotation);
        } else if (compilerAnnotation.isWarning() && this.logCompilerAnnotations) {
            if (this.logGitHubAnnotations) {
                this.getLog().info("\n" + compilerAnnotation.toGitHubAnnotation());
            }
            this.getLog().warn("\n" + compilerAnnotation);
        }
    }

    private ImmutableList<CompilationUnit> getCompilationUnits(ImmutableList<File> klassLocations) {
        this.getLog().debug("Found source files on classpath: " + klassLocations);

        ImmutableList<CompilationUnit> compilationUnits = klassLocations.collectWithIndex(
            (each, index) -> CompilationUnit.createFromFile(index, each)
        );

        return compilationUnits;
    }

    @Nonnull
    private ClassLoader getClassLoader() throws MojoExecutionException {
        try {
            List<String> classpathElements = this.mavenProject.getCompileClasspathElements();
            MutableList<URL> projectClasspathList = Lists.mutable.empty();
            for (String element : classpathElements) {
                URL url = AbstractGenerateMojo.getUrl(element);
                projectClasspathList.add(url);
            }

            URL[] urls = projectClasspathList.toArray(new URL[0]);
            return new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Dependency resolution failed", e);
        }
    }

    @Nonnull
    private static URL getUrl(@Nonnull String classpathElement) throws MojoExecutionException {
        try {
            return new File(classpathElement).toURI().toURL();
        } catch (MalformedURLException e) {
            throw new MojoExecutionException(classpathElement + " is an invalid classpath element", e);
        }
    }
}
