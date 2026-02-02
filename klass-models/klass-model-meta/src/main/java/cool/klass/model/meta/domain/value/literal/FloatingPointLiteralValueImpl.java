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

package cool.klass.model.meta.domain.value.literal;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.api.value.literal.FloatingPointLiteralValue;
import cool.klass.model.meta.grammar.KlassParser.FloatingPointLiteralContext;

public final class FloatingPointLiteralValueImpl extends AbstractLiteralValue implements FloatingPointLiteralValue {

	private final double value;

	private FloatingPointLiteralValueImpl(
		@Nonnull FloatingPointLiteralContext elementContext,
		@Nonnull Optional<Element> macroElement,
		@Nullable SourceCode sourceCode,
		double value
	) {
		super(elementContext, macroElement, sourceCode);
		this.value = value;
	}

	@Nonnull
	@Override
	public FloatingPointLiteralContext getElementContext() {
		return (FloatingPointLiteralContext) super.getElementContext();
	}

	@Override
	public double getValue() {
		return this.value;
	}

	public static final class FloatingPointLiteralValueBuilder
		extends AbstractLiteralValueBuilder<FloatingPointLiteralValueImpl> {

		private final double value;

		public FloatingPointLiteralValueBuilder(
			@Nonnull FloatingPointLiteralContext elementContext,
			@Nonnull Optional<ElementBuilder<?>> macroElement,
			@Nullable SourceCodeBuilder sourceCode,
			double value
		) {
			super(elementContext, macroElement, sourceCode);
			this.value = value;
		}

		@Override
		@Nonnull
		protected FloatingPointLiteralValueImpl buildUnsafe() {
			return new FloatingPointLiteralValueImpl(
				(FloatingPointLiteralContext) this.elementContext,
				this.macroElement.map(ElementBuilder::getElement),
				this.sourceCode.build(),
				this.value
			);
		}
	}
}
