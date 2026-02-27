/*
 * Copyright 2026 Craig Motlin
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
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

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

	@Nonnull
	default Optional<AssociationEnd> findDeclaredAssociationEndByName(String name) {
		return this.getDeclaredAssociationEnds().detectOptional((associationEnd) ->
			associationEnd.getName().equals(name)
		);
	}

	@Nonnull
	default AssociationEnd getDeclaredAssociationEndByName(String name) {
		return this.findDeclaredAssociationEndByName(name).orElseThrow(() ->
			new AssertionError("No declared AssociationEnd named '" + name + "' on " + this.getName())
		);
	}

	ImmutableList<AssociationEnd> getAssociationEnds();

	@Nonnull
	default Optional<AssociationEnd> findAssociationEndByName(String name) {
		return this.getAssociationEnds().detectOptional((associationEnd) -> associationEnd.getName().equals(name));
	}

	@Nonnull
	default AssociationEnd getAssociationEndByName(String name) {
		return this.findAssociationEndByName(name).orElseThrow(() ->
			new AssertionError("No AssociationEnd named '" + name + "' on " + this.getName())
		);
	}

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
				.count(ReferenceProperty::isOwned)
			== 1
		);
	}

	@Nonnull
	default Optional<Property> findPropertyByName(String name) {
		Optional<DataTypeProperty> maybeDataTypeProperty = this.findDataTypePropertyByName(name);
		Optional<AssociationEnd> maybeAssociationEnd = this.findAssociationEndByName(name);

		if (maybeDataTypeProperty.isPresent() && maybeAssociationEnd.isPresent()) {
			String detailMessage = "Property " + name + " is both a data type property and an association end.";
			throw new AssertionError(detailMessage);
		}

		if (maybeDataTypeProperty.isPresent()) {
			return Optional.of(maybeDataTypeProperty.get());
		}

		return maybeAssociationEnd.map((associationEnd) -> associationEnd);
	}

	@Nonnull
	default Property getPropertyByName(String name) {
		return this.findPropertyByName(name).orElseThrow(() ->
			new AssertionError("No Property named '" + name + "' on " + this.getName())
		);
	}

	@Nonnull
	default Optional<PrimitiveProperty> findPrimitivePropertyByName(String name) {
		return this.findDataTypePropertyByName(name)
			.filter(PrimitiveProperty.class::isInstance)
			.map(PrimitiveProperty.class::cast);
	}

	@Nonnull
	default Optional<EnumerationProperty> findEnumerationPropertyByName(String name) {
		return this.findDataTypePropertyByName(name)
			.filter(EnumerationProperty.class::isInstance)
			.map(EnumerationProperty.class::cast);
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
