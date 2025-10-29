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

package cool.klass.dropwizard.bundle.bootstrap.writer;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import cool.klass.data.store.DataStore;
import cool.klass.dropwizard.configuration.AbstractKlassConfiguration;
import cool.klass.dropwizard.configuration.KlassFactory;
import cool.klass.model.converter.bootstrap.writer.KlassBootstrapWriter;
import cool.klass.model.meta.domain.api.DomainModel;
import io.dropwizard.setup.Environment;
import io.liftwizard.dropwizard.bundle.prioritized.PrioritizedBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(PrioritizedBundle.class)
public class BootstrapWriterBundle implements PrioritizedBundle {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapWriterBundle.class);

    @Override
    public int getPriority() {
        return -2;
    }

    @Override
    public void runWithMdc(@Nonnull Object configuration, @Nonnull Environment environment) {
        AbstractKlassConfiguration klassConfiguration = this.safeCastConfiguration(
            AbstractKlassConfiguration.class,
            configuration
        );
        boolean enabled = klassConfiguration.getBootstrapFactory().isEnabled();
        if (!enabled) {
            LOGGER.info("{} disabled.", this.getClass().getSimpleName());
            return;
        }

        LOGGER.info("Running {}.", this.getClass().getSimpleName());

        ObjectMapper objectMapper = environment.getObjectMapper();
        KlassFactory klassFactory = klassConfiguration.getKlassFactory();
        DataStore dataStore = klassFactory.getDataStoreFactory().createDataStore();
        DomainModel domainModel = klassFactory.getDomainModelFactory().createDomainModel(objectMapper);

        KlassBootstrapWriter klassBootstrapWriter = new KlassBootstrapWriter(domainModel, dataStore);
        klassBootstrapWriter.bootstrapMetaModel();

        LOGGER.info("Completing {}.", this.getClass().getSimpleName());
    }
}
