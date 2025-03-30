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

package cool.klass.model.meta.domain.parameter;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.AbstractIdentifierElement;
import cool.klass.model.meta.domain.api.DataType;
import cool.klass.model.meta.domain.api.DataType.DataTypeGetter;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.parameter.Parameter;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.antlr.v4.runtime.ParserRuleContext;

public final class ParameterImpl extends AbstractIdentifierElement implements Parameter {

    @Nonnull
    private final Multiplicity multiplicity;

    @Nonnull
    private final DataType dataType;

    private ParameterImpl(
        @Nonnull ParserRuleContext elementContext,
        @Nonnull Optional<Element> macroElement,
        @Nullable SourceCode sourceCode,
        int ordinal,
        @Nonnull IdentifierContext nameContext,
        @Nonnull Multiplicity multiplicity,
        @Nonnull DataType dataType
    ) {
        super(elementContext, macroElement, sourceCode, ordinal, nameContext);
        this.multiplicity = Objects.requireNonNull(multiplicity);
        this.dataType = Objects.requireNonNull(dataType);
    }

    @Nonnull
    @Override
    public ParserRuleContext getElementContext() {
        return super.getElementContext();
    }

    @Override
    public String toString() {
        return String.format("{%s}", this.getName());
    }

    @Override
    @Nonnull
    public DataType getType() {
        return this.dataType;
    }

    @Override
    @Nonnull
    public Multiplicity getMultiplicity() {
        return this.multiplicity;
    }

    public static final class ParameterBuilder extends IdentifierElementBuilder<ParameterImpl> {

        @Nonnull
        private final DataTypeGetter dataTypeGetter;

        @Nonnull
        private final Multiplicity multiplicity;

        public ParameterBuilder(
            @Nonnull ParserRuleContext elementContext,
            @Nonnull Optional<ElementBuilder<?>> macroElement,
            @Nullable SourceCodeBuilder sourceCode,
            int ordinal,
            @Nonnull IdentifierContext nameContext,
            @Nonnull DataTypeGetter dataType,
            @Nonnull Multiplicity multiplicity
        ) {
            super(elementContext, macroElement, sourceCode, ordinal, nameContext);
            this.dataTypeGetter = Objects.requireNonNull(dataType);
            this.multiplicity = Objects.requireNonNull(multiplicity);
        }

        @Override
        @Nonnull
        protected ParameterImpl buildUnsafe() {
            return new ParameterImpl(
                this.elementContext,
                this.macroElement.map(ElementBuilder::getElement),
                this.sourceCode.build(),
                this.ordinal,
                this.getNameContext(),
                this.multiplicity,
                this.dataTypeGetter.getType()
            );
        }
    }
}
