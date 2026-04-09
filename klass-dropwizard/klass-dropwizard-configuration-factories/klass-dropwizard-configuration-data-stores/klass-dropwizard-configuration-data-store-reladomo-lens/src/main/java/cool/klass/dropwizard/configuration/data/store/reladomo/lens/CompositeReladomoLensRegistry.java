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

package cool.klass.dropwizard.configuration.data.store.reladomo.lens;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.gs.fw.common.mithra.finder.RelatedFinder;
import cool.klass.model.lens.ClassLens;
import cool.klass.model.lens.reladomo.ReladomoLensRegistry;
import cool.klass.model.meta.domain.api.Klass;
import org.eclipse.collections.api.list.ImmutableList;

public class CompositeReladomoLensRegistry implements ReladomoLensRegistry {

	@Nonnull
	private final ImmutableList<ReladomoLensRegistry> registries;

	public CompositeReladomoLensRegistry(@Nonnull ImmutableList<ReladomoLensRegistry> registries) {
		this.registries = Objects.requireNonNull(registries);
	}

	@Override
	public boolean hasClassLens(@Nonnull Klass klass) {
		return this.registries.anySatisfy((registry) -> registry.hasClassLens(klass));
	}

	@Override
	@Nonnull
	public ClassLens<?> getClassLens(@Nonnull Klass klass) {
		for (ReladomoLensRegistry registry : this.registries) {
			if (registry.hasClassLens(klass)) {
				return registry.getClassLens(klass);
			}
		}
		throw new IllegalStateException("No ClassLens registered for Klass: " + klass.getName());
	}

	@Override
	@Nonnull
	public Klass getKlassForJavaClass(@Nonnull Class<?> javaClass) {
		for (ReladomoLensRegistry registry : this.registries) {
			try {
				return registry.getKlassForJavaClass(javaClass);
			} catch (IllegalStateException ignored) {
				// try next registry
			}
		}
		throw new IllegalStateException("No Klass registered for Java class: " + javaClass.getName());
	}

	@Override
	@Nonnull
	public RelatedFinder<?> getRelatedFinderForKlass(@Nonnull Klass klass) {
		for (ReladomoLensRegistry registry : this.registries) {
			try {
				return registry.getRelatedFinderForKlass(klass);
			} catch (IllegalStateException ignored) {
				// try next registry
			}
		}
		throw new IllegalStateException("No RelatedFinder registered for Klass: " + klass.getName());
	}
}
