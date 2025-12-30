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

package cool.klass.model.meta.domain.value.literal;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.KlassImpl.KlassBuilder;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.source.KlassWithSourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.api.value.literal.UserLiteral;
import cool.klass.model.meta.grammar.KlassParser.NativeLiteralContext;

public final class UserLiteralImpl extends AbstractLiteralValue implements UserLiteral {

	@Nonnull
	private final KlassWithSourceCode userClass;

	private UserLiteralImpl(
		@Nonnull NativeLiteralContext elementContext,
		@Nonnull Optional<Element> macroElement,
		@Nullable SourceCode sourceCode,
		@Nonnull KlassWithSourceCode userClass
	) {
		super(elementContext, macroElement, sourceCode);
		this.userClass = Objects.requireNonNull(userClass);
	}

	@Nonnull
	@Override
	public NativeLiteralContext getElementContext() {
		return (NativeLiteralContext) super.getElementContext();
	}

	@Nonnull
	@Override
	public KlassWithSourceCode getUserClass() {
		return this.userClass;
	}

	public static final class UserLiteralBuilder extends AbstractLiteralValueBuilder<UserLiteralImpl> {

		private final KlassBuilder userClassBuilder;

		public UserLiteralBuilder(
			@Nonnull NativeLiteralContext elementContext,
			@Nonnull Optional<ElementBuilder<?>> macroElement,
			@Nullable SourceCodeBuilder sourceCode,
			@Nonnull KlassBuilder userClassBuilder
		) {
			super(elementContext, macroElement, sourceCode);
			this.userClassBuilder = Objects.requireNonNull(userClassBuilder);
		}

		@Override
		@Nonnull
		protected UserLiteralImpl buildUnsafe() {
			return new UserLiteralImpl(
				(NativeLiteralContext) this.elementContext,
				this.macroElement.map(ElementBuilder::getElement),
				this.sourceCode.build(),
				this.userClassBuilder.getElement()
			);
		}
	}
}
