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
import cool.klass.model.converter.compiler.state.AntlrCompilationUnit;
import cool.klass.model.converter.compiler.state.AntlrInterface;
import cool.klass.model.converter.compiler.state.AntlrTopLevelElement;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.meta.domain.projection.AbstractProjectionElement.ProjectionChildBuilder;
import cool.klass.model.meta.domain.projection.ProjectionImpl.ProjectionBuilder;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionDeclarationContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

public class AntlrProjection extends AntlrProjectionParent implements AntlrTopLevelElement {

	// <editor-fold desc="AMBIGUOUS">
	public static final AntlrProjection AMBIGUOUS = new AntlrProjection(
		new ProjectionDeclarationContext(AMBIGUOUS_PARENT, -1),
		Optional.empty(),
		-1,
		AMBIGUOUS_IDENTIFIER_CONTEXT,
		AntlrCompilationUnit.AMBIGUOUS,
		AntlrClassifier.AMBIGUOUS,
		"ambiguous"
	) {
		@Override
		public String toString() {
			return AntlrProjection.class + ".AMBIGUOUS";
		}
	};
	// </editor-fold>

	// <editor-fold desc="NOT_FOUND">
	public static final AntlrProjection NOT_FOUND = new AntlrProjection(
		new ProjectionDeclarationContext(NOT_FOUND_PARENT, -1),
		Optional.empty(),
		-1,
		NOT_FOUND_IDENTIFIER_CONTEXT,
		AntlrCompilationUnit.NOT_FOUND,
		AntlrClassifier.NOT_FOUND,
		"not found"
	) {
		@Override
		public String toString() {
			return AntlrProjection.class + ".NOT_FOUND";
		}
	};

	// </editor-fold>

	@Nonnull
	private final AntlrCompilationUnit compilationUnitState;

	private final String packageName;

	private ProjectionBuilder projectionBuilder;

	public AntlrProjection(
		@Nonnull ProjectionDeclarationContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		int ordinal,
		@Nonnull IdentifierContext nameContext,
		@Nonnull AntlrCompilationUnit compilationUnitState,
		@Nonnull AntlrClassifier classifier,
		String packageName
	) {
		super(elementContext, compilationUnit, ordinal, nameContext, classifier);
		this.compilationUnitState = Objects.requireNonNull(compilationUnitState);
		this.packageName = Objects.requireNonNull(packageName);
	}

	@Nonnull
	@Override
	public Optional<IAntlrElement> getSurroundingElement() {
		return Optional.of(this.compilationUnitState);
	}

	@Override
	public Pair<Token, Token> getContextBefore() {
		return Tuples.pair(this.getElementContext().getStart(), this.getElementContext().projectionBlock().getStart());
	}

	@Nonnull
	@Override
	public ProjectionDeclarationContext getElementContext() {
		return (ProjectionDeclarationContext) super.getElementContext();
	}

	@Override
	protected Pattern getNamePattern() {
		return TYPE_NAME_PATTERN;
	}

	public ProjectionBuilder build() {
		if (this.projectionBuilder != null) {
			throw new IllegalStateException();
		}

		this.projectionBuilder = new ProjectionBuilder(
			(ProjectionDeclarationContext) this.elementContext,
			this.getMacroElementBuilder(),
			this.getSourceCodeBuilder(),
			this.ordinal,
			this.getNameContext(),
			this.packageName,
			this.classifier.getElementBuilder()
		);

		ImmutableList<ProjectionChildBuilder> children = this.children.collect(
			AntlrProjectionChild::build
		).toImmutable();

		this.projectionBuilder.setChildBuilders(children);
		return this.projectionBuilder;
	}

	public void build2() {
		this.children.each(AntlrProjectionElement::build2);
	}

	@Nonnull
	@Override
	public ProjectionBuilder getElementBuilder() {
		return this.projectionBuilder;
	}

	// <editor-fold desc="Report Compiler Errors">
	@Override
	public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		super.reportErrors(compilerAnnotationHolder);

		// TODO: Move not-found and ambiguous error checking from compiler phase here for consistency
		if (
			this.classifier == AntlrClassifier.NOT_FOUND
			|| this.classifier == AntlrClass.NOT_FOUND
			|| this.classifier == AntlrInterface.NOT_FOUND
		) {
			String message = "Projection type not found " + this.getElementContext().classifierReference().getText();
			compilerAnnotationHolder.add("ERR_PRJ_NFD", message, this, this.getElementContext().classifierReference());
			return;
		}

		if (
			this.classifier == AntlrClassifier.AMBIGUOUS
			|| this.classifier == AntlrClass.AMBIGUOUS
			|| this.classifier == AntlrInterface.AMBIGUOUS
		) {
			return;
		}

		this.reportForwardReference(compilerAnnotationHolder);

		for (AntlrProjectionChild child : this.children) {
			child.reportErrors(compilerAnnotationHolder);
		}
	}

	protected void reportForwardReference(CompilerAnnotationHolder compilerAnnotationHolder) {
		if (this.isForwardReference(this.classifier)) {
			String message = String.format(
				"Projection '%s' is declared on line %d and has a forward reference to classifier '%s' which is declared later in the source file '%s' on line %d.",
				this.getName(),
				this.getElementContext().getStart().getLine(),
				this.classifier.getName(),
				this.getCompilationUnit().get().getSourceName(),
				this.classifier.getElementContext().getStart().getLine()
			);
			compilerAnnotationHolder.add("ERR_FWD_REF", message, this, this.getElementContext().classifierReference());
		}
	}
	// </editor-fold>
}
