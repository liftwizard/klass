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

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.AbstractClassifier;
import cool.klass.model.meta.domain.AbstractClassifier.ClassifierBuilder;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.api.source.property.AssociationEndSignatureWithSourceCode;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndSignatureContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;

// TODO: Super class for reference-type-property?
public final class AssociationEndSignatureImpl
	extends ReferencePropertyImpl<AbstractClassifier>
	implements AssociationEndSignatureWithSourceCode {

	private AssociationEndSignatureImpl(
		@Nonnull AssociationEndSignatureContext elementContext,
		@Nonnull Optional<Element> macroElement,
		@Nullable SourceCode sourceCode,
		int ordinal,
		@Nonnull IdentifierContext nameContext,
		@Nonnull AbstractClassifier type,
		@Nonnull AbstractClassifier owningClassifier,
		@Nonnull Multiplicity multiplicity
	) {
		super(elementContext, macroElement, sourceCode, ordinal, nameContext, type, owningClassifier, multiplicity);
	}

	@Nonnull
	@Override
	public AssociationEndSignatureContext getElementContext() {
		return (AssociationEndSignatureContext) super.getElementContext();
	}

	@Override
	public String toString() {
		return String.format(
			"%s.%s: %s[%s]",
			this.getOwningClassifier().getName(),
			this.getName(),
			this.getType().getName(),
			this.multiplicity.getPrettyName()
		);
	}

	public static final class AssociationEndSignatureBuilder
		extends ReferencePropertyBuilder<AbstractClassifier, ClassifierBuilder<?>, AssociationEndSignatureImpl> {

		public AssociationEndSignatureBuilder(
			@Nonnull AssociationEndSignatureContext elementContext,
			@Nonnull Optional<ElementBuilder<?>> macroElement,
			@Nullable SourceCodeBuilder sourceCode,
			int ordinal,
			@Nonnull IdentifierContext nameContext,
			@Nonnull ClassifierBuilder<?> type,
			@Nonnull ClassifierBuilder<?> owningClassifierBuilder,
			@Nonnull Multiplicity multiplicity
		) {
			super(
				elementContext,
				macroElement,
				sourceCode,
				ordinal,
				nameContext,
				type,
				owningClassifierBuilder,
				multiplicity
			);
		}

		@Override
		@Nonnull
		protected AssociationEndSignatureImpl buildUnsafe() {
			return new AssociationEndSignatureImpl(
				(AssociationEndSignatureContext) this.elementContext,
				this.macroElement.map(ElementBuilder::getElement),
				this.sourceCode.build(),
				this.ordinal,
				this.getNameContext(),
				this.typeBuilder.getElement(),
				this.owningClassifierBuilder.getElement(),
				this.multiplicity
			);
		}
	}
}
