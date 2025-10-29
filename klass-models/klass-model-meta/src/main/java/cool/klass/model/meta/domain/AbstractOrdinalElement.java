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

package cool.klass.model.meta.domain;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import org.antlr.v4.runtime.ParserRuleContext;

public class AbstractOrdinalElement extends AbstractElement {

    protected final int ordinal;

    public AbstractOrdinalElement(
        @Nonnull ParserRuleContext elementContext,
        @Nonnull Optional<Element> macroElement,
        @Nullable SourceCode sourceCode,
        int ordinal
    ) {
        super(elementContext, macroElement, sourceCode);
        this.ordinal = ordinal;
    }

    public int getOrdinal() {
        return this.ordinal;
    }

    public abstract static class OrdinalElementBuilder<BuiltElement extends AbstractOrdinalElement>
        extends ElementBuilder<BuiltElement> {

        protected final int ordinal;

        protected OrdinalElementBuilder(
            @Nonnull ParserRuleContext elementContext,
            @Nonnull Optional<ElementBuilder<?>> macroElement,
            @Nullable SourceCodeBuilder sourceCode,
            int ordinal
        ) {
            super(elementContext, macroElement, sourceCode);
            this.ordinal = ordinal;
        }
    }
}
