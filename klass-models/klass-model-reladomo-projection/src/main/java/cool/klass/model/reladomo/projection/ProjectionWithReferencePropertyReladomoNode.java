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

package cool.klass.model.reladomo.projection;

import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;

public abstract class ProjectionWithReferencePropertyReladomoNode extends AbstractProjectionElementReladomoNode {

    protected ProjectionWithReferencePropertyReladomoNode(String name) {
        super(name);
    }

    @Override
    public abstract Classifier getOwningClassifier();

    @Override
    public abstract Classifier getType();

    public abstract ReferenceProperty getReferenceProperty();
}
