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

package cool.klass.model.converter.compiler.state;

import java.util.Set;

import cool.klass.model.converter.compiler.state.criteria.AllAntlrCriteria;
import cool.klass.model.converter.compiler.state.criteria.AntlrAndCriteria;
import cool.klass.model.converter.compiler.state.criteria.AntlrCriteriaVisitor;
import cool.klass.model.converter.compiler.state.criteria.AntlrOrCriteria;
import cool.klass.model.converter.compiler.state.criteria.EdgePointAntlrCriteria;
import cool.klass.model.converter.compiler.state.criteria.OperatorAntlrCriteria;
import cool.klass.model.converter.compiler.state.parameter.AntlrParameter;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEnd;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.value.AntlrExpressionValue;
import cool.klass.model.converter.compiler.state.value.AntlrMemberReferencePath;

public class ReferencedPropertiesCriteriaVisitor implements AntlrCriteriaVisitor {

	private final ReferencedPropertiesExpressionValueVisitor expressionValueVisitor =
		new ReferencedPropertiesExpressionValueVisitor();

	public Set<AntlrAssociationEnd> getAssociationEndsReferencedByCriteria() {
		return this.expressionValueVisitor.getAssociationEndsReferencedByCriteria();
	}

	public Set<AntlrDataTypeProperty<?>> getDataTypePropertiesReferencedByCriteria() {
		return this.expressionValueVisitor.getDataTypePropertiesReferencedByCriteria();
	}

	public Set<AntlrParameter> getParametersReferencedByCriteria() {
		return this.expressionValueVisitor.getParametersReferencedByCriteria();
	}

	@Override
	public void visitEdgePoint(EdgePointAntlrCriteria edgePointCriteria) {
		AntlrMemberReferencePath memberExpressionValue = edgePointCriteria.getMemberExpressionValue();
		memberExpressionValue.visit(this.expressionValueVisitor);
	}

	@Override
	public void visitOperator(OperatorAntlrCriteria operatorCriteria) {
		AntlrExpressionValue sourceValue = operatorCriteria.getSourceValue();
		AntlrExpressionValue targetValue = operatorCriteria.getTargetValue();
		sourceValue.visit(this.expressionValueVisitor);
		targetValue.visit(this.expressionValueVisitor);
	}

	@Override
	public void visitAll(AllAntlrCriteria allCriteria) {}

	@Override
	public void visitAnd(AntlrAndCriteria andCriteria) {}

	@Override
	public void visitOr(AntlrOrCriteria orCriteria) {}
}
