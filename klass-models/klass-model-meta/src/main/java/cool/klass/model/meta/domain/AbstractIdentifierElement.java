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

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.antlr.v4.runtime.ParserRuleContext;

public abstract class AbstractIdentifierElement extends AbstractNamedElement {

	protected AbstractIdentifierElement(
		@Nonnull ParserRuleContext elementContext,
		@Nonnull Optional<Element> macroElement,
		@Nullable SourceCode sourceCode,
		int ordinal,
		@Nonnull IdentifierContext nameContext
	) {
		super(elementContext, macroElement, sourceCode, ordinal, nameContext);
	}

	@Nonnull
	@Override
	public IdentifierContext getNameContext() {
		return (IdentifierContext) super.getNameContext();
	}

	public abstract static class IdentifierElementBuilder<BuiltElement extends AbstractIdentifierElement>
		extends NamedElementBuilder<BuiltElement> {

		protected IdentifierElementBuilder(
			@Nonnull ParserRuleContext elementContext,
			@Nonnull Optional<ElementBuilder<?>> macroElement,
			@Nullable SourceCodeBuilder sourceCode,
			int ordinal,
			@Nonnull IdentifierContext nameContext
		) {
			super(elementContext, macroElement, sourceCode, ordinal, nameContext);
		}

		@Nonnull
		@Override
		public IdentifierContext getNameContext() {
			return (IdentifierContext) super.getNameContext();
		}
	}
}
