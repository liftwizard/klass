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

package cool.klass.model.meta.domain.api;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.modifier.Modifier;
import cool.klass.model.meta.domain.api.modifier.ModifierOwner;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.api.map.OrderedMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;

public interface Classifier extends Type, ModifierOwner, TopLevelElement {
    boolean isAbstract();

    @Nonnull
    ImmutableList<Interface> getInterfaces();

    @Nonnull
    default ImmutableList<Modifier> getModifiers() {
        Objects.requireNonNull(this.getDeclaredModifiers());

        MutableSet<String> propertyNames = this.getDeclaredModifiers().collect(Modifier::getKeyword).toSet();

        ImmutableList<Modifier> inheritedProperties = this.getInheritedModifiers().reject(inheritedProperty ->
            propertyNames.contains(inheritedProperty.getKeyword())
        );

        return this.getDeclaredModifiers().newWithAll(inheritedProperties);
    }

    default ImmutableList<Modifier> getInheritedModifiers() {
        return this.getInterfaces()
            .flatCollect(Classifier::getModifiers)
            .distinctBy(Modifier::getKeyword)
            .toImmutable();
    }

    @Nonnull
    ImmutableList<Modifier> getDeclaredModifiers();

    @Nonnull
    ImmutableList<Property> getProperties();

    ImmutableList<Property> getDeclaredProperties();

    ImmutableList<DataTypeProperty> getKeyProperties();

    @Nonnull
    ImmutableList<DataTypeProperty> getDataTypeProperties();

    @Nonnull
    ImmutableList<DataTypeProperty> getDeclaredDataTypeProperties();

    DataTypeProperty getDataTypePropertyByName(String name);

    ImmutableList<ReferenceProperty> getDeclaredReferenceProperties();

    ImmutableList<ReferenceProperty> getReferenceProperties();

    boolean isUniquelyOwned();

    default boolean isTemporal() {
        return this.isSystemTemporal() || this.isValidTemporal();
    }

    default boolean isBitemporal() {
        return this.isSystemTemporal() && this.isValidTemporal();
    }

    default boolean isSystemTemporal() {
        return this.getDataTypeProperties().anySatisfy(DataTypeProperty::isSystemTemporal);
    }

    default boolean isValidTemporal() {
        return this.getDataTypeProperties().anySatisfy(DataTypeProperty::isValidTemporal);
    }

    default boolean isStrictSuperTypeOf(@Nonnull Classifier classifier) {
        if (this == classifier) {
            return false;
        }

        ImmutableList<Interface> superInterfaces = classifier.getInterfaces();
        if (superInterfaces.contains(this)) {
            return true;
        }

        return superInterfaces.anySatisfy(this::isStrictSuperTypeOf);
    }

    default boolean isSubTypeOf(Classifier classifier) {
        if (this == classifier) {
            return true;
        }

        return this.isStrictSubTypeOf(classifier);
    }

    default boolean isStrictSubTypeOf(Classifier classifier) {
        if (this == classifier) {
            return false;
        }

        if (classifier instanceof Klass) {
            return false;
        }

        ImmutableList<Interface> superInterfaces = this.getInterfaces();
        if (superInterfaces.contains(classifier)) {
            return true;
        }

        return superInterfaces.anySatisfyWith(Interface::isStrictSubTypeOf, classifier);
    }

    @Nonnull
    default MutableOrderedMap<AssociationEnd, MutableOrderedMap<DataTypeProperty, DataTypeProperty>> getForeignKeys() {
        MutableOrderedMap<AssociationEnd, MutableOrderedMap<DataTypeProperty, DataTypeProperty>> foreignKeyConstraints =
            OrderedMapAdapter.adapt(new LinkedHashMap<>());

        for (DataTypeProperty foreignKey : this.getDeclaredDataTypeProperties()) {
            OrderedMap<AssociationEnd, DataTypeProperty> keysMatchingThisForeignKey =
                foreignKey.getKeysMatchingThisForeignKey();

            keysMatchingThisForeignKey.forEachKeyValue((associationEnd, key) -> {
                MutableOrderedMap<DataTypeProperty, DataTypeProperty> dataTypeProperties =
                    foreignKeyConstraints.computeIfAbsent(associationEnd, ignored ->
                        OrderedMapAdapter.adapt(new LinkedHashMap<>())
                    );
                dataTypeProperties.put(foreignKey, key);
            });
        }

        return foreignKeyConstraints;
    }

    Optional<PrimitiveProperty> getSystemProperty();

    Optional<PrimitiveProperty> getSystemFromProperty();

    Optional<PrimitiveProperty> getSystemToProperty();

    Optional<PrimitiveProperty> getValidProperty();

    Optional<PrimitiveProperty> getValidFromProperty();

    Optional<PrimitiveProperty> getValidToProperty();

    Optional<PrimitiveProperty> getCreatedByProperty();

    Optional<PrimitiveProperty> getCreatedOnProperty();

    Optional<PrimitiveProperty> getLastUpdatedByProperty();
}
