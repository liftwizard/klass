package cool.klass.model.meta.domain.property;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.EnumerationImpl;
import cool.klass.model.meta.domain.EnumerationImpl.EnumerationBuilder;
import cool.klass.model.meta.domain.KlassImpl;
import cool.klass.model.meta.domain.KlassImpl.KlassBuilder;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PropertyModifier;
import cool.klass.model.meta.domain.property.PropertyModifierImpl.PropertyModifierBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;

public final class EnumerationPropertyImpl extends AbstractDataTypeProperty<EnumerationImpl> implements EnumerationProperty
{
    private EnumerationPropertyImpl(
            @Nonnull ParserRuleContext elementContext,
            boolean inferred,
            @Nonnull ParserRuleContext nameContext,
            @Nonnull String name,
            int ordinal,
            @Nonnull EnumerationImpl enumeration,
            @Nonnull KlassImpl owningKlass,
            boolean isKey,
            boolean isOptional)
    {
        super(
                elementContext,
                inferred,
                nameContext,
                name,
                ordinal,
                enumeration,
                owningKlass,
                isKey,
                isOptional);
    }

    @Override
    public boolean isTemporalRange()
    {
        return false;
    }

    @Override
    public boolean isTemporalInstant()
    {
        return false;
    }

    @Override
    public boolean isTemporal()
    {
        return false;
    }

    public static final class EnumerationPropertyBuilder extends DataTypePropertyBuilder<EnumerationImpl, EnumerationBuilder, EnumerationPropertyImpl>
    {
        public EnumerationPropertyBuilder(
                @Nonnull ParserRuleContext elementContext,
                boolean inferred,
                @Nonnull ParserRuleContext nameContext,
                @Nonnull String name,
                int ordinal,
                @Nonnull EnumerationBuilder enumerationBuilder,
                @Nonnull KlassBuilder owningKlassBuilder,
                @Nonnull ImmutableList<PropertyModifierBuilder> propertyModifierBuilders,
                boolean isKey,
                boolean isOptional)
        {
            super(
                    elementContext,
                    inferred,
                    nameContext,
                    name,
                    ordinal,
                    enumerationBuilder,
                    owningKlassBuilder,
                    propertyModifierBuilders,
                    isKey,
                    isOptional);
        }

        @Override
        @Nonnull
        protected EnumerationPropertyImpl buildUnsafe()
        {
            return new EnumerationPropertyImpl(
                    this.elementContext,
                    this.inferred,
                    this.nameContext,
                    this.name,
                    this.ordinal,
                    this.typeBuilder.getElement(),
                    this.owningKlassBuilder.getElement(),
                    this.isKey,
                    this.isOptional);
        }

        @Override
        protected void buildChildren()
        {
            ImmutableList<PropertyModifier> propertyModifiers =
                    this.propertyModifierBuilders.collect(PropertyModifierBuilder::build);
            this.element.setPropertyModifiers(propertyModifiers);
        }
    }
}
