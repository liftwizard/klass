package cool.klass.generator.graphql.runtime.wiring;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;

public class GraphQLRuntimeWiringBuilderGenerator
{
    @Nonnull
    private final DomainModel domainModel;
    @Nonnull
    private final String      rootPackageName;
    @Nonnull
    private final String      applicationName;
    @Nonnull
    private final String      packageName;
    @Nonnull
    private final String      relativePath;

    public GraphQLRuntimeWiringBuilderGenerator(
            @Nonnull DomainModel domainModel,
            @Nonnull String rootPackageName,
            @Nonnull String applicationName)
    {
        this.domainModel     = Objects.requireNonNull(domainModel);
        this.rootPackageName = Objects.requireNonNull(rootPackageName);
        this.applicationName = Objects.requireNonNull(applicationName);
        this.packageName     = rootPackageName + ".graphql.runtime.wiring";
        this.relativePath    = this.packageName.replaceAll("\\.", "/");
    }

    public void writeRuntimeWiringBuilderFile(@Nonnull Path outputPath) throws FileNotFoundException
    {
        Path path = outputPath.resolve(this.relativePath);
        path.toFile().mkdirs();
        Path javaPath = path.resolve(this.applicationName + "RuntimeWiringBuilder.java");

        //language=JAVA
        String sourceCode = ""
                + "package " + this.packageName + ";\n"
                + "\n"
                + "import java.util.function.Supplier;\n"
                + "\n"
                + "import io.liftwizard.graphql.scalar.temporal.GraphQLTemporalScalar;\n"
                + "import " + this.rootPackageName + ".graphql.runtime.wiring.query." + this.applicationName + "QueryTypeRuntimeWiringProvider;\n"
                + this.domainModel.getClasses().reject(Klass::isAbstract).collect(this::getImport).makeString("")
                + "import graphql.Scalars;\n"
                + "import graphql.schema.idl.RuntimeWiring;\n"
                + "\n"
                + "/**\n"
                + " * Auto-generated by {@link " + this.getClass().getCanonicalName() + "}\n"
                + " */\n"
                + "public class " + this.applicationName + "RuntimeWiringBuilder\n"
                + "        implements Supplier<RuntimeWiring>\n"
                + "{\n"
                + "    @Override\n"
                + "    public RuntimeWiring get()\n"
                + "    {\n"
                + "        return RuntimeWiring.newRuntimeWiring()\n"
                + "                .scalar(new GraphQLTemporalScalar(\"Instant\"))\n"
                + "                .scalar(new GraphQLTemporalScalar(\"TemporalInstant\"))\n"
                + "                .scalar(new GraphQLTemporalScalar(\"TemporalRange\"))\n"
                + "                .scalar(Scalars.GraphQLLong)\n"
                + "                .type(new " + this.applicationName + "QueryTypeRuntimeWiringProvider().get())\n"
                + this.domainModel.getClasses().reject(Klass::isAbstract).collect(this::getSourceCode).makeString("")
                + "                .build();\n"
                + "    }\n"
                + "}\n";

        this.printStringToFile(javaPath, sourceCode);
    }

    private String getImport(Classifier classifier)
    {
        return String.format(
                "import %s.graphql.type.runtime.wiring.%sTypeRuntimeWiringProvider;\n",
                classifier.getPackageName(),
                classifier.getName());
    }

    private String getSourceCode(Classifier classifier)
    {
        return String.format(
                "                .type(new %sTypeRuntimeWiringProvider().get())\n",
                classifier.getName());
    }

    private void printStringToFile(@Nonnull Path path, String contents) throws FileNotFoundException
    {
        try (PrintStream printStream = new PrintStream(new FileOutputStream(path.toFile())))
        {
            printStream.print(contents);
        }
    }
}
