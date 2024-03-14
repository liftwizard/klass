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

package cool.klass.model.meta.domain;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.AbstractElement.ElementBuilder;
import cool.klass.model.meta.domain.api.DataType.DataTypeGetter;
import cool.klass.model.meta.domain.api.PrimitiveType;
import org.antlr.v4.runtime.ParserRuleContext;

public class PrimitiveTypeBuilder
        extends ElementBuilder<PrimitiveType>
        implements DataTypeGetter
{
    @Nonnull
    private final PrimitiveType primitiveType;

    public PrimitiveTypeBuilder(
            @Nonnull ParserRuleContext elementContext,
            @Nonnull Optional<ElementBuilder<?>> macroElement,
            @Nonnull PrimitiveType primitiveType)
    {
        super(elementContext, macroElement, null);
        this.primitiveType = Objects.requireNonNull(primitiveType);
    }

    @Nonnull
    @Override
    public PrimitiveType getType()
    {
        return this.primitiveType;
    }

    @Nonnull
    @Override
    protected PrimitiveType buildUnsafe()
    {
        return this.primitiveType;
    }
}
