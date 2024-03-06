package cool.klass.model.reladomo.tree;

import java.util.Objects;

import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Klass;

public class RootReladomoTreeNode
        extends AbstractReladomoTreeNode
{
    private final Klass klass;

    public RootReladomoTreeNode(String name, Klass klass)
    {
        super(name);
        this.klass = Objects.requireNonNull(klass);
    }

    @Override
    public void visit(ReladomoTreeNodeVisitor visitor)
    {
        visitor.visitRoot(this);
    }

    @Override
    public Classifier getOwningClassifier()
    {
        return this.klass;
    }

    @Override
    public Classifier getType()
    {
        return this.klass;
    }

    @Override
    public String getShortString()
    {
        return this.getType().getName();
    }

    @Override
    public String getNodeString(String indent)
    {
        return indent + this.getShortString() + ": " + this.getType().getName() + "\n";
    }
}
