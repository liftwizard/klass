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

package cool.klass.generator.reladomo.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import cool.klass.generator.plugin.AbstractGenerateMojo;
import cool.klass.generator.reladomo.interfacefile.ReladomoInterfaceFileGenerator;
import cool.klass.generator.reladomo.objectfile.ReladomoObjectFileGenerator;
import cool.klass.model.meta.domain.api.DomainModel;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

// TODO: GENERATE_RESOURCES default phase?
@Mojo(
    name = "generate-reladomo-object-files",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class GenerateReladomoObjectFilesMojo extends AbstractGenerateMojo {

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated-resources/reladomo")
    private File outputDirectory;

    @Override
    protected InputSource getInputSource() {
        return InputSource.CLASSPATH;
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (!this.outputDirectory.exists()) {
            this.outputDirectory.mkdirs();
        }

        Path outputPath = this.outputDirectory.toPath();
        DomainModel domainModel = this.getDomainModel();

        ReladomoObjectFileGenerator reladomoObjectFileGenerator = new ReladomoObjectFileGenerator(domainModel);
        ReladomoInterfaceFileGenerator reladomoInterfaceFileGenerator = new ReladomoInterfaceFileGenerator(domainModel);
        try {
            reladomoObjectFileGenerator.writeObjectFiles(outputPath);
            reladomoInterfaceFileGenerator.writeObjectFiles(outputPath);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        Resource resource = new Resource();
        resource.setDirectory(this.outputDirectory.getAbsolutePath());
        // TODO: Should be based on the output path
        resource.setTargetPath("reladomo");
        this.mavenProject.addResource(resource);
    }
}
