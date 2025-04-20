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

package cool.klass.model.converter.compiler.phase;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrDomainModel;
import cool.klass.model.meta.grammar.KlassBaseListener;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ClassReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaAllContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaEdgePointContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaExpressionOrContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaNativeContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaOperatorContext;
import cool.klass.model.meta.grammar.KlassParser.ExpressionValueContext;
import cool.klass.model.meta.grammar.KlassParser.LiteralContext;
import cool.klass.model.meta.grammar.KlassParser.TypeMemberReferencePathContext;

public class PossibleJoinCriteriaListener extends KlassBaseListener {

    @Nonnull
    private final AntlrDomainModel domainModel;

    @Nonnull
    private final AntlrClass targetType;

    private boolean allEqualityOperators = true;
    private boolean allOperatorsCrossTypes = true;
    private boolean allMemberReferencesAreDirect = true;
    private boolean allTypeMembersMatch = true;
    private boolean allReferencesResolve = true;

    public PossibleJoinCriteriaListener(@Nonnull AntlrDomainModel domainModel, @Nonnull AntlrClass targetType) {
        this.domainModel = Objects.requireNonNull(domainModel);
        this.targetType = Objects.requireNonNull(targetType);
    }

    public boolean hasForeignKeys() {
        return this.allEqualityOperators &&
            this.allOperatorsCrossTypes &&
            this.allMemberReferencesAreDirect &&
            this.allTypeMembersMatch &&
            this.allReferencesResolve;
    }

    @Override
    public void enterCriteriaEdgePoint(@Nonnull CriteriaEdgePointContext ctx) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".enterCriteriaEdgePoint() not implemented yet"
        );
    }

    @Override
    public void enterCriteriaNative(@Nonnull CriteriaNativeContext ctx) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".enterCriteriaNative() not implemented yet"
        );
    }

    @Override
    public void enterCriteriaAll(@Nonnull CriteriaAllContext ctx) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".enterCriteriaAll() not implemented yet"
        );
    }

    @Override
    public void enterCriteriaOperator(@Nonnull CriteriaOperatorContext ctx) {
        super.enterCriteriaOperator(ctx);

        if (ctx.operator().equalityOperator() == null) {
            this.allEqualityOperators = false;
        }
        ExpressionValueContext source = ctx.source;
        ExpressionValueContext target = ctx.target;
        boolean noThisReference = source.thisMemberReferencePath() == null && target.thisMemberReferencePath() == null;
        boolean noTypeReference = source.typeMemberReferencePath() == null && target.typeMemberReferencePath() == null;
        if (noThisReference || noTypeReference) {
            this.allOperatorsCrossTypes = false;
        }
        // TODO: check time ranges against time instants
    }

    @Override
    public void enterCriteriaExpressionOr(@Nonnull CriteriaExpressionOrContext ctx) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".enterCriteriaExpressionOr() not implemented yet"
        );
    }

    @Override
    public void enterTypeMemberReferencePath(@Nonnull TypeMemberReferencePathContext ctx) {
        super.enterTypeMemberReferencePath(ctx);

        ClassReferenceContext classReferenceContext = ctx.classReference();
        List<AssociationEndReferenceContext> associationEndReferenceContexts = ctx.associationEndReference();

        AntlrClass klass = this.domainModel.getClassByName(classReferenceContext.identifier().getText());

        if (!associationEndReferenceContexts.isEmpty()) {
            this.allMemberReferencesAreDirect = false;
        } else if (klass != this.targetType) {
            this.allTypeMembersMatch = false;
        } else if (klass == AntlrClass.NOT_FOUND || klass == AntlrClass.AMBIGUOUS) {
            this.allReferencesResolve = false;
        }
    }

    @Override
    public void enterLiteral(@Nonnull LiteralContext ctx) {
        super.enterLiteral(ctx);
        // TODO: Not sure if this should count. But the example is:
        // this.key == Comment.blueprintKey
        //         && Comment.replyToId == null
    }
}
