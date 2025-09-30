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

package cool.klass.generator.json.view.plugin;

import java.io.File;

import cool.klass.generator.json.view.JsonViewGenerator;
import cool.klass.generator.plugin.AbstractGenerateMojo;
import cool.klass.model.meta.domain.api.DomainModel;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
    name = "generate-json-views",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class GenerateJsonViewsMojo extends AbstractGenerateMojo {

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources/json-views")
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
                JsonViewGenerator jsonViewGenerator = new JsonViewGenerator(
                    domainModel,
                    this.rootPackageName,
                    this.applicationName
                );
                jsonViewGenerator.writeJsonViews(this.outputDirectory.toPath());
                return null;
            });

        if (wasGenerated) {
            this.getLog().info("Generated JSON views in: " + this.outputDirectory.getPath());
        }

        String outputDirectoryPath = this.outputDirectory.getPath();
        this.mavenProject.addCompileSourceRoot(outputDirectoryPath);
    }
}
