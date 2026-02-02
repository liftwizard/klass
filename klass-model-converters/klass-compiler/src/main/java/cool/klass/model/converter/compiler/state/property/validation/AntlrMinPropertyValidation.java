/*
 * Copyright 2026 Craig Motlin
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

package cool.klass.model.converter.compiler.state.property.validation;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.meta.domain.property.validation.MinPropertyValidationImpl.MinPropertyValidationBuilder;
import cool.klass.model.meta.grammar.KlassParser.MinValidationContext;
import cool.klass.model.meta.grammar.KlassParser.MinValidationKeywordContext;

public class AntlrMinPropertyValidation extends AbstractAntlrNumericPropertyValidation {

	private MinPropertyValidationBuilder elementBuilder;

	public AntlrMinPropertyValidation(
		@Nonnull MinValidationContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		@Nonnull AntlrDataTypeProperty<?> owningProperty,
		int number
	) {
		super(elementContext, compilationUnit, owningProperty, number);
	}

	@Override
	public MinPropertyValidationBuilder build() {
		if (this.elementBuilder != null) {
			throw new IllegalStateException();
		}
		this.elementBuilder = new MinPropertyValidationBuilder(
			(MinValidationContext) this.elementContext,
			this.getMacroElementBuilder(),
			this.getSourceCodeBuilder(),
			this.owningProperty.getElementBuilder(),
			this.number
		);
		return this.elementBuilder;
	}

	@Nonnull
	@Override
	public MinPropertyValidationBuilder getElementBuilder() {
		return Objects.requireNonNull(this.elementBuilder);
	}

	@Nonnull
	@Override
	public MinValidationContext getElementContext() {
		return (MinValidationContext) super.getElementContext();
	}

	@Override
	public MinValidationKeywordContext getKeywordToken() {
		return this.getElementContext().minValidationKeyword();
	}
}
