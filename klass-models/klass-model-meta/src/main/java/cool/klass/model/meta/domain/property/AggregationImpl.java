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

package cool.klass.model.meta.domain.property;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.AggregateFunction;
import cool.klass.model.meta.domain.api.property.Aggregation;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;

public final class AggregationImpl
	implements Aggregation
{
	@Nonnull
	private final DataTypeProperty owningProperty;

	@Nonnull
	private final AggregateFunction function;

	@Nonnull
	private final String associationEndName;

	@Nonnull
	private final Optional<String> aggregatedPropertyName;

	public AggregationImpl(
		@Nonnull DataTypeProperty owningProperty,
		@Nonnull AggregateFunction function,
		@Nonnull String associationEndName,
		@Nonnull Optional<String> aggregatedPropertyName
	)
	{
		this.owningProperty = Objects.requireNonNull(owningProperty);
		this.function = Objects.requireNonNull(function);
		this.associationEndName = Objects.requireNonNull(associationEndName);
		this.aggregatedPropertyName = Objects.requireNonNull(aggregatedPropertyName);
	}

	@Nonnull
	@Override
	public DataTypeProperty getOwningProperty()
	{
		return this.owningProperty;
	}

	@Nonnull
	@Override
	public AggregateFunction getFunction()
	{
		return this.function;
	}

	// Association ends are built after data type properties, so the association end is looked up on demand.
	@Nonnull
	@Override
	public AssociationEnd getAssociationEnd()
	{
		var owningClass = (Klass) this.owningProperty.getOwningClassifier();
		return owningClass.getAssociationEndByName(this.associationEndName);
	}

	@Nonnull
	@Override
	public Optional<DataTypeProperty> getAggregatedProperty()
	{
		return this.aggregatedPropertyName.map((propertyName) ->
			this.getAssociationEnd().getType().getDataTypePropertyByName(propertyName)
		);
	}

	@Override
	public String toString()
	{
		String path = this.aggregatedPropertyName.map((propertyName) -> "." + propertyName).orElse("");
		return "%s(this.%s%s)".formatted(this.function.getPrettyName(), this.associationEndName, path);
	}
}
