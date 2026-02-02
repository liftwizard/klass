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

package cool.klass.generator.uml.nomnoml.plugin;

import java.io.File;
import java.nio.file.Path;

import cool.klass.generator.plugin.AbstractGenerateMojo;
import cool.klass.generator.uml.nomnoml.UmlNomnomlGenerator;
import cool.klass.model.meta.domain.api.DomainModel;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
	name = "generate-uml-diagram-nomnoml",
	defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
	threadSafe = true,
	requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class GenerateUmlNomnomlMojo extends AbstractGenerateMojo {

	@Parameter(
		property = "outputDirectory",
		defaultValue = "${project.build.directory}/generated-resources/uml-diagram-nomnoml"
	)
	private File outputDirectory;

	@Parameter(property = "applicationName", required = true)
	private String applicationName;

	@Parameter(property = "rootPackageName", required = true)
	private String rootPackageName;

	@Override
	protected InputSource getInputSource() {
		return InputSource.CLASSPATH;
	}

	@Override
	public void execute() throws MojoExecutionException {
		boolean wasGenerated = this.executeWithCaching(this.outputDirectory, () -> {
				DomainModel domainModel = this.getDomainModel();
				Path outputPath = this.outputDirectory.toPath();

				UmlNomnomlGenerator generator = new UmlNomnomlGenerator(
					domainModel,
					this.rootPackageName,
					this.applicationName
				);
				generator.writeUmlDiagram(outputPath);
				return null;
			});

		if (wasGenerated) {
			this.getLog().info("Generated UML diagram in: " + this.outputDirectory.getPath());
		}

		Resource resource = new Resource();
		resource.setDirectory(this.outputDirectory.getAbsolutePath());
		// TODO: Should be based on the output path
		this.mavenProject.addResource(resource);
	}
}
