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
import cool.klass.generator.reladomo.classlist.ReladomoClassListGenerator;
import cool.klass.model.meta.domain.api.DomainModel;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
    name = "generate-reladomo-class-list",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class GenerateReladomoClassListMojo extends AbstractGenerateMojo {

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated-resources/reladomo")
    private File outputDirectory;

    @Parameter(property = "outputFilename", required = true, defaultValue = "ReladomoClassList.xml")
    private String outputFilename;

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
        Path path = outputPath.resolve(this.outputFilename);
        try {
            ReladomoClassListGenerator reladomoClassListGenerator = new ReladomoClassListGenerator(domainModel);
            reladomoClassListGenerator.writeClassListFile(path);
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
