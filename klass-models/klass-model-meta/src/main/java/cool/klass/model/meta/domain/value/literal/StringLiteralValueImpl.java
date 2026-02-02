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

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.api.value.literal.StringLiteralValue;
import cool.klass.model.meta.grammar.KlassParser.StringLiteralContext;

public final class StringLiteralValueImpl extends AbstractLiteralValue implements StringLiteralValue {

	@Nonnull
	private final String value;

	private StringLiteralValueImpl(
		@Nonnull StringLiteralContext elementContext,
		@Nonnull Optional<Element> macroElement,
		@Nullable SourceCode sourceCode,
		@Nonnull String value
	) {
		super(elementContext, macroElement, sourceCode);
		this.value = Objects.requireNonNull(value);
	}

	@Nonnull
	@Override
	public StringLiteralContext getElementContext() {
		return (StringLiteralContext) super.getElementContext();
	}

	@Override
	@Nonnull
	public String getValue() {
		return this.value;
	}

	public static final class StringLiteralValueBuilder extends AbstractLiteralValueBuilder<StringLiteralValueImpl> {

		@Nonnull
		private final String value;

		public StringLiteralValueBuilder(
			@Nonnull StringLiteralContext elementContext,
			@Nonnull Optional<ElementBuilder<?>> macroElement,
			@Nullable SourceCodeBuilder sourceCode,
			@Nonnull String value
		) {
			super(elementContext, macroElement, sourceCode);
			this.value = Objects.requireNonNull(value);
		}

		@Override
		@Nonnull
		protected StringLiteralValueImpl buildUnsafe() {
			return new StringLiteralValueImpl(
				(StringLiteralContext) this.elementContext,
				this.macroElement.map(ElementBuilder::getElement),
				this.sourceCode.build(),
				this.value
			);
		}
	}
}
