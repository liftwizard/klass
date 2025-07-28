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

package cool.klass.generator.klass.projection.plugin;

import java.io.File;

import cool.klass.generator.klass.projection.KlassProjectionGenerator;
import cool.klass.generator.plugin.AbstractGenerateMojo;
import cool.klass.model.meta.domain.api.DomainModel;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
    name = "generate-klass-projection",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class GenerateProjectionMojo extends AbstractGenerateMojo {

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources/projection")
    private File outputDirectory;

    @Override
    protected InputSource getInputSource() {
        return InputSource.CLASSPATH;
    }

    @Override
    public void execute() throws MojoExecutionException {
        DomainModel domainModel = this.getDomainModel();
        var generator = new KlassProjectionGenerator(domainModel);
        generator.writeFiles(this.outputDirectory.toPath());
    }
}
