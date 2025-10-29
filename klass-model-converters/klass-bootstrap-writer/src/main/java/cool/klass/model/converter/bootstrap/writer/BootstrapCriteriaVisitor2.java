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

package cool.klass.model.converter.bootstrap.writer;

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.criteria.AllCriteria;
import cool.klass.model.meta.domain.api.criteria.AndCriteria;
import cool.klass.model.meta.domain.api.criteria.BinaryCriteria;
import cool.klass.model.meta.domain.api.criteria.Criteria;
import cool.klass.model.meta.domain.api.criteria.CriteriaVisitor;
import cool.klass.model.meta.domain.api.criteria.EdgePointCriteria;
import cool.klass.model.meta.domain.api.criteria.OperatorCriteria;
import cool.klass.model.meta.domain.api.criteria.OrCriteria;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.value.ExpressionValue;
import cool.klass.model.meta.domain.api.value.MemberReferencePath;
import klass.model.meta.domain.AllCriteriaList;
import klass.model.meta.domain.AndCriteriaList;
import klass.model.meta.domain.BinaryCriteriaList;
import klass.model.meta.domain.EdgePointCriteriaList;
import klass.model.meta.domain.OperatorCriteriaList;
import klass.model.meta.domain.OrCriteriaList;
import org.eclipse.collections.api.map.ImmutableMap;

public class BootstrapCriteriaVisitor2 implements CriteriaVisitor {

    private final ImmutableMap<Criteria, klass.model.meta.domain.Criteria> criteriaByCriteria;
    private final ImmutableMap<
        ExpressionValue,
        klass.model.meta.domain.ExpressionValue
    > expressionValuesByExpressionValue;

    private final AllCriteriaList allCriteria = new AllCriteriaList();
    private final EdgePointCriteriaList edgePointCriteria = new EdgePointCriteriaList();
    private final OperatorCriteriaList operatorCriteria = new OperatorCriteriaList();
    private final BinaryCriteriaList binaryCriteria = new BinaryCriteriaList();
    private final AndCriteriaList andCriteria = new AndCriteriaList();
    private final OrCriteriaList orCriteria = new OrCriteriaList();

    public BootstrapCriteriaVisitor2(
        ImmutableMap<Criteria, klass.model.meta.domain.Criteria> criteriaByCriteria,
        ImmutableMap<ExpressionValue, klass.model.meta.domain.ExpressionValue> expressionValuesByExpressionValue
    ) {
        this.criteriaByCriteria = Objects.requireNonNull(criteriaByCriteria);
        this.expressionValuesByExpressionValue = expressionValuesByExpressionValue;
    }

    public AllCriteriaList getAllCriteria() {
        return this.allCriteria;
    }

    public EdgePointCriteriaList getEdgePointCriteria() {
        return this.edgePointCriteria;
    }

    public OperatorCriteriaList getOperatorCriteria() {
        return this.operatorCriteria;
    }

    public BinaryCriteriaList getBinaryCriteria() {
        return this.binaryCriteria;
    }

    public AndCriteriaList getAndCriteria() {
        return this.andCriteria;
    }

    public OrCriteriaList getOrCriteria() {
        return this.orCriteria;
    }

    @Override
    public void visitAll(@Nonnull AllCriteria allCriteria) {
        var bootstrappedCriteria = this.criteriaByCriteria.get(allCriteria);

        var bootstrappedAllCriteria = new klass.model.meta.domain.AllCriteria();
        bootstrappedAllCriteria.setId(bootstrappedCriteria.getId());
        this.allCriteria.add(bootstrappedAllCriteria);
    }

    @Override
    public void visitOperator(@Nonnull OperatorCriteria operatorCriteria) {
        ExpressionValue sourceValue = operatorCriteria.getSourceValue();
        ExpressionValue targetValue = operatorCriteria.getTargetValue();

        var bootstrappedSourceValue = this.expressionValuesByExpressionValue.get(sourceValue);
        var bootstrappedTargetValue = this.expressionValuesByExpressionValue.get(targetValue);
        Objects.requireNonNull(bootstrappedSourceValue);
        Objects.requireNonNull(bootstrappedTargetValue);

        var bootstrappedCriteria = this.criteriaByCriteria.get(operatorCriteria);

        var bootstrappedOperatorCriteria = new klass.model.meta.domain.OperatorCriteria();
        bootstrappedOperatorCriteria.setId(bootstrappedCriteria.getId());
        bootstrappedOperatorCriteria.setOperator(operatorCriteria.getOperator().getOperatorText());
        long sourceValueId = bootstrappedSourceValue.getId();
        long targetValueId = bootstrappedTargetValue.getId();
        bootstrappedOperatorCriteria.setSourceExpressionId(sourceValueId);
        bootstrappedOperatorCriteria.setTargetExpressionId(targetValueId);
        this.operatorCriteria.add(bootstrappedOperatorCriteria);
    }

    @Override
    public void visitEdgePoint(@Nonnull EdgePointCriteria edgePointCriteria) {
        var bootstrappedExpressionValue = new klass.model.meta.domain.ExpressionValue();
        bootstrappedExpressionValue.insert();

        var bootstrappedMemberReferencePath = new klass.model.meta.domain.MemberReferencePath();
        bootstrappedMemberReferencePath.setId(bootstrappedExpressionValue.getId());

        MemberReferencePath memberExpressionValue = edgePointCriteria.getMemberExpressionValue();
        Klass klass = memberExpressionValue.getKlass();
        DataTypeProperty property = memberExpressionValue.getProperty();

        bootstrappedMemberReferencePath.setClassName(klass.getName());
        bootstrappedMemberReferencePath.setPropertyClassName(property.getOwningClassifier().getName());
        bootstrappedMemberReferencePath.setPropertyName(property.getName());
        bootstrappedMemberReferencePath.insert();

        var bootstrappedCriteria = this.criteriaByCriteria.get(edgePointCriteria);

        var bootstrappedEdgePointCriteria = new klass.model.meta.domain.EdgePointCriteria();
        bootstrappedEdgePointCriteria.setId(bootstrappedCriteria.getId());
        bootstrappedEdgePointCriteria.setMemberReferencePath(bootstrappedMemberReferencePath);
        this.edgePointCriteria.add(bootstrappedEdgePointCriteria);
    }

    @Override
    public void visitAnd(@Nonnull AndCriteria andCriteria) {
        var bootstrappedCriteria = this.handleBinaryCriteria(andCriteria);

        var bootstrappedAndCriteria = new klass.model.meta.domain.AndCriteria();
        bootstrappedAndCriteria.setId(bootstrappedCriteria.getId());
        this.andCriteria.add(bootstrappedAndCriteria);

        andCriteria.getLeft().visit(this);
        andCriteria.getRight().visit(this);
    }

    @Override
    public void visitOr(@Nonnull OrCriteria orCriteria) {
        var bootstrappedCriteria = this.handleBinaryCriteria(orCriteria);

        var bootstrappedOrCriteria = new klass.model.meta.domain.OrCriteria();
        bootstrappedOrCriteria.setId(bootstrappedCriteria.getId());
        this.orCriteria.add(bootstrappedOrCriteria);

        orCriteria.getLeft().visit(this);
        orCriteria.getRight().visit(this);
    }

    @Nonnull
    private klass.model.meta.domain.Criteria handleBinaryCriteria(@Nonnull BinaryCriteria binaryCriteria) {
        var bootstrappedCriteria = this.criteriaByCriteria.get(binaryCriteria);
        var bootstrappedLeftCriteria = this.criteriaByCriteria.get(binaryCriteria.getLeft());
        var bootstrappedRightCriteria = this.criteriaByCriteria.get(binaryCriteria.getRight());

        var bootstrappedBinaryCriteria = new klass.model.meta.domain.BinaryCriteria();
        bootstrappedBinaryCriteria.setId(bootstrappedCriteria.getId());
        bootstrappedBinaryCriteria.setLeftId(bootstrappedLeftCriteria.getId());
        bootstrappedBinaryCriteria.setRightId(bootstrappedRightCriteria.getId());
        this.binaryCriteria.add(bootstrappedBinaryCriteria);
        return bootstrappedCriteria;
    }
}
