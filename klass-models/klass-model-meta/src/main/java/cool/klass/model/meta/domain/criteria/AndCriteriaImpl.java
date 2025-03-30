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

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.criteria.AndCriteria;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.grammar.KlassParser.CriteriaExpressionAndContext;

public final class AndCriteriaImpl extends AbstractBinaryCriteria implements AndCriteria {

    private AndCriteriaImpl(
        @Nonnull CriteriaExpressionAndContext elementContext,
        @Nonnull Optional<Element> macroElement,
        @Nullable SourceCode sourceCode,
        @Nonnull AbstractCriteria left,
        @Nonnull AbstractCriteria right
    ) {
        super(elementContext, macroElement, sourceCode, left, right);
    }

    @Nonnull
    @Override
    public CriteriaExpressionAndContext getElementContext() {
        return (CriteriaExpressionAndContext) super.getElementContext();
    }

    public static final class AndCriteriaBuilder extends AbstractBinaryCriteriaBuilder<AndCriteriaImpl> {

        public AndCriteriaBuilder(
            @Nonnull CriteriaExpressionAndContext elementContext,
            @Nonnull Optional<ElementBuilder<?>> macroElement,
            @Nullable SourceCodeBuilder sourceCode,
            @Nonnull AbstractCriteriaBuilder<?> left,
            @Nonnull AbstractCriteriaBuilder<?> right
        ) {
            super(elementContext, macroElement, sourceCode, left, right);
        }

        @Override
        @Nonnull
        protected AndCriteriaImpl buildUnsafe() {
            return new AndCriteriaImpl(
                (CriteriaExpressionAndContext) this.elementContext,
                this.macroElement.map(ElementBuilder::getElement),
                this.sourceCode.build(),
                this.left.build(),
                this.right.build()
            );
        }
    }
}
