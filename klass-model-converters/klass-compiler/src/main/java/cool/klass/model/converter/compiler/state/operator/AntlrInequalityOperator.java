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

package cool.klass.model.converter.compiler.state.operator;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrType;
import cool.klass.model.meta.domain.operator.InequalityOperatorImpl.InequalityOperatorBuilder;
import cool.klass.model.meta.grammar.KlassParser.CriteriaOperatorContext;
import cool.klass.model.meta.grammar.KlassParser.InequalityOperatorContext;
import org.eclipse.collections.api.list.ListIterable;

public class AntlrInequalityOperator extends AntlrOperator {

	private InequalityOperatorBuilder elementBuilder;

	public AntlrInequalityOperator(
		@Nonnull InequalityOperatorContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		@Nonnull String operatorText
	) {
		super(elementContext, compilationUnit, operatorText);
	}

	@Nonnull
	@Override
	public InequalityOperatorBuilder build() {
		if (this.elementBuilder != null) {
			throw new IllegalStateException();
		}
		this.elementBuilder = new InequalityOperatorBuilder(
			(InequalityOperatorContext) this.elementContext,
			this.getMacroElementBuilder(),
			this.getSourceCodeBuilder(),
			this.operatorText
		);
		return this.elementBuilder;
	}

	@Nonnull
	@Override
	public InequalityOperatorBuilder getElementBuilder() {
		return Objects.requireNonNull(this.elementBuilder);
	}

	@Override
	public void checkTypes(
		@Nonnull CompilerAnnotationHolder compilerAnnotationHolder,
		@Nonnull ListIterable<AntlrType> sourceTypes,
		@Nonnull ListIterable<AntlrType> targetTypes
	) {
		if (sourceTypes.isEmpty() || targetTypes.isEmpty()) {
			return;
		}

		if (sourceTypes.equals(targetTypes)) {
			return;
		}

		if (sourceTypes.size() == 1 && targetTypes.contains(sourceTypes.getOnly())) {
			return;
		}

		String message = String.format(
			"Incompatible types: '%s' and '%s'.",
			sourceTypes.getFirst(),
			targetTypes.getFirst()
		);
		// Cast is a deliberate assertion
		CriteriaOperatorContext criteriaOperatorContext =
			(CriteriaOperatorContext) this.elementContext.getParent().getParent();
		compilerAnnotationHolder.add("ERR_OPR_NEQ", message, this, criteriaOperatorContext);
	}
}
