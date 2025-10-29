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

package cool.klass.model.meta.domain.api.service;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.TopLevelElement;
import cool.klass.model.meta.domain.api.TopLevelElementVisitor;
import cool.klass.model.meta.domain.api.service.url.Url;
import org.eclipse.collections.api.list.ImmutableList;

public interface ServiceGroup extends TopLevelElement {
    @Override
    default void visit(TopLevelElementVisitor visitor) {
        visitor.visitServiceGroup(this);
    }

    @Nonnull
    Klass getKlass();

    @Nonnull
    ImmutableList<Url> getUrls();
}
