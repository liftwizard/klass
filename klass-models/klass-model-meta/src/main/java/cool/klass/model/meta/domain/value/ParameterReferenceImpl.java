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

package cool.klass.model.meta.domain.value;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.api.value.ParameterReference;
import cool.klass.model.meta.domain.parameter.ParameterImpl;
import cool.klass.model.meta.domain.parameter.ParameterImpl.ParameterBuilder;
import cool.klass.model.meta.grammar.KlassParser.ParameterReferenceContext;

public final class ParameterReferenceImpl extends AbstractExpressionValue implements ParameterReference {

    @Nonnull
    private final ParameterImpl parameter;

    private ParameterReferenceImpl(
        @Nonnull ParameterReferenceContext elementContext,
        @Nonnull Optional<Element> macroElement,
        @Nullable SourceCode sourceCode,
        @Nonnull ParameterImpl parameter
    ) {
        super(elementContext, macroElement, sourceCode);
        this.parameter = Objects.requireNonNull(parameter);
    }

    @Nonnull
    @Override
    public ParameterReferenceContext getElementContext() {
        return (ParameterReferenceContext) super.getElementContext();
    }

    @Override
    @Nonnull
    public ParameterImpl getParameter() {
        return this.parameter;
    }

    public static final class ParameterReferenceBuilder extends AbstractExpressionValueBuilder<ParameterReferenceImpl> {

        @Nonnull
        private final ParameterBuilder parameterBuilder;

        public ParameterReferenceBuilder(
            @Nonnull ParameterReferenceContext elementContext,
            @Nonnull Optional<ElementBuilder<?>> macroElement,
            @Nullable SourceCodeBuilder sourceCode,
            @Nonnull ParameterBuilder parameterBuilder
        ) {
            super(elementContext, macroElement, sourceCode);
            this.parameterBuilder = Objects.requireNonNull(parameterBuilder);
        }

        @Override
        @Nonnull
        protected ParameterReferenceImpl buildUnsafe() {
            return new ParameterReferenceImpl(
                (ParameterReferenceContext) this.elementContext,
                this.macroElement.map(ElementBuilder::getElement),
                this.sourceCode.build(),
                this.parameterBuilder.getElement()
            );
        }
    }
}
