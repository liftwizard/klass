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

package cool.klass.model.converter.compiler.state.projection;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.AntlrIdentifierElement;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrEnumerationProperty;
import cool.klass.model.converter.compiler.state.property.AntlrReferenceProperty;
import cool.klass.model.meta.domain.projection.ProjectionDataTypePropertyImpl.ProjectionDataTypePropertyBuilder;
import cool.klass.model.meta.grammar.KlassParser.HeaderContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionPrimitiveMemberContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.tuple.Pair;

public class AntlrProjectionDataTypeProperty extends AntlrIdentifierElement implements AntlrProjectionChild {

	@Nonnull
	private final HeaderContext headerContext;

	@Nonnull
	private final String headerText;

	@Nonnull
	private final AntlrProjectionParent antlrProjectionParent;

	@Nonnull
	private final AntlrClassifier classifier;

	@Nonnull
	private final AntlrDataTypeProperty<?> dataTypeProperty;

	private ProjectionDataTypePropertyBuilder projectionDataTypePropertyBuilder;

	public AntlrProjectionDataTypeProperty(
		@Nonnull ProjectionPrimitiveMemberContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		int ordinal,
		@Nonnull IdentifierContext nameContext,
		@Nonnull HeaderContext headerContext,
		@Nonnull String headerText,
		@Nonnull AntlrProjectionParent antlrProjectionParent,
		@Nonnull AntlrClassifier classifier,
		@Nonnull AntlrDataTypeProperty<?> dataTypeProperty
	) {
		super(elementContext, compilationUnit, ordinal, nameContext);
		this.antlrProjectionParent = Objects.requireNonNull(antlrProjectionParent);
		this.headerText = Objects.requireNonNull(headerText);
		this.headerContext = Objects.requireNonNull(headerContext);
		this.classifier = Objects.requireNonNull(classifier);
		this.dataTypeProperty = Objects.requireNonNull(dataTypeProperty);
	}

	@Nonnull
	public AntlrDataTypeProperty<?> getProperty() {
		return this.dataTypeProperty;
	}

	@Nonnull
	@Override
	public AntlrClassifier getDeclaringClassifier() {
		return this.classifier;
	}

	@Nonnull
	@Override
	public ProjectionDataTypePropertyBuilder build() {
		if (this.projectionDataTypePropertyBuilder != null) {
			throw new IllegalStateException();
		}
		this.projectionDataTypePropertyBuilder = new ProjectionDataTypePropertyBuilder(
			(ProjectionPrimitiveMemberContext) this.elementContext,
			this.getMacroElementBuilder(),
			this.getSourceCodeBuilder(),
			this.ordinal,
			this.getNameContext(),
			this.headerContext,
			this.headerText,
			this.antlrProjectionParent.getElementBuilder(),
			this.classifier.getElementBuilder(),
			this.dataTypeProperty.getElementBuilder()
		);
		return this.projectionDataTypePropertyBuilder;
	}

	@Override
	public void build2() {
		// Deliberately empty
	}

	@Override
	public void visit(@Nonnull AntlrProjectionVisitor visitor) {
		visitor.visitDataTypeProperty(this);
	}

	@Override
	@Nonnull
	public ProjectionPrimitiveMemberContext getElementContext() {
		return (ProjectionPrimitiveMemberContext) this.elementContext;
	}

	@Override
	@Nonnull
	public ProjectionDataTypePropertyBuilder getElementBuilder() {
		return Objects.requireNonNull(this.projectionDataTypePropertyBuilder);
	}

	@Override
	public boolean isContext() {
		return true;
	}

	@Override
	public Pair<Token, Token> getContextBefore() {
		return this.getEntireContext();
	}

	@Nonnull
	@Override
	public AntlrProjectionParent getParent() {
		return this.antlrProjectionParent;
	}

	// <editor-fold desc="Report Compiler Errors">
	@Override
	public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		if (this.headerText.trim().isEmpty()) {
			compilerAnnotationHolder.add("ERR_PRJ_HDR", "Empty header string.", this, this.headerContext);
		}

		AntlrClassifier parentClassifier = this.antlrProjectionParent.getClassifier();
		if (
			parentClassifier == AntlrClass.NOT_FOUND
			|| parentClassifier == AntlrClass.AMBIGUOUS
			|| parentClassifier == AntlrClassifier.AMBIGUOUS
			|| parentClassifier == AntlrClassifier.NOT_FOUND
		) {
			return;
		}

		if (this.dataTypeProperty == AntlrEnumerationProperty.NOT_FOUND) {
			AntlrReferenceProperty<?> referenceProperty = parentClassifier.getReferencePropertyByName(this.getName());
			if (referenceProperty == AntlrReferenceProperty.NOT_FOUND) {
				String message = String.format(
					"Cannot find member '%s.%s'.",
					parentClassifier.getName(),
					this.getName()
				);
				compilerAnnotationHolder.add("ERR_PRJ_DTP", message, this);
			} else {
				String message =
					"Leaf projection nodes require a data type property, but found a reference property '%s.%s' with type '%s'".formatted(
						parentClassifier.getName(),
						referenceProperty.getName(),
						referenceProperty.getTypeName()
					);
				compilerAnnotationHolder.add("ERR_PRJ_TYP", message, this);
			}
		}

		this.reportPrivateProperty(compilerAnnotationHolder);
		this.reportForwardReference(compilerAnnotationHolder);
	}

	private void reportPrivateProperty(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		if (this.dataTypeProperty.isPrivate()) {
			String message = String.format(
				"Projection includes private property '%s.%s'.",
				this.dataTypeProperty.getOwningClassifier().getName(),
				this.getName()
			);
			compilerAnnotationHolder.add("ERR_PRJ_PRV", message, this);
		}
	}

	private void reportForwardReference(CompilerAnnotationHolder compilerAnnotationHolder) {
		if (!this.isForwardReference(this.dataTypeProperty)) {
			return;
		}

		String message = String.format(
			"Projection property '%s' is declared on line %d and has a forward reference to property '%s' which is declared later in the source file '%s' on line %d.",
			this.getName(),
			this.getElementContext().getStart().getLine(),
			this.dataTypeProperty,
			this.getCompilationUnit().get().getSourceName(),
			this.dataTypeProperty.getElementContext().getStart().getLine()
		);
		compilerAnnotationHolder.add("ERR_FWD_REF", message, this, this.getElementContext().identifier());
	}

	@Override
	public void reportDuplicateMemberName(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		String message = String.format("Duplicate member: '%s'.", this.getName());
		compilerAnnotationHolder.add("ERR_DUP_PRJ", message, this);
	}

	@Override
	public void reportNameErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		// Intentionally blank. Reference to a named element that gets its name checked.
	}

	// </editor-fold>

	@Nonnull
	@Override
	protected Pattern getNamePattern() {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".getNamePattern() not implemented yet"
		);
	}
}
