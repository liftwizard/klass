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

import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;

public enum Multiplicity {
	ZERO_TO_ONE("0..1"),
	ONE_TO_ONE("1..1"),
	ZERO_TO_MANY("0..*"),
	ONE_TO_MANY("1..*");

	private static final ImmutableMap<String, Multiplicity> MULTIPLICITY_BY_PRETTY_NAME = ArrayAdapter.adapt(
		Multiplicity.values()
	)
		.groupByUniqueKey(Multiplicity::getPrettyName)
		.toImmutable();

	private final String prettyName;

	Multiplicity(String prettyName) {
		this.prettyName = prettyName;
	}

	public static Multiplicity getByPrettyName(String prettyName) {
		return MULTIPLICITY_BY_PRETTY_NAME.get(prettyName);
	}

	public String getPrettyName() {
		return this.prettyName;
	}

	public boolean isToOne() {
		return this == ZERO_TO_ONE || this == ONE_TO_ONE;
	}

	public boolean isToMany() {
		return this == ZERO_TO_MANY || this == ONE_TO_MANY;
	}

	public boolean isRequired() {
		return this == ONE_TO_ONE || this == ONE_TO_MANY;
	}
}
