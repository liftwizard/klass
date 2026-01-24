/*
 * Copyright 2026 Craig Motlin
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
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
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
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.fusesource.jansi.AnsiConsole;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.sonatype.plexus.build.incremental.BuildContext;

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

	@Parameter(property = "cachingEnabled", defaultValue = "true")
	protected boolean cachingEnabled;

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	protected MavenProject mavenProject;

	@Component
	protected BuildContext buildContext;

	private ImmutableList<File> cachedInputFiles;
	private ImmutableMap<String, String> cachedFileContents = Maps.immutable.empty();

	public enum InputSource {
		FILESYSTEM,
		CLASSPATH,
	}

	protected abstract InputSource getInputSource();

	@Nonnull
	protected DomainModelWithSourceCode getDomainModelFromFiles() throws MojoExecutionException {
		CompilationResult compilationResult = this.getCompilationResultFromFiles();

		this.handleErrorsCompilationResult(compilationResult);

		return compilationResult.domainModelWithSourceCode().get();
	}

	@Nonnull
	private CompilationResult getCompilationResultFromFiles() throws MojoExecutionException {
		ImmutableList<File> klassLocations = this.loadInputFiles();

		if (klassLocations.isEmpty()) {
			String message = "Could not find any files matching %s in: %s".formatted(
				KLASS_FILE_EXTENSION,
				this.mavenProject.getResources()
			);
			throw new MojoExecutionException(message);
		}

		ImmutableList<CompilationUnit> compilationUnits = this.getCompilationUnits(klassLocations);

		AnsiColorScheme ansiColorScheme = ColorSchemeProvider.getByName(this.colorScheme);
		if (this.colorScheme != null) {
			this.getLog().info("Using configured color scheme: " + this.colorScheme);
		} else {
			this.getLog().info("No color scheme configured, using default");
		}
		// TODO: We should use an abstract DomainModelFactory here, not necessarily the compiler.
		KlassCompiler klassCompiler = new KlassCompiler(compilationUnits, ansiColorScheme, false);
		return klassCompiler.compile();
	}

	@Nonnull
	protected DomainModelWithSourceCode getDomainModel() throws MojoExecutionException {
		if (this.klassSourcePackages.isEmpty()) {
			String message =
				""
				+ "Klass maven plugins must be configured with at least one klassSourcePackage. For example:\n"
				+ "<klassSourcePackages>\n"
				+ "    <klassSourcePackage>klass.model.meta.domain</klassSourcePackage>\n"
				+ "    <klassSourcePackage>${app.rootPackageName}</klassSourcePackage>\n"
				+ "</klassSourcePackages>";
			throw new MojoExecutionException(message);
		}

		AnsiColorScheme ansiColorScheme = ColorSchemeProvider.getByName(this.colorScheme);
		// TODO: We should use an abstract DomainModelFactory here, not necessarily the compiler.
		var loader = new DomainModelCompilerLoader(
			Lists.immutable.withAll(this.klassSourcePackages),
			this.getClassLoader(),
			this::logCompilerAnnotation,
			ansiColorScheme,
			false
		);
		return loader.load();
	}

	private ImmutableList<File> loadInputFiles() {
		if (this.cachedInputFiles != null) {
			return this.cachedInputFiles;
		}

		MutableList<String> adaptedKlassSourcePackages = ListAdapter.adapt(this.klassSourcePackages);

		MutableList<File> klassLocations = Lists.mutable.empty();
		for (Resource resource : this.mavenProject.getResources()) {
			this.loadfiles(klassLocations, adaptedKlassSourcePackages, resource);
		}

		this.cachedInputFiles = klassLocations.toImmutable();
		return this.cachedInputFiles;
	}

	private void loadfiles(
		MutableList<File> resultKlassLocations,
		ListIterable<String> klassSourcePackages,
		Resource resource
	) {
		String directory = resource.getDirectory();
		String message = "Scanning source packages: %s in directory: %s".formatted(
			klassSourcePackages.makeString(),
			directory
		);
		this.getLog().info(message);

		klassSourcePackages
			.asLazy()
			.collect((klassSourcePackage) -> klassSourcePackage.replaceAll("\\.", "/"))
			.collect((relativeDirectory) -> new File(directory, relativeDirectory))
			.forEach((file) -> {
				File[] files = file.listFiles();
				if (files == null) {
					this.getLog().warn("Could not find directory: " + file.getAbsolutePath());
				}
			});

		// list all files in sourceDirectory
		klassSourcePackages
			.asLazy()
			.collect((klassSourcePackage) -> klassSourcePackage.replaceAll("\\.", "/"))
			.collect((relativeDirectory) -> new File(directory, relativeDirectory))
			.collect(File::listFiles)
			.reject(Objects::isNull)
			.collect(ArrayAdapter::adapt)
			.forEach((files) ->
				files
					.asLazy()
					.select((file) -> KLASS_FILE_EXTENSION.matcher(file.getAbsolutePath()).matches())
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

		// Build cache if needed
		if (this.cachedFileContents.isEmpty() && klassLocations.notEmpty()) {
			this.cachedFileContents = this.buildFileContentCache(klassLocations);
		}

		ImmutableList<CompilationUnit> compilationUnits = klassLocations.collectWithIndex((file, index) -> {
			String path = file.getAbsolutePath();
			String content = this.cachedFileContents.get(path);
			return CompilationUnit.createFromText(index, Optional.empty(), path, content);
		});

		return compilationUnits;
	}

	private ImmutableMap<String, String> buildFileContentCache(ImmutableList<File> files) {
		return files.toImmutableMap(File::getAbsolutePath, (file) -> {
			try {
				return Files.readString(file.toPath());
			} catch (IOException e) {
				throw new RuntimeException("Failed to read file: " + file.getAbsolutePath(), e);
			}
		});
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

	protected boolean shouldSkipGeneration(File outputDirectory) throws MojoExecutionException {
		if (!outputDirectory.exists()) {
			this.getLog().info("Output directory does not exist, generating");
			return false;
		}

		File[] outputFiles = outputDirectory.listFiles();
		if (ArrayIterate.isEmpty(outputFiles)) {
			this.getLog().info("No output files exist, generating");
			return false;
		}

		ArrayAdapter<File> outputFilesAdapter = ArrayAdapter.adapt(outputFiles);
		long oldestOutputTime = outputFilesAdapter.collect(File::lastModified).min();

		return this.getInputSource() == InputSource.FILESYSTEM
			? this.shouldSkipFilesystemGeneration(oldestOutputTime)
			: this.shouldSkipClasspathGeneration(outputDirectory);
	}

	private boolean shouldSkipFilesystemGeneration(long oldestOutputTime) {
		ImmutableList<File> inputFiles = this.loadInputFiles();
		for (File inputFile : inputFiles) {
			if (this.buildContext.hasDelta(inputFile)) {
				this.getLog().debug("Input file changed: " + inputFile.getPath());
				this.getLog().info("Input files changed, regenerating outputs");
				return false;
			}
		}

		long newestInputTime = inputFiles.collect(File::lastModified).max();

		if (newestInputTime > oldestOutputTime) {
			this.getLog().info("Input files newer than outputs, regenerating");
			return false;
		}

		this.getLog().info("No input changes detected, skipping generation");
		return true;
	}

	private boolean shouldSkipClasspathGeneration(File outputDirectory) {
		if (!this.cachingEnabled) {
			this.getLog().info("Caching disabled, regenerating");
			return false;
		}

		try {
			String currentInputHash = this.calculateClasspathHash();
			String previousInputHash = this.loadPreviousInputHash(outputDirectory);

			if (!currentInputHash.equals(previousInputHash)) {
				this.getLog().info("Classpath inputs changed, regenerating outputs");
				return false;
			}

			this.getLog().info("No input changes detected, skipping generation");
			return true;
		} catch (Exception e) {
			this.getLog().warn("Failed to calculate classpath hash, regenerating: " + e.getMessage());
			return false;
		}
	}

	private String calculateClasspathHash() throws MojoExecutionException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			ClassLoader classLoader = this.getClassLoader();

			ImmutableList<URL> urls = Lists.immutable
				.withAll(this.klassSourcePackages)
				.flatCollectWith(ClasspathHelper::forPackage, classLoader);

			FilterBuilder filterBuilder = new FilterBuilder();
			this.klassSourcePackages.forEach(filterBuilder::includePackage);

			ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
				.setScanners(new ResourcesScanner())
				.setUrls(urls.castToList())
				.filterInputsBy(filterBuilder);

			Reflections reflections = new Reflections(configurationBuilder);

			MutableList<String> klassFiles = Lists.mutable.withAll(reflections.getResources(KLASS_FILE_EXTENSION));
			klassFiles.sortThis();

			// Hash the content of each file
			for (String klassFile : klassFiles) {
				try (InputStream is = classLoader.getResourceAsStream(klassFile)) {
					if (is != null) {
						digest.update(klassFile.getBytes(StandardCharsets.UTF_8));
						digest.update(is.readAllBytes());
					}
				}
			}

			byte[] hashBytes = digest.digest();
			StringBuilder hexString = new StringBuilder();
			for (byte b : hashBytes) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (Exception e) {
			throw new MojoExecutionException("Failed to calculate classpath hash", e);
		}
	}

	private String loadPreviousInputHash(File outputDirectory) {
		if (!this.cachingEnabled) {
			return "";
		}

		File hashFile = new File(outputDirectory, ".input-hash");
		if (!hashFile.exists()) {
			return "";
		}

		try {
			return Files.readString(hashFile.toPath()).trim();
		} catch (IOException e) {
			this.getLog().warn("Failed to read previous input hash: " + e.getMessage());
			return "";
		}
	}

	private void saveCurrentInputHash(File outputDirectory, String hash) {
		if (!this.cachingEnabled) {
			return;
		}

		File hashFile = new File(outputDirectory, ".input-hash");
		try {
			Files.writeString(hashFile.toPath(), hash);
		} catch (IOException e) {
			this.getLog().warn("Failed to save input hash: " + e.getMessage());
		}
	}

	protected boolean executeWithCaching(File outputDirectory, Callable<Void> task) throws MojoExecutionException {
		this.getLog().info("Checking if generation should be skipped for: " + outputDirectory.getPath());
		if (this.shouldSkipGeneration(outputDirectory)) {
			this.getLog().info("Skipping generation based on caching logic");
			return false;
		}

		this.getLog().info("Proceeding with generation");
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}

		try {
			task.call();

			InputSource inputSource = this.getInputSource();
			if (inputSource == InputSource.CLASSPATH) {
				try {
					String currentInputHash = this.calculateClasspathHash();
					this.saveCurrentInputHash(outputDirectory, currentInputHash);
				} catch (Exception e) {
					this.getLog().warn("Failed to save classpath hash: " + e.getMessage());
				}
			}
		} catch (MojoExecutionException e) {
			throw e;
		} catch (Exception e) {
			throw new MojoExecutionException("Generation failed", e);
		}
		return true;
	}
}
