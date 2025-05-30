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

package cool.klass.model.meta.domain.property.validation;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.property.validation.MaxPropertyValidation;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.property.AbstractDataTypeProperty;
import cool.klass.model.meta.domain.property.AbstractDataTypeProperty.DataTypePropertyBuilder;
import cool.klass.model.meta.grammar.KlassParser.MaxValidationContext;

public class MaxPropertyValidationImpl extends AbstractNumericPropertyValidation implements MaxPropertyValidation {

    public MaxPropertyValidationImpl(
        @Nonnull MaxValidationContext elementContext,
        @Nonnull Optional<Element> macroElement,
        @Nullable SourceCode sourceCode,
        @Nonnull AbstractDataTypeProperty<?> owningProperty,
        int number
    ) {
        super(elementContext, macroElement, sourceCode, owningProperty, number);
    }

    @Nonnull
    @Override
    public MaxValidationContext getElementContext() {
        return (MaxValidationContext) super.getElementContext();
    }

    public static class MaxPropertyValidationBuilder
        extends NumericPropertyValidationBuilder<MaxPropertyValidationImpl> {

        public MaxPropertyValidationBuilder(
            @Nonnull MaxValidationContext elementContext,
            @Nonnull Optional<ElementBuilder<?>> macroElement,
            @Nullable SourceCodeBuilder sourceCode,
            @Nonnull DataTypePropertyBuilder<?, ?, ?> owningPropertyBuilder,
            int number
        ) {
            super(elementContext, macroElement, sourceCode, owningPropertyBuilder, number);
        }

        @Nonnull
        @Override
        protected MaxPropertyValidationImpl buildUnsafe() {
            return new MaxPropertyValidationImpl(
                (MaxValidationContext) this.elementContext,
                this.macroElement.map(ElementBuilder::getElement),
                this.sourceCode.build(),
                this.owningPropertyBuilder.getElement(),
                this.number
            );
        }
    }
}
