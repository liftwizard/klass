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

package cool.klass.model.meta.domain.api.source.projection;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.projection.ProjectionProjectionReference;
import cool.klass.model.meta.domain.api.source.ClassifierWithSourceCode;
import cool.klass.model.meta.domain.api.source.NamedElementWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.ReferencePropertyWithSourceCode;
import cool.klass.model.meta.grammar.KlassParser.ProjectionProjectionReferenceContext;

public interface ProjectionProjectionReferenceWithSourceCode
        extends ProjectionProjectionReference, NamedElementWithSourceCode
{
    @Override
    ProjectionProjectionReferenceContext getElementContext();

    @Override
    ProjectionWithSourceCode getProjection();

    @Nonnull
    @Override
    default ClassifierWithSourceCode getClassifier()
    {
        return this.getProjection().getClassifier();
    }

    @Nonnull
    @Override
    ReferencePropertyWithSourceCode getProperty();
}
