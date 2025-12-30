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

package cool.klass.model.meta.domain.service.url;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.AbstractOrdinalElement;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.grammar.KlassParser.UrlConstantContext;

public final class UrlConstantImpl extends AbstractOrdinalElement {

	@Nonnull
	private final String name;

	private UrlConstantImpl(
		@Nonnull UrlConstantContext elementContext,
		@Nonnull Optional<Element> macroElement,
		@Nullable SourceCode sourceCode,
		int ordinal,
		@Nonnull String name
	) {
		super(elementContext, macroElement, sourceCode, ordinal);
		this.name = Objects.requireNonNull(name);
	}

	@Nonnull
	@Override
	public UrlConstantContext getElementContext() {
		return (UrlConstantContext) super.getElementContext();
	}

	@Nonnull
	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return this.name;
	}

	public static final class UrlConstantBuilder extends OrdinalElementBuilder<UrlConstantImpl> {

		@Nonnull
		private final String name;

		public UrlConstantBuilder(
			@Nonnull UrlConstantContext elementContext,
			@Nonnull Optional<ElementBuilder<?>> macroElement,
			@Nullable SourceCodeBuilder sourceCode,
			int ordinal,
			@Nonnull String name
		) {
			super(elementContext, macroElement, sourceCode, ordinal);
			this.name = Objects.requireNonNull(name);
		}

		@Override
		@Nonnull
		protected UrlConstantImpl buildUnsafe() {
			return new UrlConstantImpl(
				(UrlConstantContext) this.elementContext,
				this.macroElement.map(ElementBuilder::getElement),
				this.sourceCode.build(),
				this.ordinal,
				this.name
			);
		}
	}
}
