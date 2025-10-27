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

package cool.klass.generator.dropwizard.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import cool.klass.generator.dropwizard.ApplicationSuperClassGenerator;
import cool.klass.generator.plugin.AbstractGenerateMojo;
import cool.klass.model.meta.domain.api.DomainModel;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
    name = "generate-abstract-application",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class GenerateAbstractApplicationMojo extends AbstractGenerateMojo {

    @Parameter(
        property = "outputDirectory",
        defaultValue = "${project.build.directory}/generated-sources/abstract-application"
    )
    private File outputDirectory;

    @Parameter(property = "rootPackageName", required = true)
    private String rootPackageName;

    @Parameter(property = "applicationName", required = true)
    private String applicationName;

    @Override
    protected InputSource getInputSource() {
        return InputSource.CLASSPATH;
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (!this.outputDirectory.exists()) {
            this.outputDirectory.mkdirs();
        }

        DomainModel domainModel = this.getDomainModel();
        Path outputPath = this.outputDirectory.toPath();
        try {
            ApplicationSuperClassGenerator abstractApplicationGenerator = new ApplicationSuperClassGenerator(
                domainModel,
                this.rootPackageName,
                this.applicationName,
                this.klassSourcePackages
            );
            abstractApplicationGenerator.writeAbstractApplicationFile(outputPath);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        this.mavenProject.addCompileSourceRoot(this.outputDirectory.getPath());
    }
}
