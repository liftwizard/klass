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

package cool.klass.model.adapter;

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
 * Class adapter that aggregates all accessors for a single Klass in the domain model.
 *
 * <p>This interface provides:
 * <ul>
 *     <li>Access to the corresponding domain model Klass</li>
 *     <li>Unified accessor lookup for both data type properties and association ends</li>
 *     <li>Type-specific overloads for compile-time type safety</li>
 *     <li>O(1) lookup by Property without reflection</li>
 * </ul>
 *
 * <p>The adapters become the single source of truth for both data access AND schema metadata,
 * enabling the replacement of mutual recursion over (Klass, MithraObject) with
 * recursion over (ClassAdapter, Object).
 *
 * @param <T> the domain object type this adapter handles
 */
public interface ClassAdapter<T> {
	/**
	 * Returns the Klass from the domain model that this adapter corresponds to.
	 *
	 * @return the domain model Klass
	 */
	@Nonnull
	Klass getKlass();

	/**
	 * Gets all property accessors for this class, including both data type properties
	 * and association ends.
	 *
	 * @return immutable list of all accessors
	 */
	@Nonnull
	ImmutableList<PropertyAccessor<T, ?>> getPropertyAccessors();

	/**
	 * Lookup accessor by Property (polymorphic - works for any Property subtype).
	 *
	 * @param property the property to look up
	 * @return the accessor for the property, or null if not found
	 */
	@Nullable
	PropertyAccessor<T, ?> getAccessorByProperty(@Nonnull Property property);

	/**
	 * Type-specific overload returning narrowed type for DataTypeProperty.
	 *
	 * @param property the data type property to look up
	 * @return the accessor for the property, or null if not found
	 */
	@Nullable
	DataTypePropertyAccessor<T, ?> getAccessorByProperty(@Nonnull DataTypeProperty property);

	/**
	 * Type-specific overload returning narrowed type for PrimitiveProperty.
	 *
	 * @param property the primitive property to look up
	 * @return the accessor for the property, or null if not found
	 */
	@Nullable
	PrimitivePropertyAccessor<T, ?> getAccessorByProperty(@Nonnull PrimitiveProperty property);

	/**
	 * Type-specific overload returning narrowed type for EnumerationProperty.
	 *
	 * @param property the enumeration property to look up
	 * @return the accessor for the property, or null if not found
	 */
	@Nullable
	EnumerationPropertyAccessor<T> getAccessorByProperty(@Nonnull EnumerationProperty property);

	/**
	 * Type-specific overload returning narrowed type for ReferenceProperty.
	 *
	 * @param property the reference property to look up
	 * @return the accessor for the property, or null if not found
	 */
	@Nullable
	ReferencePropertyAccessor<T, ?> getAccessorByProperty(@Nonnull ReferenceProperty property);

	/**
	 * Type-specific overload returning narrowed type for AssociationEnd.
	 *
	 * @param property the association end to look up
	 * @return the accessor for the property, or null if not found
	 */
	@Nullable
	AssociationEndAccessor<T, ?> getAccessorByProperty(@Nonnull AssociationEnd property);

	/**
	 * Returns a map of all accessors keyed by their corresponding Property.
	 *
	 * @return immutable map of Property to accessor
	 */
	@Nonnull
	ImmutableMap<Property, PropertyAccessor<T, ?>> getAccessorsByProperty();
}
