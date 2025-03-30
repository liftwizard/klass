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

package cool.klass.model.meta.domain.property;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.KlassImpl;
import cool.klass.model.meta.domain.KlassImpl.KlassBuilder;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.api.source.property.ParameterizedPropertyWithSourceCode;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterizedPropertyContext;

// TODO: Super class for reference-type-property?
public final class ParameterizedPropertyImpl
    extends ReferencePropertyImpl<KlassImpl>
    implements ParameterizedPropertyWithSourceCode {

    // @Nonnull
    // private final ImmutableList<ParameterizedPropertyModifier> parameterizedPropertyModifiers;

    private ParameterizedPropertyImpl(
        @Nonnull ParameterizedPropertyContext elementContext,
        @Nonnull Optional<Element> macroElement,
        @Nullable SourceCode sourceCode,
        int ordinal,
        @Nonnull IdentifierContext nameContext,
        @Nonnull KlassImpl type,
        @Nonnull KlassImpl owningClass,
        @Nonnull Multiplicity multiplicity
    ) {
        super(elementContext, macroElement, sourceCode, ordinal, nameContext, type, owningClass, multiplicity);
    }

    @Nonnull
    @Override
    public ParameterizedPropertyContext getElementContext() {
        return (ParameterizedPropertyContext) super.getElementContext();
    }

    @Nonnull
    @Override
    public Klass getOwningClassifier() {
        return (Klass) super.getOwningClassifier();
    }

    public static final class ParameterizedPropertyBuilder
        extends ReferencePropertyBuilder<KlassImpl, KlassBuilder, ParameterizedPropertyImpl> {

        public ParameterizedPropertyBuilder(
            @Nonnull ParameterizedPropertyContext elementContext,
            @Nonnull Optional<ElementBuilder<?>> macroElement,
            @Nullable SourceCodeBuilder sourceCode,
            int ordinal,
            @Nonnull IdentifierContext nameContext,
            @Nonnull KlassBuilder type,
            @Nonnull KlassBuilder owningClassBuilder,
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
        }

        @Override
        @Nonnull
        protected ParameterizedPropertyImpl buildUnsafe() {
            return new ParameterizedPropertyImpl(
                (ParameterizedPropertyContext) this.elementContext,
                this.macroElement.map(ElementBuilder::getElement),
                this.sourceCode.build(),
                this.ordinal,
                this.getNameContext(),
                this.typeBuilder.getElement(),
                (KlassImpl) this.owningClassifierBuilder.getElement(),
                this.multiplicity
            );
        }
    }
}
