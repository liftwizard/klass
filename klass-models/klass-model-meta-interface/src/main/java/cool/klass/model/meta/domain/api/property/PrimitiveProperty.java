package cool.klass.model.meta.domain.api.property;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.modifier.Modifier;
import cool.klass.model.meta.domain.api.visitor.DataTypePropertyVisitor;
import cool.klass.model.meta.domain.api.visitor.PrimitiveTypeVisitor;

public interface PrimitiveProperty extends DataTypeProperty
{
    @Override
    default void visit(@Nonnull PropertyVisitor visitor)
    {
        visitor.visitPrimitiveProperty(this);
    }

    @Override
    default void visit(@Nonnull DataTypePropertyVisitor visitor)
    {
        PrimitiveTypeVisitor adaptor = new DataTypePropertyVisitorAdaptor(visitor, this);
        this.getType().visit(adaptor);
    }

    @Override
    default boolean isTemporalRange()
    {
        return this.getType().isTemporalRange();
    }

    @Override
    default boolean isTemporalInstant()
    {
        return this.getType().isTemporalInstant();
    }

    @Override
    default boolean isTemporal()
    {
        return this.getType().isTemporal();
    }

    @Override
    default boolean isVersion()
    {
        return this.getModifiers().anySatisfy(Modifier::isVersion);
    }

    @Override
    default boolean isID()
    {
        return this.getModifiers().anySatisfy(Modifier::isID);
    }

    @Override
    @Nonnull
    PrimitiveType getType();
}
