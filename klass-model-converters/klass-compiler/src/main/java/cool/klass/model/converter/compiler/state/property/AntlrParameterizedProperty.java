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

package cool.klass.model.converter.compiler.state.property;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.criteria.AntlrCriteria;
import cool.klass.model.converter.compiler.state.order.AntlrOrderBy;
import cool.klass.model.converter.compiler.state.parameter.AntlrParameter;
import cool.klass.model.converter.compiler.state.parameter.AntlrParameterOwner;
import cool.klass.model.meta.domain.order.OrderByImpl.OrderByBuilder;
import cool.klass.model.meta.domain.property.ParameterizedPropertyImpl.ParameterizedPropertyBuilder;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterizedPropertyContext;

public class AntlrParameterizedProperty extends AntlrClassReferenceProperty implements AntlrParameterOwner {

    public static final AntlrParameterizedProperty AMBIGUOUS = new AntlrParameterizedProperty(
        new ParameterizedPropertyContext(AMBIGUOUS_PARENT, -1),
        Optional.empty(),
        -1,
        AMBIGUOUS_IDENTIFIER_CONTEXT,
        AntlrClass.AMBIGUOUS
    );

    public static final AntlrParameterizedProperty NOT_FOUND = new AntlrParameterizedProperty(
        new ParameterizedPropertyContext(NOT_FOUND_PARENT, -1),
        Optional.empty(),
        -1,
        NOT_FOUND_IDENTIFIER_CONTEXT,
        AntlrClass.AMBIGUOUS
    );

    // @Nonnull
    // private final ImmutableList<AntlrParameterizedPropertyModifier> parameterizedPropertyModifiers;
    @Nonnull
    private final AntlrClass owningClass;

    private final ParameterHolder parameterHolder = new ParameterHolder();

    @Nullable
    private ParameterizedPropertyBuilder parameterizedPropertyBuilder;

    private AntlrCriteria criteria;

    public AntlrParameterizedProperty(
        @Nonnull ParameterizedPropertyContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        int ordinal,
        @Nonnull IdentifierContext nameContext,
        @Nonnull AntlrClass owningClass
    ) {
        super(elementContext, compilationUnit, ordinal, nameContext);
        this.owningClass = Objects.requireNonNull(owningClass);
    }

    @Nonnull
    @Override
    public ParameterizedPropertyContext getElementContext() {
        return (ParameterizedPropertyContext) super.getElementContext();
    }

    @Nonnull
    @Override
    public Optional<IAntlrElement> getSurroundingElement() {
        return Optional.of(this.owningClass);
    }

    @Override
    public int getNumParameters() {
        return this.parameterHolder.getNumParameters();
    }

    @Override
    public void enterParameterDeclaration(@Nonnull AntlrParameter parameter) {
        this.parameterHolder.enterParameterDeclaration(parameter);
    }

    @Override
    public AntlrParameter getParameterByContext(@Nonnull ParameterDeclarationContext ctx) {
        return this.parameterHolder.getParameterByContext(ctx);
    }

    @Nonnull
    public AntlrCriteria getCriteria() {
        return Objects.requireNonNull(this.criteria);
    }

    public void setCriteria(@Nonnull AntlrCriteria criteria) {
        if (this.criteria != null) {
            throw new IllegalStateException();
        }
        this.criteria = Objects.requireNonNull(criteria);
    }

    @Nonnull
    @Override
    public ParameterizedPropertyBuilder build() {
        if (this.parameterizedPropertyBuilder != null) {
            throw new IllegalStateException();
        }

        /*
        ImmutableList<ParameterizedPropertyModifierBuilder> parameterizedPropertyModifierBuilders =
                this.parameterizedPropertyModifiers.collect(AntlrParameterizedPropertyModifier::build);
        */

        this.parameterizedPropertyBuilder = new ParameterizedPropertyBuilder(
            (ParameterizedPropertyContext) this.elementContext,
            this.getMacroElementBuilder(),
            this.getSourceCodeBuilder(),
            this.ordinal,
            this.getNameContext(),
            this.getType().getElementBuilder(),
            this.owningClass.getElementBuilder(),
            this.multiplicity.getMultiplicity()
        );

        Optional<OrderByBuilder> orderByBuilder = this.orderBy.map(AntlrOrderBy::build);
        this.parameterizedPropertyBuilder.setOrderBy(orderByBuilder);

        return this.parameterizedPropertyBuilder;
    }

    @Nonnull
    @Override
    public ParameterizedPropertyBuilder getElementBuilder() {
        return Objects.requireNonNull(this.parameterizedPropertyBuilder);
    }

    @Override
    protected IdentifierContext getTypeIdentifier() {
        return this.getElementContext().classReference().identifier();
    }

    @Nonnull
    @Override
    public AntlrClass getOwningClassifier() {
        return Objects.requireNonNull(this.owningClass);
    }

    // <editor-fold desc="Report Compiler Errors">
    @Override
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        super.reportErrors(compilerAnnotationHolder);

        if (this.orderBy != null) {
            this.orderBy.ifPresent(o -> o.reportErrors(compilerAnnotationHolder));
        }

        this.reportTypeNotFound(compilerAnnotationHolder);
    }

    @Override
    public void reportNameErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        super.reportNameErrors(compilerAnnotationHolder);
        this.parameterHolder.reportNameErrors(compilerAnnotationHolder);
    }
    // </editor-fold>
}
