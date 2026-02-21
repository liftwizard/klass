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

package cool.klass.model.lens;

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Klass;
import org.eclipse.collections.api.map.ImmutableMap;

public class MapLensRegistry implements LensRegistry {

	@Nonnull
	private final ImmutableMap<Klass, ClassLens<?>> lensesByKlass;

	public MapLensRegistry(@Nonnull ImmutableMap<Klass, ClassLens<?>> lensesByKlass) {
		this.lensesByKlass = Objects.requireNonNull(lensesByKlass);
	}

	@Override
	@Nonnull
	public ClassLens<?> getClassLens(@Nonnull Klass klass) {
		Objects.requireNonNull(klass);
		ClassLens<?> classLens = this.lensesByKlass.get(klass);
		if (classLens == null) {
			throw new IllegalStateException("No ClassLens registered for Klass: " + klass.getName());
		}
		return classLens;
	}
}
