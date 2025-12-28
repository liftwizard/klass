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

package com.workflowy.dropwizard.application;

import javax.annotation.Nonnull;

import cool.klass.dropwizard.bundle.graphql.KlassGraphQLBundle;
import cool.klass.serialization.jackson.module.meta.model.module.KlassMetaModelJacksonModule;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.liftwizard.dropwizard.bundle.httplogging.JerseyHttpLoggingBundle;
import io.liftwizard.servlet.logging.mdc.StructuredArgumentsMDCLogger;

public class WorkflowyApplication extends AbstractWorkflowyApplication {

    public static void main(String[] args) throws Exception {
        new WorkflowyApplication().run(args);
    }

    @Override
    public void initialize(@Nonnull Bootstrap<WorkflowyConfiguration> bootstrap) {
        super.initialize(bootstrap);
    }

    @Override
    protected void initializeCommands(@Nonnull Bootstrap<WorkflowyConfiguration> bootstrap) {
        super.initializeCommands(bootstrap);
    }

    @Override
    protected void initializeBundles(@Nonnull Bootstrap<WorkflowyConfiguration> bootstrap) {
        super.initializeBundles(bootstrap);

        var structuredLogger = new StructuredArgumentsMDCLogger(bootstrap.getObjectMapper());
        bootstrap.addBundle(new JerseyHttpLoggingBundle(structuredLogger));
        bootstrap.addBundle(new KlassGraphQLBundle<>());

        bootstrap.addBundle(
            new MigrationsBundle<>() {
                @Override
                public DataSourceFactory getDataSourceFactory(WorkflowyConfiguration configuration) {
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
    public void run(@Nonnull WorkflowyConfiguration configuration, @Nonnull Environment environment)
        throws Exception {
        super.run(configuration, environment);
    }
}
