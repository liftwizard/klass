package cool.klass.generator.json.view.plugin;

import java.io.File;
import java.io.IOException;

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
        requiresDependencyResolution = ResolutionScope.RUNTIME)
public class GenerateJsonViewsMojo extends AbstractGenerateMojo
{
    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources/json-views")
    private File outputDirectory;

    @Parameter(property = "applicationName", required = true)
    private String applicationName;

    @Parameter(property = "rootPackageName", required = true)
    private String rootPackageName;

    @Override
    public void execute() throws MojoExecutionException
    {
        DomainModel domainModel = this.getDomainModel();
        try
        {
            JsonViewGenerator jsonViewGenerator = new JsonViewGenerator(
                    domainModel,
                    this.rootPackageName,
                    this.applicationName);
            jsonViewGenerator.writeJsonViews(this.outputDirectory.toPath());
        }
        catch (IOException e)
        {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        String outputDirectoryPath = this.outputDirectory.getPath();
        this.getLog().info("Adding compile source root: " + outputDirectoryPath);
        this.mavenProject.addCompileSourceRoot(outputDirectoryPath);
    }
}
