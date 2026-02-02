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

package cool.klass.model.meta.domain.projection;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.AbstractClassifier;
import cool.klass.model.meta.domain.AbstractClassifier.ClassifierBuilder;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.projection.ProjectionParent;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.api.source.projection.ProjectionWithSourceCode;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionDeclarationContext;

public final class ProjectionImpl extends AbstractProjectionParent implements ProjectionWithSourceCode {

	@Nonnull
	private final String packageName;

	@Nonnull
	private final AbstractClassifier classifier;

	private ProjectionImpl(
		@Nonnull ProjectionDeclarationContext elementContext,
		@Nonnull Optional<Element> macroElement,
		@Nullable SourceCode sourceCode,
		int ordinal,
		@Nonnull IdentifierContext nameContext,
		@Nonnull String packageName,
		@Nonnull AbstractClassifier classifier
	) {
		super(elementContext, macroElement, sourceCode, ordinal, nameContext);
		this.packageName = Objects.requireNonNull(packageName);
		this.classifier = Objects.requireNonNull(classifier);
	}

	@Nonnull
	@Override
	public ProjectionDeclarationContext getElementContext() {
		return (ProjectionDeclarationContext) super.getElementContext();
	}

	@Override
	public Optional<ProjectionParent> getParent() {
		return Optional.empty();
	}

	@Override
	@Nonnull
	public AbstractClassifier getClassifier() {
		return this.classifier;
	}

	@Nonnull
	@Override
	public String getPackageName() {
		return this.packageName;
	}

	public static final class ProjectionBuilder
		extends AbstractProjectionParentBuilder<ProjectionImpl>
		implements TopLevelElementBuilderWithSourceCode {

		@Nonnull
		private final String packageName;

		@Nonnull
		private final ClassifierBuilder<?> classifierBuilder;

		public ProjectionBuilder(
			@Nonnull ProjectionDeclarationContext elementContext,
			@Nonnull Optional<ElementBuilder<?>> macroElement,
			@Nullable SourceCodeBuilder sourceCode,
			int ordinal,
			@Nonnull IdentifierContext nameContext,
			@Nonnull String packageName,
			@Nonnull ClassifierBuilder<?> classifierBuilder
		) {
			super(elementContext, macroElement, sourceCode, ordinal, nameContext);
			this.packageName = Objects.requireNonNull(packageName);
			this.classifierBuilder = Objects.requireNonNull(classifierBuilder);
		}

		@Override
		@Nonnull
		protected ProjectionImpl buildUnsafe() {
			return new ProjectionImpl(
				(ProjectionDeclarationContext) this.elementContext,
				this.macroElement.map(ElementBuilder::getElement),
				this.sourceCode.build(),
				this.ordinal,
				this.getNameContext(),
				this.packageName,
				this.classifierBuilder.getElement()
			);
		}
	}
}
