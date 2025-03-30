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
import cool.klass.model.meta.domain.api.parameter.Parameter;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.value.ExpressionValue;
import cool.klass.model.meta.domain.api.value.MemberReferencePath;
import org.eclipse.collections.api.map.ImmutableMap;

public class BootstrapCriteriaVisitor implements CriteriaVisitor {

    private final ImmutableMap<Parameter, klass.model.meta.domain.Parameter> bootstrappedParametersByParameter;
    private final ImmutableMap<ExpressionValue, klass.model.meta.domain.ExpressionValue> bootstrappedExpressionValues;

    private klass.model.meta.domain.Criteria bootstrappedCriteria;

    public BootstrapCriteriaVisitor(
        ImmutableMap<Parameter, klass.model.meta.domain.Parameter> bootstrappedParametersByParameter,
        ImmutableMap<ExpressionValue, klass.model.meta.domain.ExpressionValue> bootstrappedExpressionValues
    ) {
        this.bootstrappedParametersByParameter = Objects.requireNonNull(bootstrappedParametersByParameter);
        this.bootstrappedExpressionValues = Objects.requireNonNull(bootstrappedExpressionValues);
    }

    public static klass.model.meta.domain.Criteria convert(
        ImmutableMap<Parameter, klass.model.meta.domain.Parameter> bootstrappedParametersByParameter,
        ImmutableMap<ExpressionValue, klass.model.meta.domain.ExpressionValue> bootstrappedExpressionValues,
        @Nonnull Criteria criteria
    ) {
        var visitor = new BootstrapCriteriaVisitor(bootstrappedParametersByParameter, bootstrappedExpressionValues);
        criteria.visit(visitor);
        return visitor.getResult();
    }

    public klass.model.meta.domain.Criteria getResult() {
        return Objects.requireNonNull(this.bootstrappedCriteria);
    }

    @Override
    public void visitAll(@Nonnull AllCriteria allCriteria) {
        var bootstrappedCriteria = new klass.model.meta.domain.Criteria();

        var bootstrappedAllCriteria = new klass.model.meta.domain.AllCriteria();
        bootstrappedAllCriteria.setId(bootstrappedCriteria.getId());
        this.bootstrappedCriteria = bootstrappedCriteria;
    }

    @Override
    public void visitAnd(@Nonnull AndCriteria andCriteria) {
        var bootstrappedBinaryCriteria = this.handleBinaryCriteria(andCriteria);

        var bootstrappedAndCriteria = new klass.model.meta.domain.AndCriteria();
        bootstrappedAndCriteria.setId(bootstrappedBinaryCriteria.getId());
    }

    @Override
    public void visitOr(@Nonnull OrCriteria orCriteria) {
        var bootstrappedBinaryCriteria = this.handleBinaryCriteria(orCriteria);

        var bootstrappedAndCriteria = new klass.model.meta.domain.OrCriteria();
        bootstrappedAndCriteria.setId(bootstrappedBinaryCriteria.getId());
    }

    @Nonnull
    private klass.model.meta.domain.BinaryCriteria handleBinaryCriteria(@Nonnull BinaryCriteria binaryCriteria) {
        var bootstrappedLeft = BootstrapCriteriaVisitor.convert(
            this.bootstrappedParametersByParameter,
            this.bootstrappedExpressionValues,
            binaryCriteria.getLeft()
        );
        var bootstrappedRight = BootstrapCriteriaVisitor.convert(
            this.bootstrappedParametersByParameter,
            this.bootstrappedExpressionValues,
            binaryCriteria.getRight()
        );

        var bootstrappedCriteria = new klass.model.meta.domain.Criteria();
        bootstrappedCriteria.insert();
        this.bootstrappedCriteria = bootstrappedCriteria;

        var bootstrappedBinaryCriteria = new klass.model.meta.domain.BinaryCriteria();
        bootstrappedBinaryCriteria.setId(bootstrappedCriteria.getId());
        bootstrappedBinaryCriteria.setLeft(bootstrappedLeft);
        bootstrappedBinaryCriteria.setRight(bootstrappedRight);
        return bootstrappedBinaryCriteria;
    }

    @Override
    public void visitOperator(@Nonnull OperatorCriteria operatorCriteria) {
        ExpressionValue sourceValue = operatorCriteria.getSourceValue();
        ExpressionValue targetValue = operatorCriteria.getTargetValue();

        var bootstrappedSourceValue = this.bootstrappedExpressionValues.get(sourceValue);
        var bootstrappedTargetValue = this.bootstrappedExpressionValues.get(targetValue);

        var bootstrappedCriteria = new klass.model.meta.domain.Criteria();
        bootstrappedCriteria.insert();

        var bootstrappedOperatorCriteria = new klass.model.meta.domain.OperatorCriteria();
        bootstrappedOperatorCriteria.setId(bootstrappedCriteria.getId());
        bootstrappedOperatorCriteria.setOperator(operatorCriteria.getOperator().getOperatorText());
        long sourceValueId = bootstrappedSourceValue.getId();
        long targetValueId = bootstrappedTargetValue.getId();
        bootstrappedOperatorCriteria.setSourceExpressionId(sourceValueId);
        bootstrappedOperatorCriteria.setSourceExpressionId(targetValueId);

        this.bootstrappedCriteria = bootstrappedCriteria;
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

        var bootstrappedCriteria = new klass.model.meta.domain.Criteria();
        bootstrappedCriteria.insert();

        var bootstrappedEdgePointCriteria = new klass.model.meta.domain.EdgePointCriteria();
        bootstrappedEdgePointCriteria.setId(bootstrappedCriteria.getId());
        bootstrappedEdgePointCriteria.setMemberReferencePath(bootstrappedMemberReferencePath);

        this.bootstrappedCriteria = bootstrappedCriteria;
    }
}
