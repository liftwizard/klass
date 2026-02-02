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

package cool.klass.dropwizard.bundle.graphql;

import java.util.Objects;

import cool.klass.model.meta.domain.api.Klass;
import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;

public class KlassTypeResolver implements TypeResolver {

	private final Klass klass;

	public KlassTypeResolver(Klass klass) {
		this.klass = Objects.requireNonNull(klass);
	}

	@Override
	public GraphQLObjectType getType(TypeResolutionEnvironment env) {
		// TODO 2025-03-23: Assert that the resolved type is a subclass of this.klass
		String simpleName = env.getObject().getClass().getSimpleName();
		if (simpleName.endsWith("DTO")) {
			// Chop off "DTO" from the end
			String truncated = simpleName.substring(0, simpleName.length() - 3);
			return env.getSchema().getObjectType(truncated);
		}
		throw new AssertionError("Expected DTO but got " + simpleName);
	}
}
