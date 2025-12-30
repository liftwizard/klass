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

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.phase.criteria.CriteriaVisitor;
import cool.klass.model.converter.compiler.state.criteria.AntlrCriteria;
import cool.klass.model.converter.compiler.state.service.AntlrServiceCriteria;
import cool.klass.model.meta.grammar.KlassParser.CriteriaExpressionContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceCriteriaDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceCriteriaKeywordContext;

public class ServiceCriteriaPhase extends AbstractCompilerPhase {

	public ServiceCriteriaPhase(@Nonnull CompilerState compilerState) {
		super(compilerState);
	}

	@Override
	public void enterServiceCriteriaDeclaration(@Nonnull ServiceCriteriaDeclarationContext ctx) {
		super.enterServiceCriteriaDeclaration(ctx);

		ServiceCriteriaKeywordContext serviceCriteriaKeywordContext = ctx.serviceCriteriaKeyword();

		String serviceCriteriaKeyword = serviceCriteriaKeywordContext.getText();

		AntlrServiceCriteria serviceCriteria = new AntlrServiceCriteria(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			serviceCriteriaKeyword,
			this.compilerState.getCompilerWalk().getService()
		);

		CriteriaExpressionContext criteriaExpressionContext = ctx.criteriaExpression();

		CriteriaVisitor criteriaVisitor = new CriteriaVisitor(this.compilerState, serviceCriteria);

		AntlrCriteria antlrCriteria = criteriaVisitor.visit(criteriaExpressionContext);
		serviceCriteria.setCriteria(antlrCriteria);

		this.compilerState.getCompilerWalk().getService().enterServiceCriteriaDeclaration(serviceCriteria);
	}
}
