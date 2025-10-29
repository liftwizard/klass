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

package cool.klass.model.meta.domain.value.literal;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.api.value.literal.BooleanLiteralValue;
import cool.klass.model.meta.grammar.KlassParser.BooleanLiteralContext;

public final class BooleanLiteralValueImpl extends AbstractLiteralValue implements BooleanLiteralValue {

    private final boolean value;

    private BooleanLiteralValueImpl(
        @Nonnull BooleanLiteralContext elementContext,
        @Nonnull Optional<Element> macroElement,
        @Nullable SourceCode sourceCode,
        boolean value
    ) {
        super(elementContext, macroElement, sourceCode);
        this.value = value;
    }

    @Nonnull
    @Override
    public BooleanLiteralContext getElementContext() {
        return (BooleanLiteralContext) super.getElementContext();
    }

    @Override
    public boolean getValue() {
        return this.value;
    }

    public static final class BooleanLiteralValueBuilder extends AbstractLiteralValueBuilder<BooleanLiteralValueImpl> {

        private final boolean value;

        public BooleanLiteralValueBuilder(
            @Nonnull BooleanLiteralContext elementContext,
            @Nonnull Optional<ElementBuilder<?>> macroElement,
            @Nullable SourceCodeBuilder sourceCode,
            boolean value
        ) {
            super(elementContext, macroElement, sourceCode);
            this.value = value;
        }

        @Override
        @Nonnull
        protected BooleanLiteralValueImpl buildUnsafe() {
            return new BooleanLiteralValueImpl(
                (BooleanLiteralContext) this.elementContext,
                this.macroElement.map(ElementBuilder::getElement),
                this.sourceCode.build(),
                this.value
            );
        }
    }
}
