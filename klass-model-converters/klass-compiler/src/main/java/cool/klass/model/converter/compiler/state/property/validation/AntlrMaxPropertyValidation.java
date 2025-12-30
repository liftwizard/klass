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

import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.meta.domain.property.validation.MaxPropertyValidationImpl.MaxPropertyValidationBuilder;
import cool.klass.model.meta.grammar.KlassParser.MaxValidationContext;
import cool.klass.model.meta.grammar.KlassParser.MaxValidationKeywordContext;

public class AntlrMaxPropertyValidation extends AbstractAntlrNumericPropertyValidation {

	private MaxPropertyValidationBuilder elementBuilder;

	public AntlrMaxPropertyValidation(
		@Nonnull MaxValidationContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		@Nonnull AntlrDataTypeProperty<?> owningProperty,
		int number
	) {
		super(elementContext, compilationUnit, owningProperty, number);
	}

	@Override
	public MaxPropertyValidationBuilder build() {
		if (this.elementBuilder != null) {
			throw new IllegalStateException();
		}
		this.elementBuilder = new MaxPropertyValidationBuilder(
			(MaxValidationContext) this.elementContext,
			this.getMacroElementBuilder(),
			this.getSourceCodeBuilder(),
			this.owningProperty.getElementBuilder(),
			this.number
		);
		return this.elementBuilder;
	}

	@Nonnull
	@Override
	public MaxPropertyValidationBuilder getElementBuilder() {
		return this.elementBuilder;
	}

	@Nonnull
	@Override
	public MaxValidationContext getElementContext() {
		return (MaxValidationContext) super.getElementContext();
	}

	@Override
	public MaxValidationKeywordContext getKeywordToken() {
		return this.getElementContext().maxValidationKeyword();
	}
}
