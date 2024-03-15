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

package cool.klass.generator.graphql.schema.plugin;

import java.io.File;

import cool.klass.generator.grahql.schema.GraphQLSchemaGenerator;
import cool.klass.generator.plugin.AbstractGenerateMojo;
import cool.klass.model.meta.domain.api.DomainModel;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
        name = "generate-graphql-schema",
        defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.RUNTIME)
public class GenerateGraphQLSchemaMojo
        extends AbstractGenerateMojo
{
    @Parameter(
            property = "outputDirectory",
            defaultValue = "${project.build.directory}/generated-resources/graphql-schema")
    private File outputDirectory;

    @Override
    public void execute()
            throws MojoExecutionException
    {
        DomainModel domainModel = this.getDomainModel();

        var generator = new GraphQLSchemaGenerator(domainModel);
        generator.writeFiles(this.outputDirectory.toPath());

        Resource resource = new Resource();
        resource.setDirectory(this.outputDirectory.getAbsolutePath());
        this.mavenProject.addResource(resource);
    }
}
