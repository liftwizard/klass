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

package cool.klass.model.meta.domain.value.literal;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.Type;
import cool.klass.model.meta.domain.api.Type.TypeGetter;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.api.value.literal.LiteralListValue;
import cool.klass.model.meta.domain.api.value.literal.LiteralValue;
import cool.klass.model.meta.grammar.KlassParser.LiteralListContext;
import org.eclipse.collections.api.list.ImmutableList;

public final class LiteralListValueImpl extends AbstractLiteralValue implements LiteralListValue {

    @Nonnull
    private final Type type;

    private ImmutableList<LiteralValue> literalValues;

    private LiteralListValueImpl(
        @Nonnull LiteralListContext elementContext,
        @Nonnull Optional<Element> macroElement,
        @Nullable SourceCode sourceCode,
        @Nonnull Type type
    ) {
        super(elementContext, macroElement, sourceCode);
        this.type = Objects.requireNonNull(type);
    }

    @Nonnull
    @Override
    public LiteralListContext getElementContext() {
        return (LiteralListContext) super.getElementContext();
    }

    @Override
    @Nonnull
    public ImmutableList<LiteralValue> getLiteralValues() {
        return Objects.requireNonNull(this.literalValues);
    }

    public void setLiteralValues(@Nonnull ImmutableList<LiteralValue> literalValues) {
        if (this.literalValues != null) {
            throw new IllegalArgumentException("Literal values already set");
        }
        this.literalValues = Objects.requireNonNull(literalValues);
    }

    @Override
    @Nonnull
    public Type getType() {
        return this.type;
    }

    public static final class LiteralListValueBuilder extends AbstractLiteralValueBuilder<LiteralListValueImpl> {

        @Nonnull
        private final TypeGetter typeBuilder;

        private ImmutableList<AbstractLiteralValueBuilder<?>> literalValueBuilders;

        public LiteralListValueBuilder(
            @Nonnull LiteralListContext elementContext,
            @Nonnull Optional<ElementBuilder<?>> macroElement,
            @Nullable SourceCodeBuilder sourceCode,
            @Nonnull TypeGetter typeBuilder
        ) {
            super(elementContext, macroElement, sourceCode);
            this.typeBuilder = Objects.requireNonNull(typeBuilder);
        }

        public void setLiteralValueBuilders(
            @Nonnull ImmutableList<AbstractLiteralValueBuilder<?>> literalValueBuilders
        ) {
            if (this.literalValueBuilders != null) {
                throw new IllegalStateException();
            }
            this.literalValueBuilders = Objects.requireNonNull(literalValueBuilders);
        }

        @Override
        @Nonnull
        protected LiteralListValueImpl buildUnsafe() {
            return new LiteralListValueImpl(
                (LiteralListContext) this.elementContext,
                this.macroElement.map(ElementBuilder::getElement),
                this.sourceCode.build(),
                this.typeBuilder.getType()
            );
        }

        @Override
        protected void buildChildren() {
            this.element.setLiteralValues(this.literalValueBuilders.collect(AbstractLiteralValueBuilder::build));
        }
    }
}
