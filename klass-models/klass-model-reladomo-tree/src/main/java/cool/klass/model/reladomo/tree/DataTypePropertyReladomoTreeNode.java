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

package cool.klass.model.reladomo.tree;

import java.util.Objects;

import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.DataType;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;

public class DataTypePropertyReladomoTreeNode extends AbstractReladomoTreeNode {

    private final DataTypeProperty dataTypeProperty;

    public DataTypePropertyReladomoTreeNode(String name, DataTypeProperty dataTypeProperty) {
        super(name);
        this.dataTypeProperty = Objects.requireNonNull(dataTypeProperty);
    }

    @Override
    public void visit(ReladomoTreeNodeVisitor visitor) {
        visitor.visitDataTypeProperty(this);
    }

    public DataTypeProperty getDataTypeProperty() {
        return this.dataTypeProperty;
    }

    @Override
    public Classifier getOwningClassifier() {
        return this.dataTypeProperty.getOwningClassifier();
    }

    @Override
    public DataType getType() {
        return this.dataTypeProperty.getType();
    }
}
