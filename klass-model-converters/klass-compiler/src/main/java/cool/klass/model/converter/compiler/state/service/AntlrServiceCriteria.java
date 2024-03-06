package cool.klass.model.converter.compiler.state.service;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.error.CompilerErrorState;
import cool.klass.model.converter.compiler.state.AntlrElement;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.criteria.AntlrCriteria;
import cool.klass.model.meta.grammar.KlassParser.ServiceCriteriaDeclarationContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.Pair;

public class AntlrServiceCriteria extends AntlrElement
{
    @Nonnull
    private final String       serviceCriteriaKeyword;
    @Nonnull
    private final AntlrService serviceState;

    private AntlrCriteria antlrCriteria;

    public AntlrServiceCriteria(
            @Nonnull ServiceCriteriaDeclarationContext elementContext,
            @Nonnull Optional<CompilationUnit> compilationUnit,
            @Nonnull String serviceCriteriaKeyword,
            @Nonnull AntlrService serviceState)
    {
        super(elementContext, compilationUnit);
        this.serviceCriteriaKeyword = Objects.requireNonNull(serviceCriteriaKeyword);
        this.serviceState           = Objects.requireNonNull(serviceState);
    }

    @Nonnull
    @Override
    public Optional<IAntlrElement> getSurroundingElement()
    {
        return Optional.of(this.serviceState);
    }

    @Override
    public boolean isContext()
    {
        return true;
    }

    @Override
    public Pair<Token, Token> getContextBefore()
    {
        return this.getEntireContext();
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

    public void reportDuplicateKeyword(@Nonnull CompilerErrorState compilerErrorHolder)
    {
        // TODO: Test coverage of duplicate service criteria
        String message = String.format("Duplicate service criteria: '%s'.", this.serviceCriteriaKeyword);
        compilerErrorHolder.add("ERR_DUP_CRI", message, this.antlrCriteria, this.getElementContext());
    }

    @Nonnull
    @Override
    public ServiceCriteriaDeclarationContext getElementContext()
    {
        return (ServiceCriteriaDeclarationContext) super.getElementContext();
    }

    @Nonnull
    public String getServiceCriteriaKeyword()
    {
        return this.serviceCriteriaKeyword;
    }

    public void reportAllowedCriteriaTypes(
            @Nonnull CompilerErrorState compilerErrorHolder,
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
                    "ERR_VRB_CRT",
                    error,
                    this,
                    this.getElementContext().serviceCriteriaKeyword());
        }
    }
}
