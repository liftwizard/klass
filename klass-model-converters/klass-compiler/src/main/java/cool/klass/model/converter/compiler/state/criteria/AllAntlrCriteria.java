package cool.klass.model.converter.compiler.state.criteria;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.error.CompilerErrorHolder;
import cool.klass.model.converter.compiler.state.service.CriteriaOwner;
import cool.klass.model.converter.compiler.state.service.url.AntlrUrlParameter;
import cool.klass.model.meta.domain.criteria.AllCriteria.AllCriteriaBuilder;
import cool.klass.model.meta.grammar.KlassParser.CriteriaAllContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.OrderedMap;

public class AllAntlrCriteria extends AntlrCriteria
{
    public AllAntlrCriteria(
            @Nonnull CriteriaAllContext elementContext,
            @Nonnull CompilationUnit compilationUnit,
            boolean inferred,
            @Nonnull CriteriaOwner criteriaOwner)
    {
        super(elementContext, compilationUnit, inferred, criteriaOwner);
    }

    @Nonnull
    @Override
    public CriteriaAllContext getElementContext()
    {
        return (CriteriaAllContext) super.getElementContext();
    }

    @Nonnull
    @Override
    public AllCriteriaBuilder build()
    {
        return new AllCriteriaBuilder(this.elementContext, this.inferred);
    }

    @Override
    public void reportErrors(
            CompilerErrorHolder compilerErrorHolder,
            ImmutableList<ParserRuleContext> parserRuleContexts)
    {
        // Intentionally blank
    }

    @Override
    public void resolveServiceVariables(OrderedMap<String, AntlrUrlParameter> formalParametersByName)
    {
        // Intentionally blank
    }

    @Override
    public void resolveTypes()
    {
        // Intentionally blank
    }
}
