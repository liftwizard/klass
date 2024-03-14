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

package cool.klass.model.meta.domain.api.property;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.modifier.Modifier;
import cool.klass.model.meta.domain.api.visitor.DataTypePropertyVisitor;
import cool.klass.model.meta.domain.api.visitor.PrimitiveTypeVisitor;

public interface PrimitiveProperty
        extends DataTypeProperty
{
    @Override
    default void visit(@Nonnull PropertyVisitor visitor)
    {
        visitor.visitPrimitiveProperty(this);
    }

    @Override
    default void visit(@Nonnull DataTypePropertyVisitor visitor)
    {
        PrimitiveTypeVisitor adaptor = new DataTypePropertyVisitorAdaptor(visitor, this);
        this.getType().visit(adaptor);
    }

    @Override
    default boolean isTemporalRange()
    {
        return this.getType().isTemporalRange();
    }

    @Override
    default boolean isTemporalInstant()
    {
        return this.getType().isTemporalInstant();
    }

    @Override
    default boolean isTemporal()
    {
        return this.getType().isTemporal();
    }

    @Override
    default boolean isVersion()
    {
        return this.getModifiers().anySatisfy(Modifier::isVersion);
    }

    @Override
    default boolean isID()
    {
        return this.getModifiers().anySatisfy(Modifier::isID);
    }

    @Override
    @Nonnull
    PrimitiveType getType();
}
