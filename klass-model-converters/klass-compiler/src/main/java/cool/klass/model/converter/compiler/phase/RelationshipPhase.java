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

import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.phase.criteria.CriteriaVisitor;
import cool.klass.model.converter.compiler.state.AntlrAssociation;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrRelationship;
import cool.klass.model.converter.compiler.state.criteria.AntlrCriteria;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEnd;
import cool.klass.model.meta.grammar.KlassParser.CriteriaExpressionContext;
import cool.klass.model.meta.grammar.KlassParser.RelationshipContext;
import cool.klass.model.meta.grammar.KlassVisitor;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.collections.api.list.MutableList;

public class RelationshipPhase extends AbstractCompilerPhase {

    public RelationshipPhase(@Nonnull CompilerState compilerState) {
        super(compilerState);
    }

    @Override
    public void enterRelationship(@Nonnull RelationshipContext ctx) {
        super.enterRelationship(ctx);

        AntlrAssociation association = this.compilerState.getCompilerWalk().getAssociation();

        AntlrRelationship relationship = new AntlrRelationship(
            ctx,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            association
        );
        association.setRelationship(relationship);

        KlassVisitor<AntlrCriteria> visitor = new CriteriaVisitor(this.compilerState, relationship);

        CriteriaExpressionContext criteriaExpressionContext = ctx.criteriaExpression();
        AntlrCriteria criteria = visitor.visit(criteriaExpressionContext);
        relationship.setCriteria(criteria);

        MutableList<AntlrAssociationEnd> associationEnds = association.getAssociationEnds();
        if (associationEnds.size() != 2) {
            return;
        }

        if (association.isManyToMany()) {
            return;
        }

        boolean possibleJoinCriteria =
            this.hasPossibleJoinCriteria(criteriaExpressionContext, association.getTargetEnd().getType());

        if (possibleJoinCriteria) {
            criteria.addForeignKeys();
        }
    }

    private boolean hasPossibleJoinCriteria(
        @Nonnull CriteriaExpressionContext criteriaExpressionContext,
        @Nonnull AntlrClass targetType
    ) {
        PossibleJoinCriteriaListener listener = new PossibleJoinCriteriaListener(
            this.compilerState.getDomainModel(),
            targetType
        );
        ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
        parseTreeWalker.walk(listener, criteriaExpressionContext);
        return listener.hasForeignKeys();
    }
}
