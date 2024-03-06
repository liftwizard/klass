package cool.klass.generator.grahql.fragment;

import java.nio.file.Path;

import javax.annotation.Nonnull;

import com.google.common.base.CaseFormat;
import cool.klass.generator.perpackage.AbstractPerPackageGenerator;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;

public class GraphQLFragmentGenerator
        extends AbstractPerPackageGenerator
{
    public GraphQLFragmentGenerator(@Nonnull DomainModel domainModel)
    {
        super(domainModel);
    }

    @Nonnull
    @Override
    protected Path getPluginRelativePath(Path path)
    {
        return path
                .resolve("graphql")
                .resolve("fragment");
    }

    @Nonnull
    @Override
    protected String getFileName()
    {
        return "GraphQLFragment.graphqls";
    }

    @Nonnull
    @Override
    protected String getPackageSourceCode(@Nonnull String fullyQualifiedPackage)
    {
        String topLevelElementsCode = this.domainModel
                .getClasses()
                .collect(this::getSourceCode)
                .makeString("");

        String sourceCode = ""
                + "# Auto-generated by " + this.getClass().getCanonicalName() + "\n"
                + "\n"
                + topLevelElementsCode;

        return sourceCode;
    }

    private String getSourceCode(@Nonnull Klass klass)
    {
        String lowerCaseName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, klass.getName());

        String propertiesSourceCode = klass
                .getDataTypeProperties()
                .reject(DataTypeProperty::isDerived)
                .reject(DataTypeProperty::isPrivate)
                .collect(this::getSourceCode)
                .makeString("");
        return "fragment " + lowerCaseName + "Fragment on " + klass.getName() + " {\n"
                + propertiesSourceCode
                + "}\n\n";
    }

    private String getSourceCode(DataTypeProperty dataTypeProperty)
    {
        return String.format("    %s\n", dataTypeProperty.getName());
    }
}
