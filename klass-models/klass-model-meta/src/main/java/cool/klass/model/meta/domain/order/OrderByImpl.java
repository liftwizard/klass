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

package cool.klass.model.meta.domain.order;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.AbstractClassifier;
import cool.klass.model.meta.domain.AbstractClassifier.ClassifierBuilder;
import cool.klass.model.meta.domain.AbstractElement;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.order.OrderBy;
import cool.klass.model.meta.domain.api.order.OrderByMemberReferencePath;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.order.OrderByMemberReferencePathImpl.OrderByMemberReferencePathBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;

public final class OrderByImpl extends AbstractElement implements OrderBy {

    @Nonnull
    private final AbstractClassifier thisContext;

    private ImmutableList<OrderByMemberReferencePath> orderByMemberReferencePaths;

    private OrderByImpl(
        @Nonnull ParserRuleContext elementContext,
        @Nonnull Optional<Element> macroElement,
        @Nullable SourceCode sourceCode,
        @Nonnull AbstractClassifier thisContext
    ) {
        super(elementContext, macroElement, sourceCode);
        this.thisContext = Objects.requireNonNull(thisContext);
    }

    @Override
    public ImmutableList<OrderByMemberReferencePath> getOrderByMemberReferencePaths() {
        return this.orderByMemberReferencePaths;
    }

    private void setOrderByMemberReferencePaths(ImmutableList<OrderByMemberReferencePath> orderByMemberReferencePaths) {
        this.orderByMemberReferencePaths = Objects.requireNonNull(orderByMemberReferencePaths);
    }

    public static final class OrderByBuilder extends ElementBuilder<OrderByImpl> {

        @Nonnull
        private final ClassifierBuilder<?> thisContextBuilder;

        private ImmutableList<OrderByMemberReferencePathBuilder> orderByMemberReferencePathBuilders;

        public OrderByBuilder(
            @Nonnull ParserRuleContext elementContext,
            @Nonnull Optional<ElementBuilder<?>> macroElement,
            @Nullable SourceCodeBuilder sourceCode,
            @Nonnull ClassifierBuilder<?> thisContextBuilder
        ) {
            super(elementContext, macroElement, sourceCode);
            this.thisContextBuilder = Objects.requireNonNull(thisContextBuilder);
        }

        public void setOrderByMemberReferencePathBuilders(
            ImmutableList<OrderByMemberReferencePathBuilder> orderByMemberReferencePathBuilders
        ) {
            this.orderByMemberReferencePathBuilders = Objects.requireNonNull(orderByMemberReferencePathBuilders);
        }

        @Override
        @Nonnull
        protected OrderByImpl buildUnsafe() {
            return new OrderByImpl(
                this.elementContext,
                this.macroElement.map(ElementBuilder::getElement),
                this.sourceCode.build(),
                this.thisContextBuilder.getElement()
            );
        }

        @Override
        protected void buildChildren() {
            ImmutableList<OrderByMemberReferencePath> orderByMemberReferencePaths =
                this.orderByMemberReferencePathBuilders.collect(OrderByMemberReferencePathBuilder::build);
            this.element.setOrderByMemberReferencePaths(orderByMemberReferencePaths);
        }
    }
}
