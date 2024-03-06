package cool.klass.model.converter.compiler.state.criteria;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.error.CompilerErrorState;
import cool.klass.model.converter.compiler.state.AntlrType;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.operator.AntlrOperator;
import cool.klass.model.converter.compiler.state.parameter.AntlrParameter;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEnd;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.value.AntlrExpressionValue;
import cool.klass.model.converter.compiler.state.value.AntlrThisMemberReferencePath;
import cool.klass.model.converter.compiler.state.value.AntlrTypeMemberReferencePath;
import cool.klass.model.converter.compiler.state.value.literal.AbstractAntlrLiteralValue;
import cool.klass.model.meta.domain.criteria.OperatorCriteriaImpl.OperatorCriteriaBuilder;
import cool.klass.model.meta.grammar.KlassParser.CriteriaOperatorContext;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.OrderedMap;

public class OperatorAntlrCriteria extends AntlrCriteria
{
    @Nonnull
    private final AntlrOperator operator;

    @Nullable
    private AntlrExpressionValue sourceValue;
    @Nullable
    private AntlrExpressionValue targetValue;

    public OperatorAntlrCriteria(
            @Nonnull CriteriaOperatorContext elementContext,
            @Nonnull CompilationUnit compilationUnit,
            boolean inferred,
            @Nonnull IAntlrElement criteriaOwner,
            @Nonnull AntlrOperator operator)
    {
        super(elementContext, compilationUnit, inferred, criteriaOwner);
        this.operator = Objects.requireNonNull(operator);
    }

    @Nonnull
    @Override
    public CriteriaOperatorContext getElementContext()
    {
        return (CriteriaOperatorContext) super.getElementContext();
    }

    @Nullable
    public AntlrExpressionValue getSourceValue()
    {
        return Objects.requireNonNull(this.sourceValue);
    }

    public void setSourceValue(@Nullable AntlrExpressionValue sourceValue)
    {
        if (this.sourceValue != null)
        {
            throw new IllegalArgumentException();
        }
        this.sourceValue = Objects.requireNonNull(sourceValue);
    }

    @Nullable
    public AntlrExpressionValue getTargetValue()
    {
        return Objects.requireNonNull(this.targetValue);
    }

    public void setTargetValue(@Nullable AntlrExpressionValue targetValue)
    {
        if (this.targetValue != null)
        {
            throw new IllegalArgumentException();
        }
        this.targetValue = Objects.requireNonNull(targetValue);
    }

    @Nonnull
    @Override
    public OperatorCriteriaBuilder build()
    {
        // TODO: Refactor to build the parent before the children
        return new OperatorCriteriaBuilder(
                this.elementContext,
                this.inferred,
                this.operator.build(),
                this.sourceValue.build(),
                this.targetValue.build());
    }

    @Override
    public void reportErrors(CompilerErrorState compilerErrorHolder)
    {
        this.sourceValue.reportErrors(compilerErrorHolder);
        this.targetValue.reportErrors(compilerErrorHolder);
        ListIterable<AntlrType> sourceTypes = this.sourceValue.getPossibleTypes();
        ListIterable<AntlrType> targetTypes = this.targetValue.getPossibleTypes();
        this.operator.checkTypes(compilerErrorHolder, sourceTypes, targetTypes);
    }

    @Override
    public void resolveServiceVariables(OrderedMap<String, AntlrParameter> formalParametersByName)
    {
        this.sourceValue.resolveServiceVariables(formalParametersByName);
        this.targetValue.resolveServiceVariables(formalParametersByName);
    }

    @Override
    public void resolveTypes()
    {
        ImmutableList<AntlrType> sourcePossibleTypes = this.sourceValue.getPossibleTypes();
        ImmutableList<AntlrType> targetPossibleTypes = this.targetValue.getPossibleTypes();

        if (this.sourceValue instanceof AbstractAntlrLiteralValue)
        {
            if (targetPossibleTypes.size() != 1)
            {
                throw new AssertionError();
            }
            ((AbstractAntlrLiteralValue) this.sourceValue).setInferredType(targetPossibleTypes.getOnly());
        }
        if (this.targetValue instanceof AbstractAntlrLiteralValue)
        {
            if (sourcePossibleTypes.size() != 1)
            {
                throw new AssertionError();
            }
            ((AbstractAntlrLiteralValue) this.targetValue).setInferredType(sourcePossibleTypes.getOnly());
        }
    }

    @Override
    public void addForeignKeys(
            boolean foreignKeysOnThis,
            AntlrAssociationEnd endWithForeignKeys)
    {
        AntlrThisMemberReferencePath thisMemberReferencePathState = this.getThisMemberRefrencePathState();
        AntlrTypeMemberReferencePath typeMemberReferencePathState = this.getTypeMemberRefrencePathState();

        AntlrDataTypeProperty<?> thisDataTypePropertyState = thisMemberReferencePathState.getDataTypePropertyState();
        AntlrDataTypeProperty<?> typeDataTypePropertyState = typeMemberReferencePathState.getDataTypePropertyState();

        boolean foreignKeysOnThis2 = endWithForeignKeys.getOwningClassState() == thisMemberReferencePathState.getClassState();
        if (foreignKeysOnThis2 != foreignKeysOnThis)
        {
            throw new AssertionError();
        }

        if (endWithForeignKeys.getOwningClassState() == thisMemberReferencePathState.getClassState())
        {
            endWithForeignKeys.addForeignKeyPropertyMatchingProperty(thisDataTypePropertyState, typeDataTypePropertyState);
        }
        else
        {
            endWithForeignKeys.addForeignKeyPropertyMatchingProperty(typeDataTypePropertyState, thisDataTypePropertyState);
        }
    }

    private AntlrThisMemberReferencePath getThisMemberRefrencePathState()
    {
        if (this.sourceValue instanceof AntlrThisMemberReferencePath)
        {
            return (AntlrThisMemberReferencePath) this.sourceValue;
        }

        if (this.targetValue instanceof AntlrThisMemberReferencePath)
        {
            return (AntlrThisMemberReferencePath) this.targetValue;
        }

        throw new AssertionError();
    }

    private AntlrTypeMemberReferencePath getTypeMemberRefrencePathState()
    {
        if (this.sourceValue instanceof AntlrTypeMemberReferencePath)
        {
            return (AntlrTypeMemberReferencePath) this.sourceValue;
        }

        if (this.targetValue instanceof AntlrTypeMemberReferencePath)
        {
            return (AntlrTypeMemberReferencePath) this.targetValue;
        }

        throw new AssertionError();
    }
}
