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

package cool.klass.model.meta.domain.operator;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.operator.InOperator;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.grammar.KlassParser.InOperatorContext;

public final class InOperatorImpl
        extends AbstractOperator
        implements InOperator
{
    private InOperatorImpl(
            @Nonnull InOperatorContext elementContext,
            @Nonnull Optional<Element> macroElement,
            @Nullable SourceCode sourceCode,
            @Nonnull String operatorText)
    {
        super(elementContext, macroElement, sourceCode, operatorText);
    }

    @Nonnull
    @Override
    public InOperatorContext getElementContext()
    {
        return (InOperatorContext) super.getElementContext();
    }

    public static final class InOperatorBuilder
            extends AbstractOperatorBuilder<InOperatorImpl>
    {
        public InOperatorBuilder(
                @Nonnull InOperatorContext elementContext,
                @Nonnull Optional<ElementBuilder<?>> macroElement,
                @Nullable SourceCodeBuilder sourceCode,
                @Nonnull String operatorText)
        {
            super(elementContext, macroElement, sourceCode, operatorText);
        }

        @Override
        @Nonnull
        protected InOperatorImpl buildUnsafe()
        {
            return new InOperatorImpl(
                    (InOperatorContext) this.elementContext,
                    this.macroElement.map(ElementBuilder::getElement),
                    this.sourceCode.build(),
                    this.operatorText);
        }
    }
}
