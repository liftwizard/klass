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
import cool.klass.model.meta.domain.api.DataType;
import cool.klass.model.meta.domain.api.projection.ProjectionDataTypeProperty;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;

public class ProjectionDataTypePropertyReladomoNode extends AbstractProjectionElementReladomoNode {

    private final ProjectionDataTypeProperty projectionDataTypeProperty;

    public ProjectionDataTypePropertyReladomoNode(String name, ProjectionDataTypeProperty projectionDataTypeProperty) {
        super(name);
        this.projectionDataTypeProperty = Objects.requireNonNull(projectionDataTypeProperty);
    }

    @Override
    public Classifier getOwningClassifier() {
        return this.projectionDataTypeProperty.getProperty().getOwningClassifier();
    }

    @Override
    public DataType getType() {
        return this.projectionDataTypeProperty.getProperty().getType();
    }

    public DataTypeProperty getProperty() {
        return this.projectionDataTypeProperty.getProperty();
    }
}
