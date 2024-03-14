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

import cool.klass.model.meta.domain.AbstractClassifier;
import cool.klass.model.meta.domain.AbstractClassifier.ClassifierBuilder;
import cool.klass.model.meta.domain.EnumerationImpl;
import cool.klass.model.meta.domain.EnumerationImpl.EnumerationBuilder;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.api.source.property.EnumerationPropertyWithSourceCode;
import cool.klass.model.meta.grammar.KlassParser.EnumerationPropertyContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;

public final class EnumerationPropertyImpl
        extends AbstractDataTypeProperty<EnumerationImpl>
        implements EnumerationPropertyWithSourceCode
{
    private EnumerationPropertyImpl(
            @Nonnull EnumerationPropertyContext elementContext,
            @Nonnull Optional<Element> macroElement,
            @Nullable SourceCode sourceCode,
            int ordinal,
            @Nonnull IdentifierContext nameContext,
            @Nonnull EnumerationImpl enumeration,
            @Nonnull AbstractClassifier owningClassifier,
            boolean isOptional)
    {
        super(
                elementContext,
                macroElement,
                sourceCode,
                ordinal,
                nameContext,
                enumeration,
                owningClassifier,
                isOptional);
    }

    @Nonnull
    @Override
    public EnumerationPropertyContext getElementContext()
    {
        return (EnumerationPropertyContext) super.getElementContext();
    }

    public static final class EnumerationPropertyBuilder
            extends DataTypePropertyBuilder<EnumerationImpl, EnumerationBuilder, EnumerationPropertyImpl>
    {
        public EnumerationPropertyBuilder(
                @Nonnull EnumerationPropertyContext elementContext,
                @Nonnull Optional<ElementBuilder<?>> macroElement,
                @Nullable SourceCodeBuilder sourceCode,
                int ordinal,
                @Nonnull IdentifierContext nameContext,
                @Nonnull EnumerationBuilder enumerationBuilder,
                @Nonnull ClassifierBuilder<?> owningClassifierBuilder,
                boolean isOptional)
        {
            super(
                    elementContext,
                    macroElement,
                    sourceCode,
                    ordinal,
                    nameContext,
                    enumerationBuilder,
                    owningClassifierBuilder,
                    isOptional);
        }

        @Override
        @Nonnull
        protected EnumerationPropertyImpl buildUnsafe()
        {
            return new EnumerationPropertyImpl(
                    (EnumerationPropertyContext) this.elementContext,
                    this.macroElement.map(ElementBuilder::getElement),
                    this.sourceCode.build(),
                    this.ordinal,
                    this.getNameContext(),
                    this.typeBuilder.getElement(),
                    this.owningClassifierBuilder.getElement(),
                    this.isOptional);
        }
    }
}
