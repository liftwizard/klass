package cool.klass.generator.liquibase.schema;

import java.util.Objects;

import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.validation.NumericPropertyValidation;
import cool.klass.model.meta.domain.api.visitor.DataTypePropertyVisitor;

public class LiquibaseSchemaGeneratorDataTypePropertyVisitor
        implements DataTypePropertyVisitor
{
    private String dataTypeSourceCode;

    public String getDataTypeSourceCode()
    {
        Objects.requireNonNull(this.dataTypeSourceCode);
        return this.dataTypeSourceCode;
    }

    private void handleDataTypeProperty(DataTypeProperty dataTypeProperty)
    {
        int maxLength = dataTypeProperty.getMaxLengthPropertyValidation()
                .map(NumericPropertyValidation::getNumber)
                .orElse(255);
        this.dataTypeSourceCode = "varchar(" + maxLength + ')';
    }

    @Override
    public void visitEnumerationProperty(EnumerationProperty enumerationProperty)
    {
        this.handleDataTypeProperty(enumerationProperty);
    }

    @Override
    public void visitString(PrimitiveProperty primitiveProperty)
    {
        this.handleDataTypeProperty(primitiveProperty);
    }

    @Override
    public void visitInteger(PrimitiveProperty primitiveProperty)
    {
        this.dataTypeSourceCode = "int";
    }

    @Override
    public void visitLong(PrimitiveProperty primitiveProperty)
    {
        this.dataTypeSourceCode = "bigint";
    }

    @Override
    public void visitDouble(PrimitiveProperty primitiveProperty)
    {
        this.dataTypeSourceCode = "float8";
    }

    @Override
    public void visitFloat(PrimitiveProperty primitiveProperty)
    {
        this.dataTypeSourceCode = "float4";
    }

    @Override
    public void visitBoolean(PrimitiveProperty primitiveProperty)
    {
        this.dataTypeSourceCode = "boolean";
    }

    @Override
    public void visitInstant(PrimitiveProperty primitiveProperty)
    {
        this.dataTypeSourceCode = "timestamp";
    }

    @Override
    public void visitLocalDate(PrimitiveProperty primitiveProperty)
    {
        this.dataTypeSourceCode = "date";
    }

    @Override
    public void visitTemporalInstant(PrimitiveProperty primitiveProperty)
    {
        this.dataTypeSourceCode = "timestamp";
    }

    @Override
    public void visitTemporalRange(PrimitiveProperty primitiveProperty)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".visitTemporalRange() not implemented yet");
    }
}
