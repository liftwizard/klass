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

package cool.klass.model.meta.domain.api.projection;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.property.Property;

public interface ProjectionChild
        extends ProjectionElement
{
    @Nonnull
    Classifier getDeclaredClassifier();

    @Nonnull
    Property getProperty();

    default boolean isPolymorphic()
    {
        Classifier projectionParentClassifier = this.getParent().get().getClassifier();
        Classifier propertyOwner              = this.getProperty().getOwningClassifier();
        if (projectionParentClassifier == propertyOwner)
        {
            return false;
        }

        return !projectionParentClassifier.isStrictSubTypeOf(propertyOwner);
    }
}
