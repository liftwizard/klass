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

package cool.klass.model.meta.domain.api;

import java.util.Optional;

import javax.annotation.Nonnull;

public interface EnumerationLiteral extends TypedElement {
    @Nonnull
    Optional<String> getDeclaredPrettyName();

    @Nonnull
    default String getPrettyName() {
        return this.getDeclaredPrettyName().orElseGet(this::getName);
    }

    @Override
    @Nonnull
    Enumeration getType();
}
