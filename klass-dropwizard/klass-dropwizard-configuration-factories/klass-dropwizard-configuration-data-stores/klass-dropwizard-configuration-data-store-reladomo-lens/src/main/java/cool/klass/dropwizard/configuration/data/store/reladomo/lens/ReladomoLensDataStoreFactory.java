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

import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.service.AutoService;
import cool.klass.data.store.DataStore;
import cool.klass.data.store.reladomo.lens.ReladomoLensDataStore;
import cool.klass.dropwizard.configuration.data.store.DataStoreFactory;
import cool.klass.model.lens.LensRegistry;
import cool.klass.model.lens.LensRegistryFactory;
import cool.klass.model.meta.domain.api.DomainModel;
import io.liftwizard.dropwizard.configuration.uuid.UUIDSupplierFactory;
import io.liftwizard.dropwizard.configuration.uuid.seed.SeedUUIDSupplierFactory;

@JsonTypeName("reladomoLens")
@AutoService(DataStoreFactory.class)
public class ReladomoLensDataStoreFactory implements DataStoreFactory {

	private @NotNull @Valid UUIDSupplierFactory uuidFactory = new SeedUUIDSupplierFactory();
	private @NotNull @Valid LensRegistryFactory lensRegistryFactory;
	private int retryCount = 1;

	private DataStore dataStore;

	@Nonnull
	@Override
	@JsonProperty("uuid")
	public UUIDSupplierFactory getUuidFactory() {
		return this.uuidFactory;
	}

	@JsonProperty("uuid")
	public void setUuid(@Nonnull UUIDSupplierFactory uuidFactory) {
		this.uuidFactory = uuidFactory;
	}

	@JsonProperty
	public int getRetryCount() {
		return this.retryCount;
	}

	@JsonProperty
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	@JsonProperty("lensRegistry")
	public LensRegistryFactory getLensRegistryFactory() {
		return this.lensRegistryFactory;
	}

	@JsonProperty("lensRegistry")
	public void setLensRegistryFactory(LensRegistryFactory lensRegistryFactory) {
		this.lensRegistryFactory = lensRegistryFactory;
	}

	@Override
	public DataStore createDataStore(DomainModel domainModel) {
		if (this.dataStore != null) {
			return this.dataStore;
		}

		Supplier<UUID> uuidSupplier = this.uuidFactory.createUUIDSupplier();
		LensRegistry lensRegistry = this.lensRegistryFactory.createLensRegistry(domainModel);

		this.dataStore = new ReladomoLensDataStore(lensRegistry, uuidSupplier, this.retryCount);
		return this.dataStore;
	}
}
