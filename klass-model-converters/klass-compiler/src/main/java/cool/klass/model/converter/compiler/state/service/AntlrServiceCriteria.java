package cool.klass.model.converter.compiler.state.service;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.error.CompilerErrorHolder;
import cool.klass.model.converter.compiler.state.AntlrElement;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.criteria.AntlrCriteria;
import cool.klass.model.meta.grammar.KlassParser.ServiceCriteriaDeclarationContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;

public class AntlrServiceCriteria extends AntlrElement
{
    @Nonnull
    private final String       serviceCriteriaKeyword;
    @Nonnull
    private final AntlrService serviceState;

    private AntlrCriteria antlrCriteria;

    public AntlrServiceCriteria(
            @Nonnull ServiceCriteriaDeclarationContext elementContext,
            @Nullable CompilationUnit compilationUnit,
            boolean inferred,
            @Nonnull String serviceCriteriaKeyword,
            AntlrService serviceState)
    {
        super(elementContext, compilationUnit, inferred);
        this.serviceCriteriaKeyword = Objects.requireNonNull(serviceCriteriaKeyword);
        this.serviceState = Objects.requireNonNull(serviceState);
    }

    @Override
    public boolean omitParentFromSurroundingElements()
    {
        return false;
    }

    @Nonnull
    @Override
    public Optional<IAntlrElement> getSurroundingElement()
    {
        return Optional.of(this.serviceState);
    }

    @Nonnull
    public AntlrCriteria getCriteria()
    {
        return Objects.requireNonNull(this.antlrCriteria);
    }

    public void setCriteria(@Nonnull AntlrCriteria antlrCriteria)
    {
        this.antlrCriteria = Objects.requireNonNull(antlrCriteria);
    }

    public void reportDuplicateKeyword(@Nonnull CompilerErrorHolder compilerErrorHolder)
    {
        // TODO: Test coverage of duplicate service criteria
        String message = String.format("ERR_DUP_CRI: Duplicate service criteria: '%s'.", this.serviceCriteriaKeyword);
        compilerErrorHolder.add(message, this.antlrCriteria, this.getElementContext());
    }

    @Nonnull
    @Override
    public ServiceCriteriaDeclarationContext getElementContext()
    {
        return (ServiceCriteriaDeclarationContext) super.getElementContext();
    }

    @Override
    public void getParserRuleContexts(@Nonnull MutableList<ParserRuleContext> parserRuleContexts)
    {
        parserRuleContexts.add(this.elementContext);
        this.serviceState.getParserRuleContexts(parserRuleContexts);
    }

    @Nonnull
    public String getServiceCriteriaKeyword()
    {
        return this.serviceCriteriaKeyword;
    }

    public void reportAllowedCriteriaTypes(
            @Nonnull CompilerErrorHolder compilerErrorHolder,
            @Nonnull ImmutableList<String> allowedCriteriaTypes)
    {
        if (!allowedCriteriaTypes.contains(this.serviceCriteriaKeyword))
        {
            String error = String.format(
                    "Criteria '%s' not allowed for verb '%s'. Must be one of %s.",
                    this.serviceCriteriaKeyword,
                    this.serviceState.getVerbState().getVerb(),
                    allowedCriteriaTypes);
            compilerErrorHolder.add(
                    error,
                    this,
                    this.getElementContext().serviceCriteriaKeyword());
        }
    }
}
