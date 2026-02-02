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

package cool.klass.model.meta.domain.api.service;

import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;

public enum ServiceMultiplicity {
	ONE("one"),
	MANY("many");

	private static final ImmutableMap<String, ServiceMultiplicity> SERVICE_MULTIPLICITY_BY_PRETTY_NAME =
		ArrayAdapter.adapt(ServiceMultiplicity.values())
			.groupByUniqueKey(ServiceMultiplicity::getPrettyName)
			.toImmutable();

	private final String prettyName;

	ServiceMultiplicity(String prettyName) {
		this.prettyName = prettyName;
	}

	public static ServiceMultiplicity getByPrettyName(String prettyName) {
		return SERVICE_MULTIPLICITY_BY_PRETTY_NAME.get(prettyName);
	}

	public String getPrettyName() {
		return this.prettyName;
	}
}
