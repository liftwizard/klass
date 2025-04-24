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

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.api.value.literal.NullLiteral;
import cool.klass.model.meta.grammar.KlassParser.NullLiteralContext;

public final class NullLiteralImpl extends AbstractLiteralValue implements NullLiteral {

    private NullLiteralImpl(
        @Nonnull NullLiteralContext elementContext,
        @Nonnull Optional<Element> macroElement,
        @Nullable SourceCode sourceCode
    ) {
        super(elementContext, macroElement, sourceCode);
    }

    @Nonnull
    @Override
    public NullLiteralContext getElementContext() {
        return (NullLiteralContext) super.getElementContext();
    }

    public static final class NullLiteralBuilder extends AbstractLiteralValueBuilder<NullLiteralImpl> {

        public NullLiteralBuilder(
            @Nonnull NullLiteralContext elementContext,
            @Nonnull Optional<ElementBuilder<?>> macroElement,
            @Nullable SourceCodeBuilder sourceCode
        ) {
            super(elementContext, macroElement, sourceCode);
        }

        @Override
        @Nonnull
        protected NullLiteralImpl buildUnsafe() {
            return new NullLiteralImpl(
                (NullLiteralContext) this.elementContext,
                this.macroElement.map(ElementBuilder::getElement),
                this.sourceCode.build()
            );
        }
    }
}
