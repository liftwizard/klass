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

package cool.klass.model.meta.domain.api.order;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;

public enum OrderByDirection {
	ASCENDING("ascending"),
	DESCENDING("descending");

	public static final ImmutableList<OrderByDirection> ORDER_BY_DIRECTIONS = Lists.immutable.with(
		ASCENDING,
		DESCENDING
	);

	private static final ImmutableMap<String, OrderByDirection> BY_PRETTY_NAME = ORDER_BY_DIRECTIONS.groupByUniqueKey(
		OrderByDirection::getPrettyName
	);

	@Nonnull
	private final String prettyName;

	OrderByDirection(@Nonnull String prettyName) {
		this.prettyName = prettyName;
	}

	public static OrderByDirection byPrettyName(String name) {
		return Objects.requireNonNull(BY_PRETTY_NAME.get(name));
	}

	@Nonnull
	public String getPrettyName() {
		return this.prettyName;
	}

	@Nonnull
	@Override
	public String toString() {
		return this.getPrettyName();
	}
}
