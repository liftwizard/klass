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

package cool.klass.model.meta.domain.api.value.literal;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Type;
import cool.klass.model.meta.domain.api.value.ExpressionValue;
import cool.klass.model.meta.domain.api.value.ExpressionValueVisitor;
import org.eclipse.collections.api.list.ImmutableList;

public interface LiteralListValue
        extends ExpressionValue
{
    @Override
    default void visit(@Nonnull ExpressionValueVisitor visitor)
    {
        visitor.visitLiteralList(this);
    }

    @Nonnull
    ImmutableList<LiteralValue> getLiteralValues();

    @Nonnull
    Type getType();
}
