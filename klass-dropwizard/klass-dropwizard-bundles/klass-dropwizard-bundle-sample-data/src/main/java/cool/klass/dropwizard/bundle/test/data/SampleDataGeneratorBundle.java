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

package cool.klass.dropwizard.bundle.test.data;

import java.time.Instant;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import cool.klass.data.store.DataStore;
import cool.klass.dropwizard.configuration.data.store.DataStoreFactoryProvider;
import cool.klass.dropwizard.configuration.domain.model.loader.DomainModelFactoryProvider;
import cool.klass.dropwizard.configuration.sample.data.SampleDataFactory;
import cool.klass.dropwizard.configuration.sample.data.SampleDataFactoryProvider;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.reladomo.sample.data.SampleDataGenerator;
import io.dropwizard.setup.Environment;
import io.liftwizard.dropwizard.bundle.prioritized.PrioritizedBundle;
import org.eclipse.collections.api.list.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(PrioritizedBundle.class)
public class SampleDataGeneratorBundle implements PrioritizedBundle {

	private static final Logger LOGGER = LoggerFactory.getLogger(SampleDataGeneratorBundle.class);

	@Override
	public int getPriority() {
		return -1;
	}

	@Override
	public void runWithMdc(@Nonnull Object configuration, @Nonnull Environment environment) {
		SampleDataFactoryProvider sampleDataFactoryProvider = this.safeCastConfiguration(
			SampleDataFactoryProvider.class,
			configuration
		);
		DomainModelFactoryProvider domainModelFactoryProvider = this.safeCastConfiguration(
			DomainModelFactoryProvider.class,
			configuration
		);
		DataStoreFactoryProvider dataStoreFactoryProvider = this.safeCastConfiguration(
			DataStoreFactoryProvider.class,
			configuration
		);

		SampleDataFactory sampleDataFactory = sampleDataFactoryProvider.getSampleDataFactory();
		if (!sampleDataFactory.isEnabled()) {
			LOGGER.info("{} disabled.", this.getClass().getSimpleName());
			return;
		}

		LOGGER.info("Running {}.", this.getClass().getSimpleName());

		Instant dataInstant = sampleDataFactory.getDataInstant();
		ImmutableList<String> skippedPackages = sampleDataFactory.getSkippedPackages();

		ObjectMapper objectMapper = environment.getObjectMapper();
		DomainModel domainModel = domainModelFactoryProvider.getDomainModelFactory().createDomainModel(objectMapper);
		DataStore dataStore = dataStoreFactoryProvider.getDataStoreFactory().createDataStore();

		SampleDataGenerator sampleDataGenerator = new SampleDataGenerator(
			domainModel,
			dataStore,
			dataInstant,
			skippedPackages
		);
		sampleDataGenerator.generate();

		LOGGER.info("Completing {}.", this.getClass().getSimpleName());
	}
}
