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

import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * A function the database applies to the rows of a to-many association end to compute an aggregate property.
 */
public enum AggregateFunction
{
	COUNT("count", false),
	EXISTS("exists", false),
	NOT_EXISTS("notExists", false),
	SUM("sum", true),
	AVERAGE("average", true),
	MIN("min", true),
	MAX("max", true);

	@Nonnull
	private final String prettyName;

	private final boolean aggregatesProperty;

	AggregateFunction(@Nonnull String prettyName, boolean aggregatesProperty)
	{
		this.prettyName = Objects.requireNonNull(prettyName);
		this.aggregatesProperty = aggregatesProperty;
	}

	@Nonnull
	public static AggregateFunction byPrettyName(@Nonnull String prettyName)
	{
		for (AggregateFunction each : AggregateFunction.values())
		{
			if (each.prettyName.equals(prettyName))
			{
				return each;
			}
		}
		throw new IllegalArgumentException(prettyName);
	}

	@Nonnull
	public String getPrettyName()
	{
		return this.prettyName;
	}

	/**
	 * {@return true when the function reads a property of the aggregated rows, like {@code sum(this.votes.score)}, and false when it only counts or tests the rows, like {@code count(this.votes)}}
	 */
	public boolean aggregatesProperty()
	{
		return this.aggregatesProperty;
	}

	/**
	 * {@return true when the function produces null over an empty association end, so the aggregate property must be optional}
	 */
	public boolean isNullWhenEmpty()
	{
		return this == AVERAGE || this == MIN || this == MAX;
	}
}
