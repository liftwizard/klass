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

package cool.klass.model.converter.compiler.state.property.validation;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.meta.domain.property.validation.MinLengthPropertyValidationImpl.MinLengthPropertyValidationBuilder;
import cool.klass.model.meta.grammar.KlassParser.MinLengthValidationContext;
import cool.klass.model.meta.grammar.KlassParser.MinLengthValidationKeywordContext;

public class AntlrMinLengthPropertyValidation extends AbstractAntlrNumericPropertyValidation {

	private MinLengthPropertyValidationBuilder elementBuilder;

	public AntlrMinLengthPropertyValidation(
		@Nonnull MinLengthValidationContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		@Nonnull AntlrDataTypeProperty<?> owningProperty,
		int number
	) {
		super(elementContext, compilationUnit, owningProperty, number);
	}

	@Override
	public MinLengthPropertyValidationBuilder build() {
		if (this.elementBuilder != null) {
			throw new IllegalStateException();
		}
		this.elementBuilder = new MinLengthPropertyValidationBuilder(
			(MinLengthValidationContext) this.elementContext,
			this.getMacroElementBuilder(),
			this.getSourceCodeBuilder(),
			this.owningProperty.getElementBuilder(),
			this.number
		);
		return this.elementBuilder;
	}

	@Nonnull
	@Override
	public MinLengthPropertyValidationBuilder getElementBuilder() {
		return Objects.requireNonNull(this.elementBuilder);
	}

	@Nonnull
	@Override
	public MinLengthValidationContext getElementContext() {
		return (MinLengthValidationContext) super.getElementContext();
	}

	@Override
	public MinLengthValidationKeywordContext getKeywordToken() {
		return this.getElementContext().minLengthValidationKeyword();
	}
}
