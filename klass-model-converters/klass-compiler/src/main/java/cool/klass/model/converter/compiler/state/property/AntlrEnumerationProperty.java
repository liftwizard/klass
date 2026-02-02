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
import cool.klass.model.converter.compiler.state.AntlrEnumeration;
import cool.klass.model.meta.domain.EnumerationImpl;
import cool.klass.model.meta.domain.property.EnumerationPropertyImpl.EnumerationPropertyBuilder;
import cool.klass.model.meta.domain.property.ModifierImpl.ModifierBuilder;
import cool.klass.model.meta.grammar.KlassParser.EnumerationPropertyContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;

public class AntlrEnumerationProperty extends AntlrDataTypeProperty<EnumerationImpl> {

	// <editor-fold desc="AMBIGUOUS">
	public static final AntlrEnumerationProperty AMBIGUOUS = new AntlrEnumerationProperty(
		new EnumerationPropertyContext(AMBIGUOUS_PARENT, -1),
		Optional.empty(),
		-1,
		AMBIGUOUS_IDENTIFIER_CONTEXT,
		AntlrClassifier.AMBIGUOUS,
		false,
		AntlrEnumeration.AMBIGUOUS
	) {
		@Override
		public String toString() {
			return "AntlrEnumerationProperty.AMBIGUOUS";
		}
	};
	// </editor-fold>

	// <editor-fold desc="NOT_FOUND">
	public static final AntlrEnumerationProperty NOT_FOUND = new AntlrEnumerationProperty(
		new EnumerationPropertyContext(NOT_FOUND_PARENT, -1),
		Optional.empty(),
		-1,
		NOT_FOUND_IDENTIFIER_CONTEXT,
		AntlrClassifier.NOT_FOUND,
		false,
		AntlrEnumeration.NOT_FOUND
	) {
		@Override
		public String toString() {
			return "AntlrEnumerationProperty.NOT_FOUND";
		}
	};

	// </editor-fold>

	// TODO: Check that it's not NOT_FOUND
	@Nonnull
	private final AntlrEnumeration enumeration;

	private EnumerationPropertyBuilder elementBuilder;

	public AntlrEnumerationProperty(
		@Nonnull EnumerationPropertyContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		int ordinal,
		@Nonnull IdentifierContext nameContext,
		@Nonnull AntlrClassifier owningClassifier,
		boolean isOptional,
		@Nonnull AntlrEnumeration enumeration
	) {
		super(elementContext, compilationUnit, ordinal, nameContext, owningClassifier, isOptional);
		this.enumeration = Objects.requireNonNull(enumeration);
	}

	@Nonnull
	@Override
	public AntlrEnumeration getType() {
		return this.enumeration;
	}

	@Override
	protected ParserRuleContext getTypeParserRuleContext() {
		return this.getElementContext().enumerationReference();
	}

	@Nonnull
	@Override
	public EnumerationPropertyBuilder build() {
		if (this.elementBuilder != null) {
			throw new IllegalStateException();
		}

		this.elementBuilder = new EnumerationPropertyBuilder(
			(EnumerationPropertyContext) this.elementContext,
			this.getMacroElementBuilder(),
			this.getSourceCodeBuilder(),
			this.ordinal,
			this.getNameContext(),
			this.enumeration.getElementBuilder(),
			this.owningClassifier.getElementBuilder(),
			this.isOptional
		);

		ImmutableList<ModifierBuilder> modifierBuilders = this.getModifiers()
			.collect(AntlrModifier::build)
			.toImmutable();
		this.elementBuilder.setModifierBuilders(modifierBuilders);

		this.buildValidations();

		return this.elementBuilder;
	}

	@Nonnull
	@Override
	public EnumerationPropertyBuilder getElementBuilder() {
		return Objects.requireNonNull(this.elementBuilder);
	}

	@Override
	public String getTypeName() {
		return this.getElementContext().enumerationReference().getText();
	}

	// <editor-fold desc="Report Compiler Errors">
	@Override
	public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		super.reportErrors(compilerAnnotationHolder);

		this.reportTypeNotFound(compilerAnnotationHolder);
		this.reportForwardReference(compilerAnnotationHolder);
	}

	private void reportTypeNotFound(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		if (this.enumeration != AntlrEnumeration.NOT_FOUND) {
			return;
		}

		EnumerationReferenceContext offendingToken = this.getElementContext().enumerationReference();
		String message = String.format("Cannot find enumeration '%s'.", offendingToken.getText());
		compilerAnnotationHolder.add("ERR_ENM_PRP", message, this, offendingToken);
	}

	private void reportForwardReference(CompilerAnnotationHolder compilerAnnotationHolder) {
		if (!this.isForwardReference(this.enumeration)) {
			return;
		}

		String message = String.format(
			"Enumeration property '%s' is declared on line %d and has a forward reference to enumeration '%s' which is declared later in the source file '%s' on line %d.",
			this,
			this.getElementContext().getStart().getLine(),
			this.enumeration.getName(),
			this.getCompilationUnit().get().getSourceName(),
			this.enumeration.getElementContext().getStart().getLine()
		);
		compilerAnnotationHolder.add("ERR_FWD_REF", message, this, this.getElementContext().enumerationReference());
	}

	@Override
	protected void reportInvalidIdProperties(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		ListIterable<AntlrModifier> idModifiers = this.getModifiersByName("id");
		for (AntlrModifier idModifier : idModifiers) {
			String message = "Enumeration properties may not be auto-generated ids.";
			compilerAnnotationHolder.add("ERR_ENM_IDP", message, this, idModifier.getElementContext());
		}
	}

	// </editor-fold>

	@Nonnull
	@Override
	public EnumerationPropertyContext getElementContext() {
		return (EnumerationPropertyContext) super.getElementContext();
	}
}
