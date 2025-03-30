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

package cool.klass.reladomo.sample.data;

import javax.annotation.Nonnull;

import cool.klass.data.store.DataStore;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;

public class KlassRequiredDataGenerator extends AbstractKlassDataGenerator {

    private final RequiredDataTypePropertyVisitor visitor = new RequiredDataTypePropertyVisitor();

    public KlassRequiredDataGenerator(@Nonnull DataStore dataStore) {
        super(dataStore);
    }

    @Override
    protected Object getNonNullValue(@Nonnull DataTypeProperty dataTypeProperty) {
        dataTypeProperty.visit(this.visitor);
        return this.visitor.getResult();
    }

    @Override
    protected void generateIfRequired(Object persistentInstance, @Nonnull DataTypeProperty dataTypeProperty) {
        this.generate(persistentInstance, dataTypeProperty);
    }
}
