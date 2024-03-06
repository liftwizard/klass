package cool.klass.model.converter.compiler.phase.criteria;

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrDomainModel;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEnd;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.value.AntlrExpressionValue;
import cool.klass.model.converter.compiler.state.value.AntlrThisMemberReferencePath;
import cool.klass.model.converter.compiler.state.value.AntlrTypeMemberReferencePath;
import cool.klass.model.converter.compiler.state.value.AntlrVariableReference;
import cool.klass.model.converter.compiler.state.value.literal.AntlrLiteralListValue;
import cool.klass.model.converter.compiler.state.value.literal.AntlrLiteralValue;
import cool.klass.model.converter.compiler.state.value.literal.AntlrUserLiteral;
import cool.klass.model.meta.grammar.KlassBaseVisitor;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ClassReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.LiteralContext;
import cool.klass.model.meta.grammar.KlassParser.LiteralListContext;
import cool.klass.model.meta.grammar.KlassParser.MemberReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.NativeLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.ThisMemberReferencePathContext;
import cool.klass.model.meta.grammar.KlassParser.TypeMemberReferencePathContext;
import cool.klass.model.meta.grammar.KlassParser.VariableReferenceContext;
import cool.klass.model.meta.grammar.KlassVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.ListAdapter;

public class ExpressionValueVisitor extends KlassBaseVisitor<AntlrExpressionValue>
{
    @Nonnull
    private final CompilationUnit  compilationUnit;
    @Nonnull
    private final AntlrClass       thisReference;
    @Nonnull
    private final AntlrDomainModel domainModelState;
    @Nonnull
    private final IAntlrElement    expressionValueOwner;

    public ExpressionValueVisitor(
            @Nonnull CompilationUnit compilationUnit,
            @Nonnull AntlrClass thisReference,
            @Nonnull AntlrDomainModel domainModelState,
            IAntlrElement expressionValueOwner)
    {
        this.compilationUnit = Objects.requireNonNull(compilationUnit);
        this.thisReference = Objects.requireNonNull(thisReference);
        this.domainModelState = Objects.requireNonNull(domainModelState);
        this.expressionValueOwner = Objects.requireNonNull(expressionValueOwner);
    }

    @Nonnull
    @Override
    public AntlrExpressionValue visitTerminal(TerminalNode node)
    {
        throw new AssertionError();
    }

    @Nonnull
    @Override
    public AntlrLiteralListValue visitLiteralList(@Nonnull LiteralListContext ctx)
    {
        AntlrLiteralListValue literalListValue = new AntlrLiteralListValue(
                ctx,
                this.compilationUnit,
                false,
                this.expressionValueOwner);

        ImmutableList<AntlrLiteralValue> literalStates = ListAdapter.adapt(ctx.literal())
                .collectWith(this::getAntlrLiteralValue, literalListValue)
                .toImmutable();
        literalListValue.setLiteralStates(literalStates);

        return literalListValue;
    }

    protected AntlrLiteralValue getAntlrLiteralValue(LiteralContext literalCtx, IAntlrElement expressionValueOwner)
    {
        // TODO: Recurse here using a different owner?
        KlassVisitor<AntlrLiteralValue> visitor = new LiteralValueVisitor(
                this.compilationUnit,
                this.domainModelState,
                expressionValueOwner);
        return visitor.visitLiteral(literalCtx);
    }

    @Nonnull
    @Override
    public AntlrExpressionValue visitNativeLiteral(@Nonnull NativeLiteralContext ctx)
    {
        String keyword = ctx.getText();
        switch (keyword)
        {
            case "user":
                return new AntlrUserLiteral(ctx, this.compilationUnit, false, this.expressionValueOwner);
            default:
                throw new AssertionError(keyword);
        }
    }

    @Nonnull
    @Override
    public AntlrVariableReference visitVariableReference(@Nonnull VariableReferenceContext ctx)
    {
        IdentifierContext identifier   = ctx.identifier();
        String            variableName = identifier.getText();
        return new AntlrVariableReference(ctx, this.compilationUnit, false, variableName, this.expressionValueOwner);
    }

    @Nonnull
    @Override
    public AntlrThisMemberReferencePath visitThisMemberReferencePath(@Nonnull ThisMemberReferencePathContext ctx)
    {
        MemberReferenceContext memberReferenceContext = ctx.memberReference();

        AntlrClass                       currentClassState    = this.thisReference;
        MutableList<AntlrAssociationEnd> associationEndStates = Lists.mutable.empty();
        for (AssociationEndReferenceContext associationEndReferenceContext : ctx.associationEndReference())
        {
            // TODO: Or parameterizedPropertyName?
            String              associationEndName  = associationEndReferenceContext.identifier().getText();
            AntlrAssociationEnd associationEndState = currentClassState.getAssociationEndByName(associationEndName);
            associationEndStates.add(associationEndState);
            currentClassState = associationEndState.getType();
        }

        String                   memberName            = memberReferenceContext.identifier().getText();
        AntlrDataTypeProperty<?> dataTypePropertyState = currentClassState.getDataTypePropertyByName(memberName);

        return new AntlrThisMemberReferencePath(
                ctx,
                this.compilationUnit,
                false,
                this.thisReference,
                associationEndStates.toImmutable(),
                dataTypePropertyState,
                this.expressionValueOwner);
    }

    @Nonnull
    @Override
    public AntlrTypeMemberReferencePath visitTypeMemberReferencePath(@Nonnull TypeMemberReferencePathContext ctx)
    {
        ClassReferenceContext classReferenceContext = ctx.classReference();
        String                className             = classReferenceContext.identifier().getText();
        AntlrClass            classState            = this.domainModelState.getClassByName(className);

        MemberReferenceContext memberReferenceContext = ctx.memberReference();

        AntlrClass                       currentClassState    = classState;
        MutableList<AntlrAssociationEnd> associationEndStates = Lists.mutable.empty();
        for (AssociationEndReferenceContext associationEndReferenceContext : ctx.associationEndReference())
        {
            // TODO: Or parameterizedPropertyName?
            String              associationEndName  = associationEndReferenceContext.identifier().getText();
            AntlrAssociationEnd associationEndState = currentClassState.getAssociationEndByName(associationEndName);
            associationEndStates.add(associationEndState);
            currentClassState = associationEndState.getType();
        }

        String                   memberName            = memberReferenceContext.identifier().getText();
        AntlrDataTypeProperty<?> dataTypePropertyState = currentClassState.getDataTypePropertyByName(memberName);

        return new AntlrTypeMemberReferencePath(
                ctx,
                this.compilationUnit,
                false,
                classState,
                associationEndStates.toImmutable(),
                dataTypePropertyState,
                this.expressionValueOwner);
    }

    @Override
    public AntlrLiteralValue visitLiteral(LiteralContext ctx)
    {
        return this.getAntlrLiteralValue(ctx, this.expressionValueOwner);
    }
}
