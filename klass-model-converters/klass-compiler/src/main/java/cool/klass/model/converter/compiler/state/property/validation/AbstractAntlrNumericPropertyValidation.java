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
import cool.klass.model.meta.domain.property.validation.AbstractNumericPropertyValidation.NumericPropertyValidationBuilder;
import org.antlr.v4.runtime.ParserRuleContext;

public abstract class AbstractAntlrNumericPropertyValidation extends AbstractAntlrPropertyValidation {

	protected final int number;

	protected AbstractAntlrNumericPropertyValidation(
		@Nonnull ParserRuleContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		@Nonnull AntlrDataTypeProperty<?> owningProperty,
		int number
	) {
		super(elementContext, compilationUnit, owningProperty);
		this.number = number;
	}

	@Override
	public abstract NumericPropertyValidationBuilder build();

	@Nonnull
	@Override
	public abstract NumericPropertyValidationBuilder getElementBuilder();
}
