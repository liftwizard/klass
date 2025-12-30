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

package cool.klass.generator.service;

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.criteria.AllCriteria;
import cool.klass.model.meta.domain.api.criteria.AndCriteria;
import cool.klass.model.meta.domain.api.criteria.CriteriaVisitor;
import cool.klass.model.meta.domain.api.criteria.EdgePointCriteria;
import cool.klass.model.meta.domain.api.criteria.OperatorCriteria;
import cool.klass.model.meta.domain.api.criteria.OrCriteria;
import cool.klass.model.meta.domain.api.operator.Operator;
import cool.klass.model.meta.domain.api.value.ExpressionValue;
import cool.klass.model.meta.domain.api.value.MemberReferencePath;
import cool.klass.model.meta.domain.api.value.literal.NullLiteral;

public class OperationCriteriaVisitor implements CriteriaVisitor {

	private final String finderName;
	private final StringBuilder stringBuilder;

	public OperationCriteriaVisitor(@Nonnull String finderName, @Nonnull StringBuilder stringBuilder) {
		this.finderName = Objects.requireNonNull(finderName);
		this.stringBuilder = Objects.requireNonNull(stringBuilder);
	}

	@Override
	public void visitAll(@Nonnull AllCriteria allCriteria) {
		this.stringBuilder.append(this.finderName).append(".all()");
	}

	@Override
	public void visitAnd(@Nonnull AndCriteria andCriteria) {
		andCriteria.getLeft().visit(this);
		this.stringBuilder.append("\n                .and(");
		andCriteria.getRight().visit(this);
		this.stringBuilder.append(")");
	}

	@Override
	public void visitOr(@Nonnull OrCriteria orCriteria) {
		orCriteria.getLeft().visit(this);
		this.stringBuilder.append("\n                .or(");
		orCriteria.getRight().visit(this);
		this.stringBuilder.append(")");
	}

	@Override
	public void visitOperator(@Nonnull OperatorCriteria operatorCriteria) {
		ExpressionValue sourceValue = operatorCriteria.getSourceValue();
		Operator operator = operatorCriteria.getOperator();
		ExpressionValue targetValue = operatorCriteria.getTargetValue();

		sourceValue.visit(new OperationExpressionValueVisitor(this.finderName, this.stringBuilder));

		if (targetValue instanceof NullLiteral) {
			String operatorText = operator.getOperatorText();
			switch (operatorText) {
				case "==" -> this.stringBuilder.append(".isNull()");
				case "!=" -> this.stringBuilder.append(".isNotNull()");
				default -> throw new AssertionError("Unexpected operator with null literal: " + operatorText);
			}
		} else {
			operator.visit(new OperationOperatorVisitor(this.stringBuilder));
			targetValue.visit(new OperationExpressionValueVisitor(this.finderName, this.stringBuilder));
			this.stringBuilder.append(")");
		}
	}

	@Override
	public void visitEdgePoint(@Nonnull EdgePointCriteria edgePointCriteria) {
		MemberReferencePath memberExpressionValue = edgePointCriteria.getMemberExpressionValue();
		memberExpressionValue.visit(new OperationExpressionValueVisitor(this.finderName, this.stringBuilder));
		this.stringBuilder.append(".equalsEdgePoint()");
	}
}
