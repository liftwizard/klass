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

package cool.klass.model.meta.domain.api.property;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Enumeration;
import cool.klass.model.meta.domain.api.visitor.DataTypePropertyVisitor;

public interface EnumerationProperty extends DataTypeProperty {
    @Override
    default void visit(@Nonnull PropertyVisitor visitor) {
        visitor.visitEnumerationProperty(this);
    }

    @Override
    default void visit(@Nonnull DataTypePropertyVisitor visitor) {
        visitor.visitEnumerationProperty(this);
    }

    @Nonnull
    @Override
    Enumeration getType();

    @Override
    default boolean isID() {
        return false;
    }

    @Override
    default boolean isTemporalRange() {
        return false;
    }

    @Override
    default boolean isTemporalInstant() {
        return false;
    }

    @Override
    default boolean isTemporal() {
        return false;
    }

    @Override
    default boolean isVersion() {
        return false;
    }
}
