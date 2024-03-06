package cool.klass.generator.graphql.runtime.wiring.plugin;

import java.io.File;
import java.nio.file.Path;

import cool.klass.generator.graphql.runtime.wiring.GraphQLTypeRuntimeWiringProviderGenerator;
import cool.klass.generator.plugin.AbstractGenerateMojo;
import cool.klass.model.meta.domain.api.DomainModel;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(
        name = "generate-graphql-type-runtime-wiring-provider",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.RUNTIME)
public class GenerateGraphQLTypeRuntimeWiringProviderMojo
        extends AbstractGenerateMojo
{
    @Parameter(
            property = "outputDirectory",
            defaultValue = "${project.build.directory}/generated-sources/graphql-type-runtime-wiring-provider")
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException
    {
        if (!this.outputDirectory.exists())
        {
            this.outputDirectory.mkdirs();
        }

        DomainModel domainModel = this.getDomainModel();
        Path        outputPath  = this.outputDirectory.toPath();
        try
        {
            GraphQLTypeRuntimeWiringProviderGenerator runtimeWiringGenerator = new GraphQLTypeRuntimeWiringProviderGenerator(domainModel);
            runtimeWiringGenerator.writeTypeRuntimeWiringFiles(outputPath);
        }
        catch (RuntimeException e)
        {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        this.mavenProject.addCompileSourceRoot(this.outputDirectory.getPath());
    }
}