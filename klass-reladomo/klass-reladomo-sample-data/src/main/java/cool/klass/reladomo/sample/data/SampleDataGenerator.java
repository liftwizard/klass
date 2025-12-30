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

package cool.klass.reladomo.sample.data;

import java.time.Instant;
import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.data.store.DataStore;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import org.eclipse.collections.api.list.ImmutableList;

public class SampleDataGenerator {

	private final DomainModel domainModel;
	private final DataStore dataStore;

	private final Instant systemTime;
	private final ImmutableList<String> skippedPackages;

	@Nonnull
	private final KlassRequiredDataGenerator requiredDataGenerator;

	@Nonnull
	private final KlassOptionalDataGenerator optionalDataGenerator;

	public SampleDataGenerator(
		@Nonnull DomainModel domainModel,
		@Nonnull DataStore dataStore,
		@Nonnull Instant systemTime,
		@Nonnull ImmutableList<String> skippedPackages
	) {
		this.domainModel = Objects.requireNonNull(domainModel);
		this.dataStore = Objects.requireNonNull(dataStore);
		this.systemTime = Objects.requireNonNull(systemTime);
		this.skippedPackages = Objects.requireNonNull(skippedPackages);

		this.requiredDataGenerator = new KlassRequiredDataGenerator(this.dataStore);
		this.optionalDataGenerator = new KlassOptionalDataGenerator(this.dataStore);
	}

	public void generate() {
		this.dataStore.runInTransaction((transaction) -> {
				transaction.setSystemTime(this.systemTime.toEpochMilli());
				this.domainModel.getClasses().each(this::generate);
				return null;
			});
	}

	private void generate(@Nonnull Klass klass) {
		if (this.skippedPackages.contains(klass.getPackageName())) {
			return;
		}
		this.requiredDataGenerator.generateIfRequired(klass);
		this.optionalDataGenerator.generateIfRequired(klass);
	}
}
