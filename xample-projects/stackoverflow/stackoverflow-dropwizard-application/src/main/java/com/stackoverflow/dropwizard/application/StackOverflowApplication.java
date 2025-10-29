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

package com.stackoverflow.dropwizard.application;

import java.time.Clock;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stackoverflow.service.resource.QuestionResourceManual;
import cool.klass.data.store.DataStore;
import cool.klass.dropwizard.bundle.graphql.KlassGraphQLBundle;
import cool.klass.dropwizard.configuration.KlassFactory;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.serialization.jackson.module.meta.model.module.KlassMetaModelJacksonModule;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.liftwizard.dropwizard.bundle.httplogging.JerseyHttpLoggingBundle;
import io.liftwizard.servlet.logging.mdc.StructuredArgumentsMDCLogger;

public class StackOverflowApplication extends AbstractStackOverflowApplication {

    public static void main(String[] args) throws Exception {
        new StackOverflowApplication().run(args);
    }

    @Override
    public void initialize(@Nonnull Bootstrap<StackOverflowConfiguration> bootstrap) {
        super.initialize(bootstrap);
    }

    @Override
    protected void initializeCommands(@Nonnull Bootstrap<StackOverflowConfiguration> bootstrap) {
        super.initializeCommands(bootstrap);
    }

    @Override
    protected void initializeBundles(@Nonnull Bootstrap<StackOverflowConfiguration> bootstrap) {
        super.initializeBundles(bootstrap);

        var structuredLogger = new StructuredArgumentsMDCLogger(bootstrap.getObjectMapper());
        bootstrap.addBundle(new JerseyHttpLoggingBundle(structuredLogger));
        bootstrap.addBundle(new KlassGraphQLBundle<>());

        bootstrap.addBundle(
            new MigrationsBundle<>() {
                @Override
                public DataSourceFactory getDataSourceFactory(StackOverflowConfiguration configuration) {
                    return configuration.getNamedDataSourcesFactory().getNamedDataSourceFactoryByName("h2-tcp");
                }
            }
        );
    }

    @Override
    protected void registerJacksonModules(@Nonnull Environment environment) {
        super.registerJacksonModules(environment);

        environment.getObjectMapper().registerModule(new KlassMetaModelJacksonModule());
    }

    @Override
    public void run(@Nonnull StackOverflowConfiguration configuration, @Nonnull Environment environment)
        throws Exception {
        super.run(configuration, environment);

        ObjectMapper objectMapper = environment.getObjectMapper();
        KlassFactory klassFactory = configuration.getKlassFactory();
        DataStore dataStore = klassFactory.getDataStoreFactory().createDataStore();
        DomainModel domainModel = klassFactory.getDomainModelFactory().createDomainModel(objectMapper);
        Clock clock = configuration.getClockFactory().createClock();

        environment.jersey().register(new QuestionResourceManual(domainModel, dataStore, clock));
    }
}
