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

package cool.klass.model.meta.domain;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.source.InterfaceWithSourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceDeclarationContext;

public final class InterfaceImpl extends AbstractClassifier implements InterfaceWithSourceCode {

	private InterfaceImpl(
		@Nonnull InterfaceDeclarationContext elementContext,
		@Nonnull Optional<Element> macroElement,
		@Nullable SourceCode sourceCode,
		int ordinal,
		@Nonnull IdentifierContext nameContext,
		@Nonnull String packageName
	) {
		super(elementContext, macroElement, sourceCode, ordinal, nameContext, packageName);
	}

	@Nonnull
	@Override
	public InterfaceDeclarationContext getElementContext() {
		return (InterfaceDeclarationContext) super.getElementContext();
	}

	public static final class InterfaceBuilder extends ClassifierBuilder<InterfaceImpl> {

		public InterfaceBuilder(
			@Nonnull InterfaceDeclarationContext elementContext,
			@Nonnull Optional<ElementBuilder<?>> macroElement,
			@Nullable SourceCodeBuilder sourceCode,
			int ordinal,
			@Nonnull IdentifierContext nameContext,
			@Nonnull String packageName
		) {
			super(elementContext, macroElement, sourceCode, ordinal, nameContext, packageName);
		}

		@Override
		@Nonnull
		protected InterfaceImpl buildUnsafe() {
			return new InterfaceImpl(
				(InterfaceDeclarationContext) this.elementContext,
				this.macroElement.map(ElementBuilder::getElement),
				this.sourceCode.build(),
				this.ordinal,
				this.getNameContext(),
				this.packageName
			);
		}

		@Override
		public InterfaceImpl getType() {
			return Objects.requireNonNull(this.element);
		}
	}
}
