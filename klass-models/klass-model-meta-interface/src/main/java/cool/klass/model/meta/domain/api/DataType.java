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

package cool.klass.model.meta.domain.api;

import javax.annotation.Nonnull;

/**
 * A DataType is a type whose instances are identified only by their value.
 * They are primitives, enumerations, and struct-like records with no keys.
 * All instances of a DataType with the same value are considered to be equal instances.
 */
public interface DataType extends Type {
    String getDataTypeName();

    interface DataTypeGetter extends TypeGetter {
        @Nonnull
        @Override
        DataType getType();
    }
}
