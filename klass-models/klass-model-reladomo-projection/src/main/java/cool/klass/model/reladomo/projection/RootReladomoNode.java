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

import java.util.Objects;

import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.projection.Projection;

public class RootReladomoNode extends AbstractProjectionElementReladomoNode {

    private final Classifier classifier;
    private final Projection projection;

    public RootReladomoNode(String name, Classifier classifier, Projection projection) {
        super(name);
        this.classifier = Objects.requireNonNull(classifier);
        this.projection = Objects.requireNonNull(projection);
    }

    @Override
    public Classifier getOwningClassifier() {
        return this.classifier;
    }

    @Override
    public Classifier getType() {
        return this.classifier;
    }

    @Override
    public String getShortString() {
        return this.getType().getName() + "Finder";
    }

    @Override
    public String getNodeString() {
        return this.getShortString() + ": " + this.getType().getName();
    }

    public Projection getProjection() {
        return this.projection;
    }

    @Override
    public String toString() {
        return this.toString("");
    }
}
