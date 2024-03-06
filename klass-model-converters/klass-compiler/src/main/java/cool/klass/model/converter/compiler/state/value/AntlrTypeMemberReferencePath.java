package cool.klass.model.converter.compiler.state.value;

import java.util.List;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.error.CompilerErrorHolder;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrEnumeration;
import cool.klass.model.converter.compiler.state.AntlrType;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEnd;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrEnumerationProperty;
import cool.klass.model.meta.domain.value.TypeMemberReferencePath.TypeMemberReferencePathBuilder;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ClassReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.TypeMemberReferencePathContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

public class AntlrTypeMemberReferencePath extends AntlrMemberExpressionValue
{
    public AntlrTypeMemberReferencePath(
            @Nonnull TypeMemberReferencePathContext elementContext,
            CompilationUnit compilationUnit,
            boolean inferred,
            @Nonnull AntlrClass classState,
            ImmutableList<AntlrAssociationEnd> associationEndStates,
            @Nonnull AntlrDataTypeProperty<?> dataTypePropertyState)
    {
        super(elementContext, compilationUnit, inferred, classState, associationEndStates, dataTypePropertyState);
    }

    @Nonnull
    @Override
    public TypeMemberReferencePathBuilder build()
    {
        return new TypeMemberReferencePathBuilder(
                this.elementContext,
                this.inferred,
                this.classState.getKlassBuilder(),
                this.associationEndStates.collect(AntlrAssociationEnd::getAssociationEndBuilder),
                this.dataTypePropertyState.getPropertyBuilder());
    }

    @Override
    public void reportErrors(
            @Nonnull CompilerErrorHolder compilerErrorHolder,
            @Nonnull ImmutableList<ParserRuleContext> parserRuleContexts)
    {
        // TODO: This error message is firing for ambiguity, not just NOT_FOUND.

        if (this.classState == AntlrClass.AMBIGUOUS || this.classState == AntlrClass.NOT_FOUND)
        {
            ClassReferenceContext offendingToken = this.getElementContext().classReference();

            // TODO: This error message is firing for ambiguity, not just NOT_FOUND.
            String message = String.format(
                    "ERR_MEM_TYP: Cannot find class '%s'.",
                    offendingToken.getText());

            compilerErrorHolder.add(message, offendingToken);
            return;
        }

        List<AssociationEndReferenceContext> associationEndReferenceContexts = this.getElementContext().associationEndReference();
        AntlrClass currentClassState = this.reportErrorsAssociationEnds(
                compilerErrorHolder,
                parserRuleContexts,
                associationEndReferenceContexts);
        if (currentClassState == null)
        {
            return;
        }

        if (this.dataTypePropertyState == AntlrEnumerationProperty.NOT_FOUND)
        {
            IdentifierContext identifier = this.getElementContext().memberReference().identifier();
            String message = String.format(
                    "ERR_TYP_MEM: Cannot find member '%s.%s'.",
                    currentClassState.getName(),
                    identifier.getText());
            compilerErrorHolder.add(
                    message,
                    identifier,
                    parserRuleContexts.toArray(new ParserRuleContext[]{}));
        }
    }

    @Nonnull
    @Override
    public ImmutableList<AntlrType> getPossibleTypes()
    {
        AntlrType type = this.dataTypePropertyState.getType();
        if (type == AntlrEnumeration.NOT_FOUND)
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(type);
    }

    @Nonnull
    @Override
    public TypeMemberReferencePathContext getElementContext()
    {
        return (TypeMemberReferencePathContext) super.getElementContext();
    }
}
