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

package cool.klass.model.meta.domain.projection.write;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.projection.ProjectionChild;
import cool.klass.model.meta.domain.api.projection.ProjectionParent;
import cool.klass.model.meta.domain.api.projection.ProjectionReferenceProperty;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import org.eclipse.collections.api.list.ImmutableList;

public class WriteProjectionAssociationEndAdapter implements ProjectionReferenceProperty {

    private final ProjectionParent parent;
    private final AssociationEnd associationEnd;

    public WriteProjectionAssociationEndAdapter(ProjectionParent projectionParent, AssociationEnd associationEnd) {
        this.parent = Objects.requireNonNull(projectionParent);
        this.associationEnd = Objects.requireNonNull(associationEnd);
    }

    @Nonnull
    @Override
    public ReferenceProperty getProperty() {
        return this.associationEnd;
    }

    @Nonnull
    @Override
    public Classifier getDeclaredClassifier() {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".getDeclaredClassifier() not implemented yet"
        );
    }

    @Override
    public Optional<ProjectionParent> getParent() {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".getParent() not implemented yet");
    }

    @Override
    public ImmutableList<? extends ProjectionChild> getChildren() {
        Klass klass = this.associationEnd.getType();
        return klass
            .getAssociationEnds()
            .select(ReferenceProperty::isOwned)
            .collect((associationEnd) -> new WriteProjectionAssociationEndAdapter(this, associationEnd));
    }

    @Nonnull
    @Override
    public String getName() {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".getName() not implemented yet");
    }

    @Override
    public int getOrdinal() {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".getOrdinal() not implemented yet");
    }

    @Override
    public Optional<Element> getMacroElement() {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".getMacroElement() not implemented yet"
        );
    }
}
