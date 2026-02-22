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

package cool.klass.generator.reladomo.lens.plugin;

import java.io.File;

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
		DomainModel domainModel = this.getDomainModel();

		ReladomoLensGenerator generator = new ReladomoLensGenerator(domainModel, this.applicationName);

		try {
			if (!this.outputDirectory.exists()) {
				this.outputDirectory.mkdirs();
			}

			generator.writeClassLenses(this.outputDirectory.toPath());

			// Add the generated sources to the compile path
			this.mavenProject.addCompileSourceRoot(this.outputDirectory.getAbsolutePath());
		} catch (RuntimeException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
