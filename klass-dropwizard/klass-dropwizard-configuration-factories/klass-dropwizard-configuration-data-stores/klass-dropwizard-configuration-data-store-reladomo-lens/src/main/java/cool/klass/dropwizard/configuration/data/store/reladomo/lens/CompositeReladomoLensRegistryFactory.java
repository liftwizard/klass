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

import java.util.List;

import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.service.AutoService;
import cool.klass.model.lens.LensRegistry;
import cool.klass.model.lens.LensRegistryFactory;
import cool.klass.model.lens.reladomo.ReladomoLensRegistry;
import cool.klass.model.meta.domain.api.DomainModel;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

@JsonTypeName("composite")
@AutoService(LensRegistryFactory.class)
public class CompositeReladomoLensRegistryFactory implements LensRegistryFactory {

	private @NotNull @Valid List<LensRegistryFactory> registries;

	@JsonProperty("registries")
	public List<LensRegistryFactory> getRegistries() {
		return this.registries;
	}

	@JsonProperty("registries")
	public void setRegistries(List<LensRegistryFactory> registries) {
		this.registries = registries;
	}

	@Nonnull
	@Override
	public LensRegistry createLensRegistry(@Nonnull DomainModel domainModel) {
		ImmutableList<ReladomoLensRegistry> childRegistries = Lists.immutable
			.withAll(this.registries)
			.collect((factory) -> (ReladomoLensRegistry) factory.createLensRegistry(domainModel));

		return new CompositeReladomoLensRegistry(childRegistries);
	}
}
