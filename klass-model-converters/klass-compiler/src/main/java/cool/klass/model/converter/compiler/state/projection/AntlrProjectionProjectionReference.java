/*
 * Copyright 2024 Craig Motlin
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
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEnd;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrEnumerationProperty;
import cool.klass.model.converter.compiler.state.property.AntlrReferenceProperty;
import cool.klass.model.meta.domain.projection.ProjectionImpl.ProjectionBuilder;
import cool.klass.model.meta.domain.projection.ProjectionProjectionReferenceImpl.ProjectionProjectionReferenceBuilder;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionProjectionReferenceContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.tuple.Pair;

public class AntlrProjectionProjectionReference extends AntlrIdentifierElement implements AntlrProjectionChild {

    @Nonnull
    private final AntlrClassifier classifier;

    @Nonnull
    private final AntlrProjectionParent antlrProjectionParent;

    @Nonnull
    private final AntlrClassifier classifierState;

    @Nonnull
    private final AntlrReferenceProperty<?> referenceProperty;

    @Nonnull
    private final AntlrProjection referencedProjection;

    private ProjectionProjectionReferenceBuilder projectionProjectionReferenceBuilder;

    public AntlrProjectionProjectionReference(
        @Nonnull ProjectionProjectionReferenceContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        int ordinal,
        @Nonnull IdentifierContext nameContext,
        @Nonnull AntlrClassifier classifier,
        @Nonnull AntlrProjectionParent antlrProjectionParent,
        @Nonnull AntlrClassifier classifierState,
        @Nonnull AntlrReferenceProperty<?> referenceProperty,
        @Nonnull AntlrProjection referencedProjection
    ) {
        super(elementContext, compilationUnit, ordinal, nameContext);
        this.classifier = Objects.requireNonNull(classifier);
        this.antlrProjectionParent = Objects.requireNonNull(antlrProjectionParent);
        this.classifierState = Objects.requireNonNull(classifierState);
        this.referenceProperty = Objects.requireNonNull(referenceProperty);
        this.referencedProjection = Objects.requireNonNull(referencedProjection);
    }

    @Nonnull
    @Override
    public ProjectionProjectionReferenceContext getElementContext() {
        return (ProjectionProjectionReferenceContext) super.getElementContext();
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
    public ProjectionProjectionReferenceBuilder build() {
        if (this.projectionProjectionReferenceBuilder != null) {
            throw new IllegalStateException();
        }

        this.projectionProjectionReferenceBuilder = new ProjectionProjectionReferenceBuilder(
            (ProjectionProjectionReferenceContext) this.elementContext,
            this.getMacroElementBuilder(),
            this.getSourceCodeBuilder(),
            this.ordinal,
            this.getNameContext(),
            this.antlrProjectionParent.getElementBuilder(),
            this.classifierState.getElementBuilder(),
            this.referenceProperty.getElementBuilder()
        );

        return this.projectionProjectionReferenceBuilder;
    }

    @Override
    public void build2() {
        ProjectionBuilder referencedProjectionBuilder = this.referencedProjection.getElementBuilder();
        this.projectionProjectionReferenceBuilder.setReferencedProjectionBuilder(referencedProjectionBuilder);
    }

    @Override
    public void visit(@Nonnull AntlrProjectionVisitor visitor) {
        visitor.visitProjectionReference(this);
    }

    @Nonnull
    @Override
    public AntlrProjectionParent getParent() {
        return this.antlrProjectionParent;
    }

    // <editor-fold desc="Report Compiler Errors">
    @Override
    public void reportDuplicateMemberName(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        String message = String.format("Duplicate member: '%s'.", this.getName());
        compilerAnnotationHolder.add("ERR_DUP_PRJ", message, this);
    }

    @Override
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        AntlrClassifier parentClassifier = this.antlrProjectionParent.getClassifier();
        if (
            parentClassifier == AntlrClass.NOT_FOUND ||
            parentClassifier == AntlrClass.AMBIGUOUS ||
            parentClassifier == AntlrClassifier.AMBIGUOUS ||
            parentClassifier == AntlrClassifier.NOT_FOUND
        ) {
            return;
        }

        if (this.referencedProjection == AntlrProjection.AMBIGUOUS) {
            return;
        }

        if (
            this.referenceProperty == AntlrReferenceProperty.NOT_FOUND ||
            this.referenceProperty == AntlrAssociationEnd.NOT_FOUND
        ) {
            AntlrDataTypeProperty<?> dataTypeProperty = parentClassifier.getDataTypePropertyByName(this.getName());
            if (dataTypeProperty == AntlrEnumerationProperty.NOT_FOUND) {
                String message = String.format(
                    "Cannot find member '%s.%s'.",
                    parentClassifier.getName(),
                    this.getName()
                );
                compilerAnnotationHolder.add("ERR_PPR_NFD", message, this);
            } else {
                String message =
                    "Projection reference '%s' requires a reference property with type '%s', but found a data type property '%s.%s' with type '%s'.".formatted(
                            this.referencedProjection.getName(),
                            this.referencedProjection.getClassifier().getName(),
                            parentClassifier.getName(),
                            this.getName(),
                            dataTypeProperty.getTypeName()
                        );

                compilerAnnotationHolder.add("ERR_PPR_TYP", message, this);
            }
        } else if (
            this.referenceProperty == AntlrReferenceProperty.AMBIGUOUS ||
            this.referenceProperty == AntlrAssociationEnd.AMBIGUOUS
        ) {
            String message = String.format("Ambiguous: '%s'.", this);
            compilerAnnotationHolder.add("ERR_PPR_AMB", message, this);
        } else if (this.referencedProjection == AntlrProjection.NOT_FOUND) {
            String message = String.format(
                "Not found: '%s'.",
                this.getElementContext().projectionReference().getText()
            );
            compilerAnnotationHolder.add("ERR_PPR_NFD", message, this, this.getElementContext().projectionReference());
        } else {
            this.reportTypeMismatch(compilerAnnotationHolder);
            this.reportForwardReference(compilerAnnotationHolder);
        }
    }

    private void reportTypeMismatch(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (
            this.classifier == this.referencedProjection.getClassifier() ||
            this.classifier.isSubTypeOf(this.referencedProjection.getClassifier())
        ) {
            return;
        }

        String message = String.format(
            "Type mismatch: '%s' has type '%s' but '%s' has type '%s'.",
            this.getName(),
            this.classifier.getName(),
            this.referencedProjection.getName(),
            this.referencedProjection.getClassifier().getName()
        );
        compilerAnnotationHolder.add("ERR_PRR_KLS", message, this);
    }

    private void reportForwardReference(CompilerAnnotationHolder compilerAnnotationHolder) {
        if (!this.referenceProperty.isToOneRequired() || !this.isForwardReference(this.referencedProjection)) {
            return;
        }

        if (this.referenceProperty instanceof AntlrAssociationEnd associationEnd && associationEnd.isOwned()) {
            return;
        }

        String message = String.format(
            "Projection property '%s' is declared on line %d and has a forward reference to projection '%s' which is declared later in the source file '%s' on line %d.",
            this.getName(),
            this.getElementContext().getStart().getLine(),
            this.referencedProjection.getName(),
            this.getCompilationUnit().get().getSourceName(),
            this.referencedProjection.getElementContext().getStart().getLine()
        );
        compilerAnnotationHolder.add("ERR_FWD_REF", message, this, this.getElementContext().projectionReference());
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
