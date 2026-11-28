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

import java.util.Optional;

import javax.annotation.Nonnull;

import org.eclipse.collections.api.map.OrderedMap;

/**
 * States how the database computes an aggregate property from the rows of a to-many association end, like {@code numberOfVotes: Long { count(this.votes) }} or {@code totalScore: Long { sum(this.votes.score) }}.
 */
public interface Aggregation
{
	@Nonnull
	DataTypeProperty getOwningProperty();

	@Nonnull
	AggregateFunction getFunction();

	/**
	 * The to-many association end whose rows are aggregated.
	 */
	@Nonnull
	AssociationEnd getAssociationEnd();

	/**
	 * The property of the aggregated rows that the function reads, like {@code score} in {@code sum(this.votes.score)}. Empty for functions that only count or test the rows.
	 */
	@Nonnull
	Optional<DataTypeProperty> getAggregatedProperty();

	/**
	 * The join between the aggregated class and the class that owns the aggregate property.
	 *
	 * @return foreign key properties of the aggregated class, mapped to the key properties of the owning class they match
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
					" for aggregation of " +
					this.getOwningProperty()
			);
		}
		return foreignKeys;
	}
}
