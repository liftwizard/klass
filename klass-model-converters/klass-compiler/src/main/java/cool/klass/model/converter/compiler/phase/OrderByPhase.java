/*
 * Copyright 2025 Craig Motlin
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

package cool.klass.model.converter.compiler.phase;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.phase.criteria.ExpressionValueVisitor;
import cool.klass.model.converter.compiler.state.order.AntlrOrderBy;
import cool.klass.model.converter.compiler.state.order.AntlrOrderByMemberReferencePath;
import cool.klass.model.converter.compiler.state.order.AntlrOrderByOwner;
import cool.klass.model.converter.compiler.state.value.AntlrThisMemberReferencePath;
import cool.klass.model.meta.grammar.KlassParser.OrderByDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.OrderByMemberReferencePathContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceOrderByDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ThisMemberReferencePathContext;
import org.antlr.v4.runtime.ParserRuleContext;

public class OrderByPhase extends AbstractCompilerPhase {

    @Nullable
    private AntlrOrderBy orderBy;

    private ServiceOrderByDeclarationContext serviceOrderByDeclarationContext;

    public OrderByPhase(@Nonnull CompilerState compilerState) {
        super(compilerState);
    }

    @Override
    public void enterServiceOrderByDeclaration(@Nonnull ServiceOrderByDeclarationContext ctx) {
        super.enterServiceOrderByDeclaration(ctx);
        this.serviceOrderByDeclarationContext = ctx;
    }

    @Override
    public void exitServiceOrderByDeclaration(@Nonnull ServiceOrderByDeclarationContext ctx) {
        this.serviceOrderByDeclarationContext = null;
        super.exitServiceOrderByDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterOrderByDeclaration(@Nonnull OrderByDeclarationContext ctx) {
        super.enterOrderByDeclaration(ctx);

        if (this.compilerState.getCompilerWalk().getOrderByOwner() == null) {
            return;
        }

        ParserRuleContext orderByContext = this.serviceOrderByDeclarationContext == null
            ? ctx
            : this.serviceOrderByDeclarationContext;
        this.orderBy = new AntlrOrderBy(
            orderByContext,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            this.compilerState.getCompilerWalk().getThisReference(),
            this.compilerState.getCompilerWalk().getOrderByOwner()
        );
        AntlrOrderByOwner orderByOwner = this.compilerState.getCompilerWalk().getOrderByOwner();
        orderByOwner.enterOrderByDeclaration(this.orderBy);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitOrderByDeclaration(@Nonnull OrderByDeclarationContext ctx) {
        this.orderBy = null;
        super.exitOrderByDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterOrderByMemberReferencePath(@Nonnull OrderByMemberReferencePathContext ctx) {
        super.enterOrderByMemberReferencePath(ctx);
        if (this.compilerState.getCompilerWalk().getOrderByOwner() == null) {
            return;
        }

        AntlrOrderByMemberReferencePath orderByMemberReferencePath = this.convertOrderByMemberReferencePath(ctx);
        this.orderBy.enterOrderByMemberReferencePath(orderByMemberReferencePath);
    }

    @Nonnull
    private AntlrOrderByMemberReferencePath convertOrderByMemberReferencePath(
        @Nonnull OrderByMemberReferencePathContext orderByMemberReferencePathContext
    ) {
        AntlrThisMemberReferencePath thisMemberReferencePath = this.getAntlrThisMemberReferencePath(
            orderByMemberReferencePathContext
        );

        return new AntlrOrderByMemberReferencePath(
            orderByMemberReferencePathContext,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            this.orderBy,
            this.orderBy.getNumProperties(),
            thisMemberReferencePath
        );
    }

    @Nonnull
    private AntlrThisMemberReferencePath getAntlrThisMemberReferencePath(
        @Nonnull OrderByMemberReferencePathContext orderByMemberReferencePathContext
    ) {
        ExpressionValueVisitor expressionValueVisitor = new ExpressionValueVisitor(
            this.compilerState,
            this.compilerState.getCompilerWalk().getThisReference(),
            this.orderBy
        );

        ThisMemberReferencePathContext thisMemberReferencePathContext =
            orderByMemberReferencePathContext.thisMemberReferencePath();

        return expressionValueVisitor.visitThisMemberReferencePath(thisMemberReferencePathContext);
    }
}
