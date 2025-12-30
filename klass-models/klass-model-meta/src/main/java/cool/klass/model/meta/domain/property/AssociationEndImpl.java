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

import cool.klass.model.meta.domain.AssociationImpl;
import cool.klass.model.meta.domain.AssociationImpl.AssociationBuilder;
import cool.klass.model.meta.domain.KlassImpl;
import cool.klass.model.meta.domain.KlassImpl.KlassBuilder;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.api.source.property.AssociationEndWithSourceCode;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;

// TODO: Super class for reference-type-property?
public final class AssociationEndImpl extends ReferencePropertyImpl<KlassImpl> implements AssociationEndWithSourceCode {

	@Nonnull
	private final AssociationImpl owningAssociation;

	private AssociationEndImpl(
		@Nonnull AssociationEndContext elementContext,
		@Nonnull Optional<Element> macroElement,
		@Nullable SourceCode sourceCode,
		int ordinal,
		@Nonnull IdentifierContext nameContext,
		@Nonnull KlassImpl type,
		@Nonnull KlassImpl owningClass,
		@Nonnull AssociationImpl owningAssociation,
		@Nonnull Multiplicity multiplicity
	) {
		super(elementContext, macroElement, sourceCode, ordinal, nameContext, type, owningClass, multiplicity);
		this.owningAssociation = Objects.requireNonNull(owningAssociation);
	}

	@Nonnull
	@Override
	public AssociationEndContext getElementContext() {
		return (AssociationEndContext) super.getElementContext();
	}

	@Nonnull
	@Override
	public Klass getOwningClassifier() {
		return (Klass) super.getOwningClassifier();
	}

	@Override
	@Nonnull
	public AssociationImpl getOwningAssociation() {
		return this.owningAssociation;
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

	public static final class AssociationEndBuilder
		extends ReferencePropertyBuilder<KlassImpl, KlassBuilder, AssociationEndImpl> {

		@Nonnull
		private final AssociationBuilder owningAssociation;

		public AssociationEndBuilder(
			@Nonnull AssociationEndContext elementContext,
			@Nonnull Optional<ElementBuilder<?>> macroElement,
			@Nullable SourceCodeBuilder sourceCode,
			int ordinal,
			@Nonnull IdentifierContext nameContext,
			@Nonnull KlassBuilder type,
			@Nonnull KlassBuilder owningClassBuilder,
			@Nonnull AssociationBuilder owningAssociation,
			@Nonnull Multiplicity multiplicity
		) {
			super(
				elementContext,
				macroElement,
				sourceCode,
				ordinal,
				nameContext,
				type,
				owningClassBuilder,
				multiplicity
			);
			this.owningAssociation = Objects.requireNonNull(owningAssociation);
		}

		@Override
		@Nonnull
		protected AssociationEndImpl buildUnsafe() {
			return new AssociationEndImpl(
				(AssociationEndContext) this.elementContext,
				this.macroElement.map(ElementBuilder::getElement),
				this.sourceCode.build(),
				this.ordinal,
				this.getNameContext(),
				this.typeBuilder.getElement(),
				(KlassImpl) this.owningClassifierBuilder.getElement(),
				this.owningAssociation.getElement(),
				this.multiplicity
			);
		}
	}
}
