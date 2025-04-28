/*
 * Copyright 2024 Craig Motlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cool.klass.model.converter.compiler.phase.criteria;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.criteria.AllAntlrCriteria;
import cool.klass.model.converter.compiler.state.criteria.AntlrAndCriteria;
import cool.klass.model.converter.compiler.state.criteria.AntlrCriteria;
import cool.klass.model.converter.compiler.state.criteria.AntlrOrCriteria;
import cool.klass.model.converter.compiler.state.criteria.EdgePointAntlrCriteria;
import cool.klass.model.converter.compiler.state.criteria.OperatorAntlrCriteria;
import cool.klass.model.converter.compiler.state.operator.AntlrOperator;
import cool.klass.model.converter.compiler.state.value.AntlrExpressionValue;
import cool.klass.model.converter.compiler.state.value.AntlrMemberReferencePath;
import cool.klass.model.meta.grammar.KlassBaseVisitor;
import cool.klass.model.meta.grammar.KlassParser.CriteriaAllContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaEdgePointContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaExpressionAndContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaExpressionContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaExpressionGroupContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaExpressionOrContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaNativeContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaOperatorContext;
import cool.klass.model.meta.grammar.KlassParser.EqualityOperatorContext;
import cool.klass.model.meta.grammar.KlassParser.InOperatorContext;
import cool.klass.model.meta.grammar.KlassParser.InequalityOperatorContext;
import cool.klass.model.meta.grammar.KlassParser.LiteralContext;
import cool.klass.model.meta.grammar.KlassParser.LiteralListContext;
import cool.klass.model.meta.grammar.KlassParser.NativeLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.StringOperatorContext;
import cool.klass.model.meta.grammar.KlassParser.TypeMemberReferencePathContext;
import cool.klass.model.meta.grammar.KlassVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

public class CriteriaVisitor extends KlassBaseVisitor<AntlrCriteria> {

    @Nonnull
    private final CompilerState compilerState;

    @Nonnull
    private final IAntlrElement criteriaOwner;

    public CriteriaVisitor(@Nonnull CompilerState compilerState, @Nonnull IAntlrElement criteriaOwner) {
        this.compilerState = Objects.requireNonNull(compilerState);
        this.criteriaOwner = Objects.requireNonNull(criteriaOwner);
    }

    @Nonnull
    @Override
    public AntlrCriteria visitCriteriaEdgePoint(@Nonnull CriteriaEdgePointContext ctx) {
        EdgePointAntlrCriteria edgePointAntlrCriteria = new EdgePointAntlrCriteria(
            ctx,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            this.criteriaOwner
        );

        KlassVisitor<AntlrExpressionValue> expressionValueVisitor =
            this.getExpressionValueVisitor(edgePointAntlrCriteria);
        AntlrMemberReferencePath memberExpressionValue =
            (AntlrMemberReferencePath) expressionValueVisitor.visitExpressionMemberReference(
                ctx.expressionMemberReference()
            );
        edgePointAntlrCriteria.setMemberExpressionValue(memberExpressionValue);
        return edgePointAntlrCriteria;
    }

    @Nonnull
    @Override
    public AntlrCriteria visitCriteriaExpressionAnd(@Nonnull CriteriaExpressionAndContext ctx) {
        AntlrAndCriteria andCriteria = new AntlrAndCriteria(
            ctx,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            this.criteriaOwner
        );

        KlassVisitor<AntlrCriteria> criteriaVisitor = new CriteriaVisitor(this.compilerState, andCriteria);

        AntlrCriteria left = criteriaVisitor.visit(ctx.left);
        AntlrCriteria right = criteriaVisitor.visit(ctx.right);

        andCriteria.setLeft(left);
        andCriteria.setRight(right);

        return andCriteria;
    }

    @Nonnull
    @Override
    public AntlrCriteria visitCriteriaNative(CriteriaNativeContext ctx) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitCriteriaNative() not implemented yet"
        );
    }

    @Nonnull
    @Override
    public AntlrCriteria visitCriteriaExpressionGroup(@Nonnull CriteriaExpressionGroupContext ctx) {
        CriteriaExpressionContext childContext = ctx.criteriaExpression();
        return childContext.accept(this);
    }

    @Nonnull
    @Override
    public AntlrCriteria visitCriteriaAll(@Nonnull CriteriaAllContext ctx) {
        return new AllAntlrCriteria(
            ctx,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            this.criteriaOwner
        );
    }

    @Nonnull
    @Override
    public OperatorAntlrCriteria visitCriteriaOperator(@Nonnull CriteriaOperatorContext ctx) {
        KlassVisitor<AntlrOperator> operatorVisitor = new OperatorVisitor(this.compilerState);
        AntlrOperator operator = operatorVisitor.visitOperator(ctx.operator());

        OperatorAntlrCriteria operatorAntlrCriteria = new OperatorAntlrCriteria(
            ctx,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            this.criteriaOwner,
            operator
        );

        // TODO: This is probably backwards
        operator.setOwningOperatorAntlrCriteria(operatorAntlrCriteria);

        KlassVisitor<AntlrExpressionValue> expressionValueVisitor =
            this.getExpressionValueVisitor(operatorAntlrCriteria);

        AntlrExpressionValue sourceValue = expressionValueVisitor.visitExpressionValue(ctx.source);
        AntlrExpressionValue targetValue = expressionValueVisitor.visitExpressionValue(ctx.target);

        operatorAntlrCriteria.setSourceValue(sourceValue);
        operatorAntlrCriteria.setTargetValue(targetValue);

        return operatorAntlrCriteria;
    }

    @Nonnull
    @Override
    public AntlrCriteria visitCriteriaExpressionOr(@Nonnull CriteriaExpressionOrContext ctx) {
        AntlrOrCriteria orCriteria = new AntlrOrCriteria(
            ctx,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            this.criteriaOwner
        );

        KlassVisitor<AntlrCriteria> criteriaVisitor = new CriteriaVisitor(this.compilerState, orCriteria);

        AntlrCriteria left = criteriaVisitor.visit(ctx.left);
        AntlrCriteria right = criteriaVisitor.visit(ctx.right);

        orCriteria.setLeft(left);
        orCriteria.setRight(right);

        return orCriteria;
    }

    @Nonnull
    @Override
    public AntlrCriteria visitLiteralList(LiteralListContext ctx) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitLiteralList() not implemented yet"
        );
    }

    @Nonnull
    @Override
    public AntlrCriteria visitNativeLiteral(NativeLiteralContext ctx) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitNativeLiteral() not implemented yet"
        );
    }

    @Nonnull
    @Override
    public AntlrCriteria visitEqualityOperator(EqualityOperatorContext ctx) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitEqualityOperator() not implemented yet"
        );
    }

    @Nonnull
    @Override
    public AntlrCriteria visitInequalityOperator(InequalityOperatorContext ctx) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitInequalityOperator() not implemented yet"
        );
    }

    @Nonnull
    @Override
    public AntlrCriteria visitInOperator(InOperatorContext ctx) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitInOperator() not implemented yet"
        );
    }

    @Nonnull
    @Override
    public AntlrCriteria visitStringOperator(StringOperatorContext ctx) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitStringOperator() not implemented yet"
        );
    }

    @Nonnull
    @Override
    public AntlrCriteria visitParameterReference(ParameterReferenceContext ctx) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitParameterReference() not implemented yet"
        );
    }

    @Nonnull
    @Override
    public AntlrCriteria visitTypeMemberReferencePath(TypeMemberReferencePathContext ctx) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitTypeMemberReferencePath() not implemented yet"
        );
    }

    @Nonnull
    @Override
    public AntlrCriteria visitLiteral(LiteralContext ctx) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitLiteral() not implemented yet"
        );
    }

    @Nonnull
    private ExpressionValueVisitor getExpressionValueVisitor(IAntlrElement expressionValueOwner) {
        return new ExpressionValueVisitor(
            this.compilerState,
            this.compilerState.getCompilerWalk().getThisReference(),
            expressionValueOwner
        );
    }

    @Nonnull
    @Override
    public AntlrCriteria visitTerminal(TerminalNode node) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitTerminal() not implemented yet"
        );
    }
}
