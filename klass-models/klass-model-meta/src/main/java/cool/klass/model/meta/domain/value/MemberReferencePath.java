package cool.klass.model.meta.domain.value;

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.Klass;
import cool.klass.model.meta.domain.Klass.KlassBuilder;
import cool.klass.model.meta.domain.property.AssociationEnd;
import cool.klass.model.meta.domain.property.AssociationEnd.AssociationEndBuilder;
import cool.klass.model.meta.domain.property.DataTypeProperty;
import cool.klass.model.meta.domain.property.DataTypeProperty.DataTypePropertyBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;

public abstract class MemberReferencePath extends ExpressionValue
{
    @Nonnull
    private final Klass                         klass;
    @Nonnull
    private final ImmutableList<AssociationEnd> associationEnds;
    @Nonnull
    private final DataTypeProperty<?>           property;

    protected MemberReferencePath(
            @Nonnull ParserRuleContext elementContext,
            boolean inferred,
            @Nonnull Klass klass,
            @Nonnull ImmutableList<AssociationEnd> associationEnds,
            @Nonnull DataTypeProperty<?> property)
    {
        super(elementContext, inferred);
        this.klass = Objects.requireNonNull(klass);
        this.associationEnds = Objects.requireNonNull(associationEnds);
        this.property = Objects.requireNonNull(property);
    }

    @Nonnull
    public Klass getKlass()
    {
        return this.klass;
    }

    @Nonnull
    public ImmutableList<AssociationEnd> getAssociationEnds()
    {
        return this.associationEnds;
    }

    @Nonnull
    public DataTypeProperty<?> getProperty()
    {
        return this.property;
    }

    public abstract static class MemberReferencePathBuilder extends ExpressionValueBuilder
    {
        @Nonnull
        protected final KlassBuilder                         klassBuilder;
        @Nonnull
        protected final ImmutableList<AssociationEndBuilder> associationEndBuilders;
        @Nonnull
        protected final DataTypePropertyBuilder<?, ?>        propertyBuilder;

        protected MemberReferencePathBuilder(
                @Nonnull ParserRuleContext elementContext,
                boolean inferred,
                @Nonnull KlassBuilder klassBuilder,
                @Nonnull ImmutableList<AssociationEndBuilder> associationEndBuilders,
                @Nonnull DataTypePropertyBuilder<?, ?> propertyBuilder)
        {
            super(elementContext, inferred);
            this.klassBuilder = Objects.requireNonNull(klassBuilder);
            this.associationEndBuilders = Objects.requireNonNull(associationEndBuilders);
            this.propertyBuilder = Objects.requireNonNull(propertyBuilder);
        }

        @Nonnull
        @Override
        public abstract MemberReferencePath build();
    }
}
