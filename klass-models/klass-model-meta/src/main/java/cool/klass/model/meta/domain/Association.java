package cool.klass.model.meta.domain;

import cool.klass.model.meta.domain.AssociationEnd.AssociationEndBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;

public final class Association extends PackageableElement
{
    private ImmutableList<AssociationEnd> associationEnds;

    private Association(
            ParserRuleContext elementContext,
            ParserRuleContext nameContext,
            String name,
            String packageName)
    {
        super(elementContext, nameContext, name, packageName);
    }

    private void setAssociationEnds(ImmutableList<AssociationEnd> associationEnds)
    {
        this.associationEnds = associationEnds;
    }

    public static class AssociationBuilder extends PackageableElementBuilder
    {
        private ImmutableList<AssociationEndBuilder> associationEnds;

        public AssociationBuilder(
                ParserRuleContext elementContext,
                ParserRuleContext nameContext,
                String name,
                String packageName)
        {
            super(elementContext, nameContext, name, packageName);
        }

        public void setAssociationEnds(ImmutableList<AssociationEndBuilder> associationEnds)
        {
            this.associationEnds = associationEnds;
        }

        public Association build()
        {
            Association association = new Association(
                    this.elementContext,
                    this.nameContext,
                    this.name,
                    this.packageName);

            ImmutableList<AssociationEnd> associationEnds = this.associationEnds
                    .collect(AssociationEndBuilder::getAssociationEnd);

            association.setAssociationEnds(associationEnds);
            return association;
        }
    }
}
