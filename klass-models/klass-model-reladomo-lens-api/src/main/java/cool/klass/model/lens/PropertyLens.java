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

import cool.klass.model.meta.domain.api.property.Property;

/**
 * Base interface for all property lenses. A lens combines getter and setter
 * functionality with a reference to the corresponding domain model Property.
 *
 * <p>A Lens is a composable getter/setter pair from functional programming.
 * This design is similar to Reladomo's Attribute class, but abstracted
 * to be independent of any specific data store implementation.
 *
 * @param <T> the domain object type
 * @param <V> the value type
 */
public interface PropertyLens<T, V> {
	/**
	 * Gets the value of this property from the domain object.
	 *
	 * @param domainObject the domain object to read from
	 * @return the property value, or null if not set
	 */
	@Nullable
	V get(@Nonnull T domainObject);

	/**
	 * Sets the value of this property on the domain object.
	 *
	 * @param domainObject the domain object to write to
	 * @param value        the value to set
	 */
	void set(@Nonnull T domainObject, @Nullable V value);

	/**
	 * Returns the Property from the domain model that this lens corresponds to.
	 * Subtypes override this with covariant return types.
	 *
	 * @return the domain model Property
	 */
	@Nonnull
	Property getProperty();
}
