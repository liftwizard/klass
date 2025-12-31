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

package cool.klass.syntax.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.io.CharStreams;
import cool.klass.model.converter.compiler.token.categories.TokenCategory;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "generate-css", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class GenerateCssMojo extends AbstractMojo {

	private static final String CSS_TEMPLATE_PATH = "/klass-syntax-header.css";
	private static final Converter<String, String> CONVERTER = CaseFormat.UPPER_UNDERSCORE.converterTo(
		CaseFormat.LOWER_HYPHEN
	);

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject mavenProject;

	@Parameter(
		property = "outputDirectory",
		defaultValue = "${project.build.directory}/generated-resources/ui/static/css"
	)
	private File outputDirectory;

	@Override
	public void execute() throws MojoExecutionException {
		this.getLog().info("Generating CSS for token categories");

		if (!this.outputDirectory.exists()) {
			if (!this.outputDirectory.mkdirs()) {
				throw new MojoExecutionException(
					"Failed to create output directory: " + this.outputDirectory.getAbsolutePath()
				);
			}
		}

		File cssFile = new File(this.outputDirectory, "klass-syntax.css");
		try {
			this.generateCssFile(cssFile);
			this.addResourceDirectory();
		} catch (IOException e) {
			throw new MojoExecutionException("Error generating CSS file", e);
		}
	}

	private void generateCssFile(File cssFile) throws IOException, MojoExecutionException {
		String headerContent = this.readCssTemplate();

		String cssBuilder = Arrays.stream(TokenCategory.values())
			.map(this::generateCssClass)
			.collect(Collectors.joining("", headerContent, ""));

		try (Writer writer = Files.newBufferedWriter(cssFile.toPath(), StandardCharsets.UTF_8)) {
			writer.write(cssBuilder);
		}

		this.getLog().info("Generated CSS file: " + cssFile.getAbsolutePath());
	}

	private String readCssTemplate() throws IOException, MojoExecutionException {
		try (InputStream inputStream = this.getClass().getResourceAsStream(CSS_TEMPLATE_PATH)) {
			if (inputStream == null) {
				throw new MojoExecutionException("CSS template file not found on classpath: " + CSS_TEMPLATE_PATH);
			}
			try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
				return CharStreams.toString(reader);
			}
		}
	}

	private void addResourceDirectory() {
		Resource resource = new Resource();
		resource.setDirectory(this.outputDirectory.getAbsolutePath());
		this.mavenProject.addResource(resource);

		this.getLog().info("Added resource directory: " + this.outputDirectory.getAbsolutePath());
	}

	private String generateCssClass(TokenCategory tokenCategory) {
		return ".klass-%s {%n    color: %s;%n}%n".formatted(
			this.getTokenCategoryName(tokenCategory),
			this.getCssVar(tokenCategory)
		);
	}

	private String getCssVar(TokenCategory tokenCategory) {
		TokenCategory parentCategory = tokenCategory.getParentCategory();
		String fallbackCssVar = parentCategory == null ? "--color-foreground" : this.getCssVar(parentCategory);

		return String.format("var(--klass-color-%s, %s)", this.getTokenCategoryName(tokenCategory), fallbackCssVar);
	}

	private String getTokenCategoryName(TokenCategory tokenCategory) {
		return CONVERTER.convert(tokenCategory.name());
	}
}
