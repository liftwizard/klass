package cool.klass.model.meta.domain.api.property;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Association;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.modifier.AssociationEndModifier;
import org.eclipse.collections.api.list.ImmutableList;

public interface AssociationEnd extends ReferenceProperty
{
    @Nonnull
    @Override
    Klass getType();

    @Nonnull
    @Override
    Klass getOwningClassifier();

    @Override
    default void visit(@Nonnull PropertyVisitor visitor)
    {
        visitor.visitAssociationEnd(this);
    }

    @Nonnull
    ImmutableList<AssociationEndModifier> getAssociationEndModifiers();

    // TODO: Delete overrides
    default boolean isOwned()
    {
        return this.getAssociationEndModifiers().anySatisfy(AssociationEndModifier::isOwned);
    }

    default boolean isVersion()
    {
        return this.getAssociationEndModifiers().anySatisfy(AssociationEndModifier::isVersion);
    }

    default boolean isFinal()
    {
        return this.getAssociationEndModifiers().anySatisfy(AssociationEndModifier::isFinal);
    }

    default boolean isPrivate()
    {
        return this.getAssociationEndModifiers().anySatisfy(AssociationEndModifier::isPrivate);
    }

    @Nonnull
    default AssociationEnd getOpposite()
    {
        Association association = this.getOwningAssociation();

        if (this == association.getSourceAssociationEnd())
        {
            return association.getTargetAssociationEnd();
        }

        if (this == association.getTargetAssociationEnd())
        {
            return association.getSourceAssociationEnd();
        }

        throw new AssertionError();
    }

    @Nonnull
    Association getOwningAssociation();
}
