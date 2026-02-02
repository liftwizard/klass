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

package cool.klass.model.converter.compiler.state.property;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.AntlrClassifierReference;
import cool.klass.model.converter.compiler.state.AntlrClassifierReferenceOwner;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.order.AntlrOrderBy;
import cool.klass.model.meta.domain.order.OrderByImpl.OrderByBuilder;
import cool.klass.model.meta.domain.property.AssociationEndSignatureImpl.AssociationEndSignatureBuilder;
import cool.klass.model.meta.domain.property.ModifierImpl.ModifierBuilder;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndSignatureContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.eclipse.collections.api.list.ImmutableList;

public class AntlrAssociationEndSignature
	extends AntlrReferenceProperty<AntlrClassifier>
	implements AntlrClassifierReferenceOwner {

	public static final AntlrAssociationEndSignature AMBIGUOUS = new AntlrAssociationEndSignature(
		new AssociationEndSignatureContext(AMBIGUOUS_PARENT, -1),
		Optional.empty(),
		-1,
		AMBIGUOUS_IDENTIFIER_CONTEXT,
		AntlrClassifier.AMBIGUOUS
	);

	@Nonnull
	private final AntlrClassifier owningClassifier;

	private AssociationEndSignatureBuilder associationEndSignatureBuilder;

	private AntlrClassifierReference classifierReference;

	public AntlrAssociationEndSignature(
		@Nonnull AssociationEndSignatureContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		int ordinal,
		@Nonnull IdentifierContext nameContext,
		@Nonnull AntlrClassifier owningClassifier
	) {
		super(elementContext, compilationUnit, ordinal, nameContext);
		this.owningClassifier = Objects.requireNonNull(owningClassifier);
	}

	@Nonnull
	@Override
	public Optional<IAntlrElement> getSurroundingElement() {
		return Optional.of(this.owningClassifier);
	}

	@Nonnull
	@Override
	public AssociationEndSignatureBuilder build() {
		if (this.associationEndSignatureBuilder != null) {
			throw new IllegalStateException();
		}

		// TODO: 🔗 Set association end's opposite
		this.associationEndSignatureBuilder = new AssociationEndSignatureBuilder(
			(AssociationEndSignatureContext) this.elementContext,
			this.getMacroElementBuilder(),
			this.getSourceCodeBuilder(),
			this.ordinal,
			this.getNameContext(),
			this.getType().getElementBuilder(),
			this.owningClassifier.getElementBuilder(),
			this.multiplicity.getMultiplicity()
		);

		ImmutableList<ModifierBuilder> modifierBuilders = this.getModifiers()
			.collect(AntlrModifier::build)
			.toImmutable();

		this.associationEndSignatureBuilder.setModifiers(modifierBuilders);

		Optional<OrderByBuilder> orderByBuilder = this.orderBy.map(AntlrOrderBy::build);
		this.associationEndSignatureBuilder.setOrderBy(orderByBuilder);

		return this.associationEndSignatureBuilder;
	}

	@Override
	public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		super.reportErrors(compilerAnnotationHolder);

		if (this.orderBy != null) {
			this.orderBy.ifPresent((o) -> o.reportErrors(compilerAnnotationHolder));
		}

		this.reportInvalidMultiplicity(compilerAnnotationHolder);
	}

	@Nonnull
	@Override
	public AntlrClassifier getOwningClassifier() {
		return Objects.requireNonNull(this.owningClassifier);
	}

	@Override
	@Nonnull
	public AssociationEndSignatureBuilder getElementBuilder() {
		return Objects.requireNonNull(this.associationEndSignatureBuilder);
	}

	@Override
	protected IdentifierContext getTypeIdentifier() {
		return this.getElementContext().classifierReference().identifier();
	}

	@Nonnull
	@Override
	public AssociationEndSignatureContext getElementContext() {
		return (AssociationEndSignatureContext) super.getElementContext();
	}

	@Nonnull
	@Override
	public AntlrClassifier getType() {
		return this.classifierReference.getClassifier();
	}

	@Override
	public void enterClassifierReference(@Nonnull AntlrClassifierReference classifierReference) {
		if (this.classifierReference != null) {
			throw new AssertionError();
		}
		this.classifierReference = Objects.requireNonNull(classifierReference);
	}
}
