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

package cool.klass.model.meta.domain.api.property;

import javax.annotation.Nonnull;

import org.eclipse.collections.api.map.OrderedMap;

/**
 * States how the database computes a derived property, like {@code numberOfVotes: Long derived = count(this.votes);}.
 * The only derivation so far is the count of a to-many association end.
 */
public interface Derivation
{
	@Nonnull
	DataTypeProperty getOwningProperty();

	/**
	 * The to-many association end whose rows are counted.
	 */
	@Nonnull
	AssociationEnd getAssociationEnd();

	/**
	 * The join between the counted class and the class that owns the derived property.
	 *
	 * @return foreign key properties of the counted class, mapped to the key properties of the owning class they match
	 */
	@Nonnull
	default OrderedMap<DataTypeProperty, DataTypeProperty> getForeignKeys()
	{
		AssociationEnd associationEnd = this.getAssociationEnd();
		OrderedMap<DataTypeProperty, DataTypeProperty> foreignKeys = associationEnd
			.getType()
			.getForeignKeys()
			.get(associationEnd.getOpposite());
		if (foreignKeys == null || foreignKeys.isEmpty())
		{
			throw new IllegalStateException(
				"Expected foreign keys on " +
					associationEnd.getType().getName() +
					" for derivation of " +
					this.getOwningProperty()
			);
		}
		return foreignKeys;
	}
}
