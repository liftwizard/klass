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

package cool.klass.model.meta.domain.api.source;

import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.token.categories.TokenCategory;
import cool.klass.model.meta.domain.api.DomainModel;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * This is mostly a marker interface, because invariant generics make it inconvenient or pointless to override methods. However, most methods return specific subclasses with source codes:
 * {@link #getTopLevelElements()}
 * {@link #getEnumerations()}
 * {@link #getClassifiers()}
 * {@link #getInterfaces()}
 * {@link #getClasses()}
 * {@link #getAssociations()}
 * {@link #getProjections()}
 * {@link #getServiceGroups()}
 */
public interface DomainModelWithSourceCode extends DomainModel {
    @Override
    TopLevelElementWithSourceCode getTopLevelElementByName(String topLevelElementName);

    ImmutableList<SourceCode> getSourceCodes();

    Optional<TokenCategory> getTokenCategory(Token token);

    @Nonnull
    Optional<ElementWithSourceCode> getElementByReference(Token token);

    @Nonnull
    Optional<ElementWithSourceCode> getElementByDeclaration(Token token);
}
