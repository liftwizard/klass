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

package cool.klass.dropwizard.configuration.data.store.reladomo;

import java.util.List;

import javax.validation.Validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import cool.klass.dropwizard.configuration.data.store.DataStoreFactory;
import io.dropwizard.configuration.JsonConfigurationFactory;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.jackson.DiscoverableSubtypeResolver;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.serialization.jackson.config.ObjectMapperConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(LogMarkerTestExtension.class)
class ReladomoDataStoreFactoryTest {

    private final ObjectMapper objectMapper = newObjectMapper();
    private final Validator validator = Validators.newValidator();

    private final JsonConfigurationFactory<DataStoreFactory> factory = new JsonConfigurationFactory<>(
        DataStoreFactory.class,
        this.validator,
        this.objectMapper,
        "dw"
    );

    @Test
    void isDiscoverable() {
        // Make sure the types we specified in META-INF gets picked up
        var discoverableSubtypeResolver = new DiscoverableSubtypeResolver();
        List<Class<?>> discoveredSubtypes = discoverableSubtypeResolver.getDiscoveredSubtypes();
        assertThat(discoveredSubtypes).contains(ReladomoDataStoreFactory.class);
    }

    @Test
    void reladomoDataStore() throws Exception {
        DataStoreFactory dataStoreFactory =
            this.factory.build(new ResourceConfigurationSourceProvider(), "config-test.json5");
        assertThat(dataStoreFactory).isInstanceOf(ReladomoDataStoreFactory.class);
    }

    private static ObjectMapper newObjectMapper() {
        ObjectMapper objectMapper = Jackson.newObjectMapper();
        ObjectMapperConfig.configure(objectMapper);
        return objectMapper;
    }
}
