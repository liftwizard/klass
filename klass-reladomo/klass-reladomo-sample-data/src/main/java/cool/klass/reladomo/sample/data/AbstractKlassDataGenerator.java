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

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.data.store.DataStore;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;

public abstract class AbstractKlassDataGenerator {

    @Nonnull
    protected final DataStore dataStore;

    protected AbstractKlassDataGenerator(@Nonnull DataStore dataStore) {
        this.dataStore = Objects.requireNonNull(dataStore);
    }

    protected abstract Object getNonNullValue(@Nonnull DataTypeProperty dataTypeProperty);

    protected abstract void generateIfRequired(Object persistentInstance, @Nonnull DataTypeProperty dataTypeProperty);

    public void generateIfRequired(@Nonnull Klass klass) {
        Object persistentInstance = this.instantiate(klass);

        klass
            .getDataTypeProperties()
            .reject(DataTypeProperty::isKey)
            .reject(DataTypeProperty::isSystem)
            .reject(DataTypeProperty::isDerived)
            .each(dataTypeProperty -> this.generateIfRequired(persistentInstance, dataTypeProperty));

        this.dataStore.insert(persistentInstance);
    }

    @Nonnull
    private Object instantiate(@Nonnull Klass klass) {
        ImmutableList<DataTypeProperty> keyProperties = klass.getKeyProperties().reject(DataTypeProperty::isID);
        ImmutableMap<DataTypeProperty, Object> keyValues = keyProperties.toImmutableMap(
            keyProperty -> keyProperty,
            this::getNonNullValue
        );

        if (klass.isValidTemporal()) {
            throw new AssertionError();
        }
        return this.dataStore.instantiate(klass, keyValues);
    }

    protected final void generate(Object persistentInstance, @Nonnull DataTypeProperty dataTypeProperty) {
        if (dataTypeProperty.isVersion()) {
            this.dataStore.setDataTypeProperty(persistentInstance, dataTypeProperty, 1);
            return;
        }

        Object value = this.getNonNullValue(dataTypeProperty);
        this.dataStore.setDataTypeProperty(persistentInstance, dataTypeProperty, value);
    }
}
