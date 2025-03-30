/*
 * Copyright 2024 Craig Motlin
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

package cool.klass.dropwizard.bundle.reladomo.response;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.auto.service.AutoService;
import cool.klass.data.store.DataStore;
import cool.klass.dropwizard.configuration.data.store.DataStoreFactoryProvider;
import cool.klass.dropwizard.configuration.domain.model.loader.DomainModelFactoryProvider;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.serialization.jackson.response.KlassResponse;
import cool.klass.serialization.jackson.response.reladomo.KlassResponseReladomoJsonSerializer;
import io.dropwizard.setup.Environment;
import io.liftwizard.dropwizard.bundle.prioritized.PrioritizedBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(PrioritizedBundle.class)
public class ReladomoResponseBundle implements PrioritizedBundle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReladomoResponseBundle.class);

    @Override
    public int getPriority() {
        return -2;
    }

    @Override
    public void runWithMdc(@Nonnull Object configuration, @Nonnull Environment environment) {
        DomainModelFactoryProvider domainModelFactoryProvider =
            this.safeCastConfiguration(DomainModelFactoryProvider.class, configuration);

        DataStoreFactoryProvider dataStoreFactoryProvider =
            this.safeCastConfiguration(DataStoreFactoryProvider.class, configuration);

        LOGGER.info("Running {}.", this.getClass().getSimpleName());

        ObjectMapper objectMapper = environment.getObjectMapper();
        DomainModel domainModel = domainModelFactoryProvider.getDomainModelFactory().createDomainModel(objectMapper);
        DataStore dataStore = dataStoreFactoryProvider.getDataStoreFactory().createDataStore();

        JsonSerializer<KlassResponse> serializer = new KlassResponseReladomoJsonSerializer(domainModel, dataStore);

        SimpleModule module = new SimpleModule();
        module.addSerializer(KlassResponse.class, serializer);
        objectMapper.registerModule(module);

        LOGGER.info("Completing {}.", this.getClass().getSimpleName());
    }
}
