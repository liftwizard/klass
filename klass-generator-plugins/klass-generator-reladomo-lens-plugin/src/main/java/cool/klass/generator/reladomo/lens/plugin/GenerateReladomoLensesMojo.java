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

package cool.klass.generator.reladomo.lens.plugin;

import java.io.File;
import java.nio.file.Path;

import cool.klass.generator.plugin.AbstractGenerateMojo;
import cool.klass.generator.reladomo.lens.ReladomoLensGenerator;
import cool.klass.model.meta.domain.api.DomainModel;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Maven plugin goal that generates Reladomo Lens implementations.
 *
 * <p>This plugin generates type-safe lens classes for each Klass in the domain model,
 * providing a unified interface for property access without reflection.
 */
@Mojo(
	name = "generate-reladomo-lenses",
	defaultPhase = LifecyclePhase.GENERATE_SOURCES,
	threadSafe = true,
	requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class GenerateReladomoLensesMojo extends AbstractGenerateMojo {

	@Parameter(
		property = "outputDirectory",
		defaultValue = "${project.build.directory}/generated-sources/reladomo-lens"
	)
	private File outputDirectory;

	@Parameter(property = "applicationName", required = true)
	private String applicationName;

	@Override
	protected InputSource getInputSource() {
		return InputSource.CLASSPATH;
	}

	@Override
	public void execute() throws MojoExecutionException {
		boolean wasGenerated = this.executeWithCaching(this.outputDirectory, () -> {
				DomainModel domainModel = this.getDomainModel();
				Path outputPath = this.outputDirectory.toPath();
				ReladomoLensGenerator generator = new ReladomoLensGenerator(domainModel, this.applicationName);
				generator.writeClassLenses(outputPath);
				return null;
			});

		if (wasGenerated) {
			this.getLog().info("Generated Reladomo lenses in: " + this.outputDirectory.getPath());
		}

		this.mavenProject.addCompileSourceRoot(this.outputDirectory.getPath());
	}
}
