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

package cool.klass.model.meta.domain.reference;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.criteria.AllCriteria;
import cool.klass.model.meta.domain.api.criteria.AndCriteria;
import cool.klass.model.meta.domain.api.criteria.CriteriaVisitor;
import cool.klass.model.meta.domain.api.criteria.EdgePointCriteria;
import cool.klass.model.meta.domain.api.criteria.OperatorCriteria;
import cool.klass.model.meta.domain.api.criteria.OrCriteria;
import cool.klass.model.meta.domain.api.value.ExpressionValue;
import cool.klass.model.meta.domain.api.value.MemberReferencePath;

public class DomainModelReferencesCriteriaVisitor implements CriteriaVisitor {

    private final DomainModelReferences domainModelReferences;

    public DomainModelReferencesCriteriaVisitor(DomainModelReferences domainModelReferences) {
        this.domainModelReferences = domainModelReferences;
    }

    @Override
    public void visitAll(@Nonnull AllCriteria allCriteria) {
        // Deliberately empty
    }

    @Override
    public void visitAnd(@Nonnull AndCriteria andCriteria) {
        andCriteria.getLeft().visit(this);
        andCriteria.getRight().visit(this);
    }

    @Override
    public void visitOr(@Nonnull OrCriteria orCriteria) {
        orCriteria.getLeft().visit(this);
        orCriteria.getRight().visit(this);
    }

    @Override
    public void visitOperator(@Nonnull OperatorCriteria operatorCriteria) {
        ExpressionValue sourceValue = operatorCriteria.getSourceValue();
        ExpressionValue targetValue = operatorCriteria.getTargetValue();
        sourceValue.visit(new DomainModelReferencesExpressionValueVisitor(this.domainModelReferences));
        targetValue.visit(new DomainModelReferencesExpressionValueVisitor(this.domainModelReferences));
    }

    @Override
    public void visitEdgePoint(@Nonnull EdgePointCriteria edgePointCriteria) {
        MemberReferencePath memberExpressionValue = edgePointCriteria.getMemberExpressionValue();
        memberExpressionValue.visit(new DomainModelReferencesExpressionValueVisitor(this.domainModelReferences));
    }
}
