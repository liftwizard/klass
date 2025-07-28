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

package cool.klass.generator.react.prop.type.plugin;

import java.io.File;

import cool.klass.generator.plugin.AbstractGenerateMojo;
import cool.klass.generator.react.prop.type.ReactPropTypeGenerator;
import cool.klass.model.meta.domain.api.DomainModel;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
    name = "generate-react-prop-types",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class GenerateReactPropTypesMojo extends AbstractGenerateMojo {

    @Parameter(
        property = "outputDirectory",
        defaultValue = "${project.build.directory}/generated-resources/react-prop-types"
    )
    private File outputDirectory;

    @Override
    protected InputSource getInputSource() {
        return InputSource.CLASSPATH;
    }

    @Override
    public void execute() throws MojoExecutionException {
        boolean wasGenerated =
            this.executeWithCaching(this.outputDirectory, () -> {
                    DomainModel domainModel = this.getDomainModel();
                    ReactPropTypeGenerator propTypeGenerator = new ReactPropTypeGenerator(domainModel);
                    propTypeGenerator.writePropTypes(this.outputDirectory.toPath());
                    return null;
                });

        if (wasGenerated) {
            this.getLog().info("Generated React prop types in: " + this.outputDirectory.getPath());
        }

        Resource resource = new Resource();
        resource.setDirectory(this.outputDirectory.getAbsolutePath());
        // TODO: Should be based on the output path
        resource.setTargetPath("react-prop-types");
        this.mavenProject.addResource(resource);
    }
}
