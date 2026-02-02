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

package cool.klass.xample.coverage.dropwizard.application;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import cool.klass.dropwizard.bundle.graphql.KlassGraphQLBundle;
import cool.klass.serialization.jackson.module.meta.model.module.KlassMetaModelJacksonModule;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.liftwizard.dropwizard.bundle.httplogging.JerseyHttpLoggingBundle;
import io.liftwizard.servlet.bundle.singlepage.SinglePageRedirectFilterBundle;
import io.liftwizard.servlet.config.singlepage.SinglePageRedirectFilterFactory;
import io.liftwizard.servlet.logging.logstash.encoder.StructuredArgumentsLogstashEncoderLogger;
import io.liftwizard.servlet.logging.mdc.StructuredArgumentsMDCLogger;
import io.liftwizard.servlet.logging.typesafe.StructuredArguments;

public class CoverageExampleApplication extends AbstractCoverageExampleApplication {

	public static void main(String[] args) throws Exception {
		new CoverageExampleApplication().run(args);
	}

	@Override
	public void initialize(@Nonnull Bootstrap<CoverageExampleConfiguration> bootstrap) {
		super.initialize(bootstrap);
	}

	@Override
	protected void initializeCommands(@Nonnull Bootstrap<CoverageExampleConfiguration> bootstrap) {
		super.initializeCommands(bootstrap);
	}

	@Override
	protected void initializeBundles(@Nonnull Bootstrap<CoverageExampleConfiguration> bootstrap) {
		super.initializeBundles(bootstrap);

		var mdcLogger = new StructuredArgumentsMDCLogger(bootstrap.getObjectMapper());
		var logstashLogger = new StructuredArgumentsLogstashEncoderLogger();

		Consumer<StructuredArguments> structuredLogger = (structuredArguments) -> {
			mdcLogger.accept(structuredArguments);
			logstashLogger.accept(structuredArguments);
		};

		bootstrap.addBundle(new JerseyHttpLoggingBundle(structuredLogger));

		bootstrap.addBundle(new KlassGraphQLBundle<>());

		bootstrap.addBundle(
			new MigrationsBundle<>() {
				@Override
				public DataSourceFactory getDataSourceFactory(CoverageExampleConfiguration configuration) {
					return configuration.getNamedDataSourcesFactory().getNamedDataSourceFactoryByName("h2-tcp");
				}
			}
		);

		bootstrap.addBundle(
			new SinglePageRedirectFilterBundle<CoverageExampleConfiguration>() {
				@Override
				public SinglePageRedirectFilterFactory getSinglePageRedirectFilterFactory(
					CoverageExampleConfiguration configuration
				) {
					return configuration.getSinglePageRedirectFilterFactory();
				}
			}
		);
	}

	@Override
	protected void registerJacksonModules(@Nonnull Environment environment) {
		super.registerJacksonModules(environment);

		environment.getObjectMapper().registerModule(new KlassMetaModelJacksonModule());
	}
}
