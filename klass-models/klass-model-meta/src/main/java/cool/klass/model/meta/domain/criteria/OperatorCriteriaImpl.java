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

package cool.klass.model.meta.domain.criteria;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.criteria.OperatorCriteria;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.operator.AbstractOperator;
import cool.klass.model.meta.domain.operator.AbstractOperator.AbstractOperatorBuilder;
import cool.klass.model.meta.domain.value.AbstractExpressionValue;
import cool.klass.model.meta.domain.value.AbstractExpressionValue.AbstractExpressionValueBuilder;
import cool.klass.model.meta.grammar.KlassParser.CriteriaOperatorContext;

public final class OperatorCriteriaImpl
        extends AbstractCriteria
        implements OperatorCriteria
{
    @Nonnull
    private final AbstractOperator        operator;
    @Nonnull
    private final AbstractExpressionValue sourceValue;
    @Nonnull
    private final AbstractExpressionValue targetValue;

    private OperatorCriteriaImpl(
            @Nonnull CriteriaOperatorContext elementContext,
            @Nonnull Optional<Element> macroElement,
            @Nullable SourceCode sourceCode,
            @Nonnull AbstractOperator operator,
            @Nonnull AbstractExpressionValue sourceValue,
            @Nonnull AbstractExpressionValue targetValue)
    {
        super(elementContext, macroElement, sourceCode);
        this.operator    = Objects.requireNonNull(operator);
        this.sourceValue = Objects.requireNonNull(sourceValue);
        this.targetValue = Objects.requireNonNull(targetValue);
    }

    @Nonnull
    @Override
    public CriteriaOperatorContext getElementContext()
    {
        return (CriteriaOperatorContext) super.getElementContext();
    }

    @Override
    @Nonnull
    public AbstractOperator getOperator()
    {
        return this.operator;
    }

    @Override
    @Nonnull
    public AbstractExpressionValue getSourceValue()
    {
        return this.sourceValue;
    }

    @Override
    @Nonnull
    public AbstractExpressionValue getTargetValue()
    {
        return this.targetValue;
    }

    public static final class OperatorCriteriaBuilder
            extends AbstractCriteriaBuilder<OperatorCriteriaImpl>
    {
        @Nonnull
        private final AbstractOperatorBuilder<?>        operator;
        @Nonnull
        private final AbstractExpressionValueBuilder<?> sourceValue;
        @Nonnull
        private final AbstractExpressionValueBuilder<?> targetValue;

        public OperatorCriteriaBuilder(
                @Nonnull CriteriaOperatorContext elementContext,
                @Nonnull Optional<ElementBuilder<?>> macroElement,
                @Nullable SourceCodeBuilder sourceCode,
                @Nonnull AbstractOperatorBuilder<?> operator,
                @Nonnull AbstractExpressionValueBuilder<?> sourceValue,
                @Nonnull AbstractExpressionValueBuilder<?> targetValue)
        {
            super(elementContext, macroElement, sourceCode);
            this.operator    = Objects.requireNonNull(operator);
            this.sourceValue = Objects.requireNonNull(sourceValue);
            this.targetValue = Objects.requireNonNull(targetValue);
        }

        @Override
        @Nonnull
        protected OperatorCriteriaImpl buildUnsafe()
        {
            return new OperatorCriteriaImpl(
                    (CriteriaOperatorContext) this.elementContext,
                    this.macroElement.map(ElementBuilder::getElement),
                    this.sourceCode.build(),
                    this.operator.build(),
                    this.sourceValue.build(),
                    this.targetValue.build());
        }

        @Override
        public String toString()
        {
            return "OperatorCriteriaBuilder{"
                    + "operator=" + this.operator
                    + ", sourceValue=" + this.sourceValue
                    + ", targetValue=" + this.targetValue
                    + '}';
        }
    }
}
