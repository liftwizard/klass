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

package cool.klass.model.lens;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;

/**
 * Class lens that aggregates all lenses for a single Klass in the domain model.
 *
 * <p>This interface provides:
 * <ul>
 *     <li>Access to the corresponding domain model Klass</li>
 *     <li>Unified lens lookup for both data type properties and association ends</li>
 *     <li>Type-specific overloads for compile-time type safety</li>
 *     <li>O(1) lookup by Property without reflection</li>
 * </ul>
 *
 * <p>The lenses become the single source of truth for both data access AND schema metadata,
 * enabling the replacement of mutual recursion over (Klass, MithraObject) with
 * recursion over (ClassLens, Object).
 *
 * @param <T> the domain object type this lens handles
 */
public interface ClassLens<T> {
	/**
	 * Returns the Klass from the domain model that this lens corresponds to.
	 *
	 * @return the domain model Klass
	 */
	@Nonnull
	Klass getKlass();

	/**
	 * Gets all property lenses for this class, including both data type properties
	 * and association ends.
	 *
	 * @return immutable list of all lenses
	 */
	@Nonnull
	ImmutableList<PropertyLens<T, ?>> getLenses();

	/**
	 * Lookup lens by Property (polymorphic - works for any Property subtype).
	 *
	 * @param property the property to look up
	 * @return the lens for the property, or null if not found
	 */
	@Nullable
	PropertyLens<T, ?> getLensByProperty(@Nonnull Property property);

	/**
	 * Type-specific overload returning narrowed type for DataTypeProperty.
	 *
	 * @param property the data type property to look up
	 * @return the lens for the property, or null if not found
	 */
	@Nullable
	DataTypeLens<T, ?> getLensByProperty(@Nonnull DataTypeProperty property);

	/**
	 * Type-specific overload returning narrowed type for PrimitiveProperty.
	 *
	 * @param property the primitive property to look up
	 * @return the lens for the property, or null if not found
	 */
	@Nullable
	PrimitiveLens<T, ?> getLensByProperty(@Nonnull PrimitiveProperty property);

	/**
	 * Type-specific overload returning narrowed type for EnumerationProperty.
	 *
	 * @param property the enumeration property to look up
	 * @return the lens for the property, or null if not found
	 */
	@Nullable
	EnumerationLens<T> getLensByProperty(@Nonnull EnumerationProperty property);

	/**
	 * Type-specific overload returning narrowed type for ReferenceProperty.
	 *
	 * @param property the reference property to look up
	 * @return the lens for the property, or null if not found
	 */
	@Nullable
	ReferenceLens<T, ?> getLensByProperty(@Nonnull ReferenceProperty property);

	/**
	 * Type-specific overload returning narrowed type for AssociationEnd.
	 *
	 * @param property the association end to look up
	 * @return the lens for the property, or null if not found
	 */
	@Nullable
	AssociationLens<T, ?> getLensByProperty(@Nonnull AssociationEnd property);

	/**
	 * Returns a map of all lenses keyed by their corresponding Property.
	 *
	 * @return immutable map of Property to lens
	 */
	@Nonnull
	ImmutableMap<Property, PropertyLens<T, ?>> getLensesByProperty();
}
