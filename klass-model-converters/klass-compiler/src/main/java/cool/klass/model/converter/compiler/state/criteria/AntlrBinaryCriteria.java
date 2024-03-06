package cool.klass.model.converter.compiler.state.criteria;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.parameter.AntlrParameter;
import cool.klass.model.meta.domain.criteria.AbstractBinaryCriteria.AbstractBinaryCriteriaBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.map.OrderedMap;

public abstract class AntlrBinaryCriteria
        extends AntlrCriteria
{
    protected AntlrCriteria left;
    protected AntlrCriteria right;

    protected AntlrBinaryCriteria(
            @Nonnull ParserRuleContext elementContext,
            @Nonnull Optional<CompilationUnit> compilationUnit,
            @Nonnull IAntlrElement criteriaOwner)
    {
        super(elementContext, compilationUnit, criteriaOwner);
    }

    @Nonnull
    @Override
    public abstract AbstractBinaryCriteriaBuilder<?> build();

    @Nonnull
    @Override
    public abstract AbstractBinaryCriteriaBuilder<?> getElementBuilder();

    public void setLeft(@Nonnull AntlrCriteria left)
    {
        if (this.left != null)
        {
            throw new IllegalStateException();
        }
        this.left = Objects.requireNonNull(left);
    }

    public void setRight(@Nonnull AntlrCriteria right)
    {
        if (this.right != null)
        {
            throw new IllegalStateException();
        }
        this.right = Objects.requireNonNull(right);
    }

    @Override
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder)
    {
        // TODO: Error if both clauses are identical, or if any left true subclause is a subclause of the right
        // Java | Probable bugs | Constant conditions & exceptions

        this.left.reportErrors(compilerAnnotationHolder);
        this.right.reportErrors(compilerAnnotationHolder);
    }

    @Override
    public final void resolveServiceVariables(@Nonnull OrderedMap<String, AntlrParameter> formalParametersByName)
    {
        this.left.resolveServiceVariables(formalParametersByName);
        this.right.resolveServiceVariables(formalParametersByName);
    }

    @Override
    public final void resolveTypes()
    {
        this.left.resolveTypes();
        this.right.resolveTypes();
    }
}
