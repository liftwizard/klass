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

package cool.klass.model.meta.domain.projection;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.AbstractClassifier;
import cool.klass.model.meta.domain.AbstractClassifier.ClassifierBuilder;
import cool.klass.model.meta.domain.AbstractIdentifierElement;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.projection.ProjectionParent;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.api.source.projection.ProjectionProjectionReferenceWithSourceCode;
import cool.klass.model.meta.domain.api.source.projection.ProjectionWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.ReferencePropertyWithSourceCode;
import cool.klass.model.meta.domain.projection.AbstractProjectionElement.ProjectionChildBuilder;
import cool.klass.model.meta.domain.projection.AbstractProjectionParent.AbstractProjectionParentBuilder;
import cool.klass.model.meta.domain.projection.ProjectionImpl.ProjectionBuilder;
import cool.klass.model.meta.domain.property.ReferencePropertyImpl.ReferencePropertyBuilder;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionProjectionReferenceContext;

public final class ProjectionProjectionReferenceImpl
    extends AbstractIdentifierElement
    implements ProjectionProjectionReferenceWithSourceCode {

    @Nonnull
    private final ProjectionParent parent;

    @Nonnull
    private final AbstractClassifier classifier;

    @Nonnull
    private final ReferencePropertyWithSourceCode referenceProperty;

    private ProjectionImpl referencedProjection;

    private ProjectionProjectionReferenceImpl(
        @Nonnull ProjectionProjectionReferenceContext elementContext,
        @Nonnull Optional<Element> macroElement,
        @Nullable SourceCode sourceCode,
        int ordinal,
        @Nonnull IdentifierContext nameContext,
        @Nonnull ProjectionParent parent,
        @Nonnull AbstractClassifier classifier,
        @Nonnull ReferencePropertyWithSourceCode referenceProperty
    ) {
        super(elementContext, macroElement, sourceCode, ordinal, nameContext);
        this.parent = Objects.requireNonNull(parent);
        this.classifier = Objects.requireNonNull(classifier);
        this.referenceProperty = Objects.requireNonNull(referenceProperty);
    }

    @Nonnull
    @Override
    public ProjectionProjectionReferenceContext getElementContext() {
        return (ProjectionProjectionReferenceContext) super.getElementContext();
    }

    @Override
    public ProjectionWithSourceCode getProjection() {
        return this.referencedProjection;
    }

    @Override
    @Nonnull
    public Optional<ProjectionParent> getParent() {
        return Optional.of(this.parent);
    }

    @Nonnull
    @Override
    public Classifier getDeclaredClassifier() {
        return this.classifier;
    }

    @Override
    @Nonnull
    public ReferencePropertyWithSourceCode getProperty() {
        return this.referenceProperty;
    }

    private void setReferencedProjection(ProjectionImpl referencedProjection) {
        if (this.referencedProjection != null) {
            throw new IllegalStateException();
        }
        this.referencedProjection = Objects.requireNonNull(referencedProjection);
    }

    public static final class ProjectionProjectionReferenceBuilder
        extends IdentifierElementBuilder<ProjectionProjectionReferenceImpl>
        implements ProjectionChildBuilder {

        @Nonnull
        private final AbstractProjectionParentBuilder<?> parentBuilder;

        @Nonnull
        private final ClassifierBuilder<?> classifierBuilder;

        @Nonnull
        private final ReferencePropertyBuilder<?, ?, ?> referencePropertyBuilder;

        private ProjectionBuilder referencedProjectionBuilder;

        public ProjectionProjectionReferenceBuilder(
            @Nonnull ProjectionProjectionReferenceContext elementContext,
            @Nonnull Optional<ElementBuilder<?>> macroElement,
            @Nullable SourceCodeBuilder sourceCode,
            int ordinal,
            @Nonnull IdentifierContext nameContext,
            @Nonnull AbstractProjectionParentBuilder<?> parentBuilder,
            @Nonnull ClassifierBuilder<?> classifierBuilder,
            @Nonnull ReferencePropertyBuilder<?, ?, ?> referencePropertyBuilder
        ) {
            super(elementContext, macroElement, sourceCode, ordinal, nameContext);
            this.parentBuilder = Objects.requireNonNull(parentBuilder);
            this.classifierBuilder = Objects.requireNonNull(classifierBuilder);
            this.referencePropertyBuilder = Objects.requireNonNull(referencePropertyBuilder);
        }

        public void setReferencedProjectionBuilder(ProjectionBuilder referencedProjectionBuilder) {
            if (this.referencedProjectionBuilder != null) {
                throw new IllegalStateException();
            }

            this.referencedProjectionBuilder = Objects.requireNonNull(referencedProjectionBuilder);
        }

        @Override
        @Nonnull
        protected ProjectionProjectionReferenceImpl buildUnsafe() {
            return new ProjectionProjectionReferenceImpl(
                (ProjectionProjectionReferenceContext) this.elementContext,
                this.macroElement.map(ElementBuilder::getElement),
                this.sourceCode.build(),
                this.ordinal,
                this.getNameContext(),
                this.parentBuilder.getElement(),
                this.classifierBuilder.getElement(),
                this.referencePropertyBuilder.getElement()
            );
        }

        @Override
        public void build2() {
            ProjectionImpl projection = this.referencedProjectionBuilder.getElement();
            this.getElement().setReferencedProjection(projection);
        }
    }
}
