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

package cool.klass.data.store;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import org.eclipse.collections.api.map.MapIterable;

public interface DataStore {
    <Result> Result runInTransaction(TransactionalCommand<Result> transactionalCommand);

    void runInTransaction(Runnable runnable);

    List<Object> findAll(Klass klass);

    Object findByKey(Klass klass, MapIterable<DataTypeProperty, Object> keys);

    List<Object> findByKeyReturningList(Klass klass, MapIterable<DataTypeProperty, Object> keys);

    default Object getReferenceProperty(Object persistentInstance, ReferenceProperty referenceProperty) {
        if (referenceProperty.getMultiplicity().isToOne()) {
            return this.getToOne(persistentInstance, referenceProperty);
        }
        if (referenceProperty.getMultiplicity().isToMany()) {
            return this.getToMany(persistentInstance, referenceProperty);
        }
        throw new IllegalStateException("Unknown multiplicity: " + referenceProperty.getMultiplicity());
    }

    Object getToOne(Object persistentSourceInstance, @Nonnull ReferenceProperty referenceProperty);

    @Nonnull
    List<Object> getToMany(Object persistentSourceInstance, ReferenceProperty referenceProperty);

    @Nullable
    Object getDataTypeProperty(Object persistentInstance, DataTypeProperty dataTypeProperty);

    boolean setDataTypeProperty(Object persistentInstance, DataTypeProperty dataTypeProperty, Object newValue);

    @Nonnull
    Object instantiate(Klass klass, MapIterable<DataTypeProperty, Object> keys);

    void insert(Object persistentInstance);

    boolean setToOne(Object persistentSourceInstance, AssociationEnd associationEnd, Object persistentTargetInstance);

    void deleteOrTerminate(@Nonnull Object persistentInstance);

    void purgeAll(@Nonnull Klass klass);

    boolean isInstanceOf(Object persistentInstance, Classifier classifier);

    Klass getMostSpecificSubclass(Object persistentInstance, Klass klass);

    Object getSuperClass(Object persistentInstance, Klass klass);

    Object getSubClass(Object persistentInstance, Klass superClass, Klass subClass);
}
