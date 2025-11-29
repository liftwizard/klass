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

import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.modifier.Modifier;
import cool.klass.model.meta.domain.api.order.OrderBy;
import org.eclipse.collections.api.list.ImmutableList;

public interface ReferenceProperty extends Property {
    @Nonnull
    Multiplicity getMultiplicity();

    @Override
    default boolean isRequired() {
        return this.getMultiplicity().isRequired();
    }

    @Override
    default boolean isDerived() {
        // TODO: derived ReferenceProperties
        return false;
    }

    @Nonnull
    Optional<OrderBy> getOrderBy();

    @Override
    @Nonnull
    Classifier getType();

    @Nonnull
    ImmutableList<Modifier> getModifiers();

    // TODO: Delete overrides
    default boolean isOwned() {
        return this.getModifiers().anySatisfy((modifier) -> modifier.is("owned"));
    }

    default boolean isVersion() {
        return this.getModifiers().anySatisfy(Modifier::isVersion);
    }

    default boolean isAudit() {
        return this.isCreatedBy() || this.isLastUpdatedBy();
    }

    default boolean isCreatedBy() {
        return this.getModifiers().anySatisfy(Modifier::isCreatedBy);
    }

    default boolean isLastUpdatedBy() {
        return this.getModifiers().anySatisfy(Modifier::isLastUpdatedBy);
    }

    default boolean isFinal() {
        return this.getModifiers().anySatisfy(Modifier::isFinal);
    }

    @Override
    default boolean isPrivate() {
        return this.getModifiers().anySatisfy(Modifier::isPrivate);
    }

    default boolean isToSelf() {
        return this.getOwningClassifier().equals(this.getType());
    }
}
