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

package cool.klass.model.converter.compiler.phase;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.projection.AntlrProjection;
import cool.klass.model.converter.compiler.state.projection.AntlrProjectionDataTypeProperty;
import cool.klass.model.converter.compiler.state.projection.AntlrProjectionParent;
import cool.klass.model.converter.compiler.state.projection.AntlrProjectionProjectionReference;
import cool.klass.model.converter.compiler.state.projection.AntlrProjectionReferenceProperty;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrReferenceProperty;
import cool.klass.model.meta.grammar.KlassParser.ClassifierReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.HeaderContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionParameterizedPropertyContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionPrimitiveMemberContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionProjectionReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionReferencePropertyContext;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.stack.MutableStack;

public class ProjectionPhase extends AbstractCompilerPhase {

    private final MutableStack<AntlrProjectionParent> elementStack = Stacks.mutable.empty();

    public ProjectionPhase(@Nonnull CompilerState compilerState) {
        super(compilerState);
    }

    @Override
    public void enterProjectionDeclaration(@Nonnull ProjectionDeclarationContext ctx) {
        super.enterProjectionDeclaration(ctx);

        AntlrProjection projection = this.compilerState.getCompilerWalk().getProjection();
        Objects.requireNonNull(projection);
        this.elementStack.push(projection);
    }

    @Override
    public void exitProjectionDeclaration(@Nonnull ProjectionDeclarationContext ctx) {
        this.elementStack.pop();
        super.exitProjectionDeclaration(ctx);
    }

    @Override
    public void enterProjectionPrimitiveMember(@Nonnull ProjectionPrimitiveMemberContext ctx) {
        super.enterProjectionPrimitiveMember(ctx);

        AntlrProjectionParent projectionParent = this.elementStack.peek();
        Objects.requireNonNull(projectionParent);

        IdentifierContext nameContext = ctx.identifier();
        String name = nameContext.getText();
        HeaderContext header = ctx.header();
        String headerQuotedText = header.StringLiteral().getText();
        // TODO: ❓ Unescaping header text?
        String headerText = headerQuotedText.substring(1, headerQuotedText.length() - 1);

        ClassifierReferenceContext classifierReferenceContext = ctx.classifierReference();
        AntlrClassifier classifier = classifierReferenceContext == null
            ? projectionParent.getClassifier()
            : this.compilerState.getDomainModel().getClassifierByName(classifierReferenceContext.getText());

        AntlrDataTypeProperty<?> dataTypePropertyState = classifier.getDataTypePropertyByName(name);

        AntlrProjectionDataTypeProperty projectionPrimitiveMemberState = new AntlrProjectionDataTypeProperty(
            ctx,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            projectionParent.getNumChildren() + 1,
            nameContext,
            header,
            headerText,
            projectionParent,
            classifier,
            dataTypePropertyState
        );

        projectionParent.enterAntlrProjectionMember(projectionPrimitiveMemberState);
    }

    @Override
    public void enterProjectionReferenceProperty(@Nonnull ProjectionReferencePropertyContext ctx) {
        super.enterProjectionReferenceProperty(ctx);

        AntlrProjectionParent projectionParent = this.elementStack.peek();

        IdentifierContext nameContext = ctx.identifier();
        String name = nameContext.getText();

        ClassifierReferenceContext classifierReferenceContext = ctx.classifierReference();
        AntlrClassifier classifier = classifierReferenceContext == null
            ? projectionParent.getClassifier()
            : this.compilerState.getDomainModel().getClassifierByName(classifierReferenceContext.getText());
        AntlrReferenceProperty<?> referenceProperty = classifier.getReferencePropertyByName(name);

        AntlrProjectionReferenceProperty projectionReferenceProperty = new AntlrProjectionReferenceProperty(
            ctx,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            projectionParent.getNumChildren() + 1,
            nameContext,
            referenceProperty.getType(),
            projectionParent,
            classifier,
            referenceProperty
        );

        projectionParent.enterAntlrProjectionMember(projectionReferenceProperty);

        this.elementStack.push(projectionReferenceProperty);
    }

    @Override
    public void exitProjectionReferenceProperty(@Nonnull ProjectionReferencePropertyContext ctx) {
        this.elementStack.pop();
        super.exitProjectionReferenceProperty(ctx);
    }

    @Override
    public void enterProjectionProjectionReference(@Nonnull ProjectionProjectionReferenceContext ctx) {
        super.enterProjectionProjectionReference(ctx);

        AntlrProjectionParent projectionParent = this.elementStack.peek();

        IdentifierContext nameContext = ctx.identifier();
        String name = nameContext.getText();
        ProjectionReferenceContext projectionReferenceContext = ctx.projectionReference();
        String projectionName = projectionReferenceContext.identifier().getText();

        ClassifierReferenceContext classifierReferenceContext = ctx.classifierReference();
        AntlrClassifier classifier = classifierReferenceContext == null
            ? projectionParent.getClassifier()
            : this.compilerState.getDomainModel().getClassByName(classifierReferenceContext.getText());
        AntlrReferenceProperty<?> referenceProperty = classifier.getReferencePropertyByName(name);

        AntlrProjection projection = this.compilerState.getDomainModel().getProjectionByName(projectionName);

        AntlrProjectionProjectionReference projectionProjectionReference = new AntlrProjectionProjectionReference(
            ctx,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            projectionParent.getNumChildren() + 1,
            nameContext,
            referenceProperty.getType(),
            projectionParent,
            classifier,
            referenceProperty,
            projection
        );

        projectionParent.enterAntlrProjectionMember(projectionProjectionReference);
    }

    @Override
    public void exitProjectionProjectionReference(@Nonnull ProjectionProjectionReferenceContext ctx) {
        super.exitProjectionProjectionReference(ctx);
    }

    @Override
    public void enterProjectionParameterizedProperty(@Nonnull ProjectionParameterizedPropertyContext ctx) {
        super.enterProjectionParameterizedProperty(ctx);
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".enterProjectionParameterizedProperty() not implemented yet"
        );
    }

    @Override
    public void exitProjectionParameterizedProperty(@Nonnull ProjectionParameterizedPropertyContext ctx) {
        super.exitProjectionParameterizedProperty(ctx);
        throw new UnsupportedOperationException(this.getClass().getSimpleName());
    }
}
