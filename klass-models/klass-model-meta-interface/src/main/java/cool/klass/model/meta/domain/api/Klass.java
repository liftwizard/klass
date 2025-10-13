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

import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.modifier.Modifier;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

public interface Klass extends Classifier {
    @Override
    default void visit(TopLevelElementVisitor visitor) {
        visitor.visitKlass(this);
    }

    @Override
    default ImmutableList<Modifier> getInheritedModifiers() {
        ImmutableList<Modifier> superClassModifiers = this.getSuperClass()
            .map(Classifier::getModifiers)
            .orElseGet(Lists.immutable::empty);

        ImmutableList<Modifier> interfaceModifiers = Classifier.super.getInheritedModifiers();

        ImmutableList<Modifier> allModifiers = superClassModifiers.newWithAll(interfaceModifiers);
        return allModifiers.distinctBy(Modifier::getKeyword);
    }

    ImmutableList<AssociationEnd> getDeclaredAssociationEnds();

    AssociationEnd getDeclaredAssociationEndByName(String name);

    ImmutableList<AssociationEnd> getAssociationEnds();

    AssociationEnd getAssociationEndByName(String name);

    // TODO: Replace with an implementation that preserves order
    @Nonnull
    @Override
    default ImmutableList<Property> getProperties() {
        return Lists.immutable
            .<Property>empty()
            .newWithAll(this.getDataTypeProperties())
            .newWithAll(this.getAssociationEnds());
    }

    @Override
    default ImmutableList<Property> getDeclaredProperties() {
        return Lists.immutable
            .<Property>empty()
            .newWithAll(this.getDeclaredDataTypeProperties())
            .newWithAll(this.getDeclaredAssociationEnds());
    }

    @Override
    default boolean isUniquelyOwned() {
        return (
            this.getAssociationEnds()
                .asLazy()
                .reject(ReferenceProperty::isToSelf)
                .collect(AssociationEnd::getOpposite)
                .count(ReferenceProperty::isOwned) ==
            1
        );
    }

    @Nonnull
    default Optional<Property> getPropertyByName(String name) {
        DataTypeProperty dataTypeProperty = this.getDataTypePropertyByName(name);
        AssociationEnd associationEnd = this.getAssociationEndByName(name);

        if (dataTypeProperty != null && associationEnd != null) {
            String detailMessage = "Property " + name + " is both a data type property and an association end.";
            throw new AssertionError(detailMessage);
        }

        if (dataTypeProperty != null) {
            return Optional.of(dataTypeProperty);
        }

        if (associationEnd != null) {
            return Optional.of(associationEnd);
        }

        return Optional.empty();
    }

    @Nonnull
    Optional<AssociationEnd> getVersionProperty();

    @Nonnull
    Optional<AssociationEnd> getVersionedProperty();

    default Optional<DataTypeProperty> getVersionNumberProperty() {
        ImmutableList<DataTypeProperty> versionProperties = this.getDataTypeProperties().select(
            DataTypeProperty::isVersion
        );
        if (versionProperties.size() > 1) {
            throw new AssertionError();
        }
        if (versionProperties.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(versionProperties.getOnly());
    }

    @Nonnull
    Optional<Klass> getSuperClass();

    ImmutableList<Klass> getSubClasses();

    boolean isUser();

    boolean isTransient();

    default boolean isVersioned() {
        return this.getVersionProperty().isPresent();
    }

    default boolean isAudited() {
        return this.getDataTypeProperties().anySatisfy(DataTypeProperty::isAudit);
    }

    @Override
    default boolean isStrictSuperTypeOf(@Nonnull Classifier classifier) {
        if (Classifier.super.isStrictSuperTypeOf(classifier)) {
            return true;
        }

        if (this == classifier) {
            return false;
        }

        if (classifier instanceof Interface) {
            return false;
        }

        Klass klass = (Klass) classifier;
        Optional<Klass> optionalSuperClass = klass.getSuperClass();
        if (optionalSuperClass.isEmpty()) {
            return false;
        }

        Klass superClass = optionalSuperClass.get();
        if (this == superClass) {
            return true;
        }

        return this.isStrictSuperTypeOf(superClass);
    }

    @Override
    default boolean isStrictSubTypeOf(Classifier classifier) {
        if (Classifier.super.isStrictSubTypeOf(classifier)) {
            return true;
        }

        if (this == classifier) {
            return false;
        }

        Optional<Klass> optionalSuperClass = this.getSuperClass();
        if (optionalSuperClass.isEmpty()) {
            return false;
        }

        Klass superClass = optionalSuperClass.get();
        if (superClass == classifier) {
            return true;
        }

        return superClass.isStrictSubTypeOf(classifier);
    }

    // TODO: Consider changing this to BFS to get them ordered by depth
    default ImmutableList<Klass> getSubClassChain() {
        return this.getSubClasses().flatCollect(Klass::getSubClassChainWithThis).toImmutable();
    }

    default ImmutableList<Klass> getSubClassChainWithThis() {
        return Lists.immutable.with(this).newWithAll(this.getSubClassChain());
    }

    default ImmutableList<Klass> getSuperClassChain() {
        return this.getSuperClass().map(Klass::getSuperClassChainWithThis).orElseGet(Lists.immutable::empty);
    }

    default ImmutableList<Klass> getSuperClassChainWithThis() {
        return Lists.immutable.with(this).newWithAll(this.getSuperClassChain());
    }
}
