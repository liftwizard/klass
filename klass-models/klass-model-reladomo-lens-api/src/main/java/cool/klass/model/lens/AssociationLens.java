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

import cool.klass.model.meta.domain.api.property.AssociationEnd;

/**
 * Lens for association ends.
 *
 * @param <T> the domain object type
 * @param <V> the associated type (single object for to-one, collection for to-many)
 */
public interface AssociationLens<T, V> extends ReferenceLens<T, V> {
	/**
	 * Returns the AssociationEnd from the domain model that this lens corresponds to.
	 *
	 * @return the domain model AssociationEnd
	 */
	@Nonnull
	AssociationEnd getAssociationEnd();

	@Override
	@Nonnull
	default AssociationEnd getProperty() {
		return this.getAssociationEnd();
	}
}
