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

package cool.klass.deserializer.json.context;

import java.util.Objects;
import java.util.Optional;

import cool.klass.model.meta.domain.api.NamedElement;

public class ContextNode
{
    private final NamedElement namedElement;
    private final Optional<Integer> index;

    public ContextNode(NamedElement namedElement)
    {
        this.namedElement = Objects.requireNonNull(namedElement);
        this.index = Optional.empty();
    }

    public ContextNode(NamedElement namedElement, int index)
    {
        this.namedElement = namedElement;
        this.index = Optional.of(index);
    }

    @Override
    public String toString()
    {
        String name = this.namedElement.getName();
        String indexString = this.index.map(present -> String.format("[%d]", present)).orElse("");
        return name + indexString;
    }
}
