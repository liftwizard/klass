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

package cool.klass.model.meta.domain.property;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.AbstractOrdinalElement;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.modifier.Modifier;
import cool.klass.model.meta.domain.api.modifier.ModifierOwner;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public final class ModifierImpl extends AbstractOrdinalElement implements Modifier {

	@Nonnull
	private final ModifierOwner modifierOwner;

	public ModifierImpl(
		@Nonnull ParserRuleContext elementContext,
		@Nonnull Optional<Element> macroElement,
		@Nullable SourceCode sourceCode,
		int ordinal,
		@Nonnull ModifierOwner modifierOwner
	) {
		super(elementContext, macroElement, sourceCode, ordinal);
		this.modifierOwner = Objects.requireNonNull(modifierOwner);
	}

	public Token getKeywordToken() {
		ParserRuleContext elementContext = this.getElementContext();
		int childCount = elementContext.getChildCount();
		if (childCount != 1) {
			throw new AssertionError();
		}
		return elementContext.getStart();
	}

	@Override
	public String getKeyword() {
		return this.getKeywordToken().getText();
	}

	@Override
	public ModifierOwner getModifierOwner() {
		return this.modifierOwner;
	}

	@Override
	public String toString() {
		return this.getKeyword();
	}

	public static final class ModifierBuilder extends OrdinalElementBuilder<ModifierImpl> {

		@Nonnull
		private final ElementBuilder<?> surroundingElementBuilder;

		public ModifierBuilder(
			@Nonnull ParserRuleContext elementContext,
			@Nonnull Optional<ElementBuilder<?>> macroElement,
			@Nullable SourceCodeBuilder sourceCode,
			int ordinal,
			@Nonnull ElementBuilder<?> surroundingElementBuilder
		) {
			super(elementContext, macroElement, sourceCode, ordinal);
			this.surroundingElementBuilder = Objects.requireNonNull(surroundingElementBuilder);
		}

		@Override
		@Nonnull
		protected ModifierImpl buildUnsafe() {
			return new ModifierImpl(
				this.elementContext,
				this.macroElement.map(ElementBuilder::getElement),
				this.sourceCode.build(),
				this.ordinal,
				(ModifierOwner) this.surroundingElementBuilder.getElement()
			);
		}
	}
}
