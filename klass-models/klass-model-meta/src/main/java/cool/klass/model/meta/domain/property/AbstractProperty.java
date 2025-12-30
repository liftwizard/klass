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

package cool.klass.model.meta.domain.property;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.AbstractClassifier;
import cool.klass.model.meta.domain.AbstractClassifier.ClassifierBuilder;
import cool.klass.model.meta.domain.AbstractTypedElement;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.Type;
import cool.klass.model.meta.domain.api.Type.TypeGetter;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.antlr.v4.runtime.ParserRuleContext;

public abstract class AbstractProperty<T extends Type> extends AbstractTypedElement<T> implements Property {

	@Nonnull
	private final AbstractClassifier owningClassifier;

	protected AbstractProperty(
		@Nonnull ParserRuleContext elementContext,
		@Nonnull Optional<Element> macroElement,
		@Nullable SourceCode sourceCode,
		int ordinal,
		@Nonnull IdentifierContext nameContext,
		@Nonnull T type,
		@Nonnull AbstractClassifier owningClassifier
	) {
		super(elementContext, macroElement, sourceCode, ordinal, nameContext, type);
		this.owningClassifier = Objects.requireNonNull(owningClassifier);
	}

	@Override
	@Nonnull
	public Classifier getOwningClassifier() {
		return this.owningClassifier;
	}

	public abstract static class PropertyBuilder<
		T extends Type,
		TG extends TypeGetter,
		BuiltElement extends AbstractProperty<T>
	>
		extends TypedElementBuilder<T, TG, BuiltElement> {

		@Nonnull
		protected final ClassifierBuilder<?> owningClassifierBuilder;

		protected PropertyBuilder(
			@Nonnull ParserRuleContext elementContext,
			@Nonnull Optional<ElementBuilder<?>> macroElement,
			@Nullable SourceCodeBuilder sourceCode,
			int ordinal,
			@Nonnull IdentifierContext nameContext,
			@Nonnull TG typeBuilder,
			@Nonnull ClassifierBuilder<?> owningClassifierBuilder
		) {
			super(elementContext, macroElement, sourceCode, ordinal, nameContext, typeBuilder);
			this.owningClassifierBuilder = Objects.requireNonNull(owningClassifierBuilder);
		}
	}
}
