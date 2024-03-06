package cool.klass.generator.relational.schema;

import java.text.MessageFormat;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;

public final class DdlGenerator
{
    public static final Converter<String, String> TABLE_NAME_CONVERTER =
            CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.UPPER_UNDERSCORE);

    public static final Converter<String, String> COLUMN_NAME_CONVERTER =
            CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE);

    private DdlGenerator()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static String getDdl(@Nonnull Klass klass)
    {
        Objects.requireNonNull(klass);

        String tableName            = getTableName(klass);
        String propertiesSourceCode = getPropertiesSourceCode(klass);

        String format = """
                drop table if exists {0};

                create table {0}
                (
                {1}
                );

                """;
        return MessageFormat.format(
                format,
                tableName,
                propertiesSourceCode);
    }

    @Nonnull
    public static String getTableName(@Nonnull Klass klass)
    {
        return TABLE_NAME_CONVERTER.convert(klass.getName());
    }

    @Nonnull
    private static String getPropertiesSourceCode(@Nonnull Klass klass)
    {
        return klass
                .getDataTypeProperties()
                .reject(DataTypeProperty::isDerived)
                .reject(DataTypeProperty::isTemporalRange)
                .collect(DdlGenerator::getPropertySourceCode)
                .makeString(",\n");
    }

    private static String getPropertySourceCode(DataTypeProperty dataTypeProperty)
    {
        String name = COLUMN_NAME_CONVERTER.convert(dataTypeProperty.getName());
        String dataType    = getDataType(dataTypeProperty);
        String nullability = getNullability(dataTypeProperty);
        return "    %s %s%s".formatted(
                name,
                dataType,
                nullability);
    }

    private static String getDataType(DataTypeProperty dataTypeProperty)
    {
        var visitor = new RelationalSchemaGeneratorDataTypePropertyVisitor();
        dataTypeProperty.visit(visitor);
        return visitor.getDataTypeSourceCode();
    }

    private static String getNullability(DataTypeProperty dataTypeProperty)
    {
        return dataTypeProperty.isTemporalInstant() || dataTypeProperty.isRequired()
                ? " not null"
                : "";
    }
}
