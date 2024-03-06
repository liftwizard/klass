package cool.klass.model.reladomo.projection;

import java.util.LinkedHashMap;
import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Interface;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.MapAdapter;

public abstract class AbstractProjectionElementReladomoNode
        implements ProjectionElementReladomoNode
{
    protected final MutableMap<String, ProjectionElementReladomoNode> children = MapAdapter.adapt(new LinkedHashMap<>());

    protected RootReladomoNode rootReladomoNode;

    private final String name;

    protected AbstractProjectionElementReladomoNode(@Nonnull String name)
    {
        this.name = Objects.requireNonNull(name);
    }

    @Nonnull
    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public RootReladomoNode getRootReladomoNode()
    {
        return this.rootReladomoNode;
    }

    @Override
    public MutableMap<String, ProjectionElementReladomoNode> getChildren()
    {
        if (this.rootReladomoNode == null)
        {
            return this.children;
        }

        if (this.children.notEmpty())
        {
            throw new AssertionError(this.children);
        }

        return this.rootReladomoNode.getChildren();
    }

    @Override
    public ProjectionElementReladomoNode computeChild(String name, ProjectionElementReladomoNode child)
    {
        if (this.rootReladomoNode != null)
        {
            throw new AssertionError(this.rootReladomoNode);
        }

        if (this.getType() != child.getOwningClassifier() && !(child.getOwningClassifier() instanceof Interface))
        {
            String detailMessage = this.getType() + " != " + child.getOwningClassifier();
            throw new AssertionError(detailMessage);
        }
        ProjectionElementReladomoNode result = this.children.getIfAbsentPut(name, child);
        if (result != child)
        {
            if (!result.equals(child))
            {
                throw new AssertionError(result + " != " + child);
            }
        }
        return result;
    }

    @Override
    public void setProjection(
            RootReladomoNode rootReladomoNode)
    {
        if (this.children.notEmpty())
        {
            throw new AssertionError(this.children);
        }

        this.rootReladomoNode = Objects.requireNonNull(rootReladomoNode);
    }

    @Override
    public String toString()
    {
        return this.getNodeString();
    }
}
