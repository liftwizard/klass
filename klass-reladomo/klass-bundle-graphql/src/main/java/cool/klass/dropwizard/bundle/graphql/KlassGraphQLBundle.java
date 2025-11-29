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

package cool.klass.dropwizard.bundle.graphql;

import java.time.Clock;
import java.util.List;

import javax.annotation.Nonnull;
import javax.servlet.ServletRegistration.Dynamic;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import com.smoketurner.dropwizard.graphql.CachingPreparsedDocumentProvider;
import com.smoketurner.dropwizard.graphql.GraphQLBundle;
import com.smoketurner.dropwizard.graphql.GraphQLFactory;
import cool.klass.data.store.reladomo.ReladomoDataStore;
import cool.klass.dropwizard.configuration.data.store.DataStoreFactoryProvider;
import cool.klass.dropwizard.configuration.domain.model.loader.DomainModelFactoryProvider;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.reladomo.tree.converter.graphql.ReladomoTreeGraphqlConverter;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.execution.GraphQLQueryInvoker;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.GraphQLHttpServlet;
import graphql.scalars.java.JavaPrimitives;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeRuntimeWiring;
import io.dropwizard.Configuration;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.liftwizard.dropwizard.configuration.graphql.GraphQLFactoryProvider;
import io.liftwizard.graphql.instrumentation.logging.LiftwizardGraphQLLoggingInstrumentation;
import io.liftwizard.graphql.instrumentation.metrics.LiftwizardGraphQLMetricsInstrumentation;
import io.liftwizard.graphql.scalar.temporal.GraphQLLocalDateScalar;
import io.liftwizard.graphql.scalar.temporal.GraphQLTemporalScalar;
import org.atteo.evo.inflector.English;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

/**
 * The bundle registers the GraphIQL UI at /graphiql and the GraphQL Playground UI at /graphql-playground, by delegating to AssetsBundle. This overrides the behavior of the smoketurner bundle {@link GraphQLBundle}, which registers just one UI (graphiql in older versions, and graphql-playground in newer versions) and registers the UI at the root.
 *
 * <p>
 * The bundle also registers two instrumentations for logging and metrics.
 *
 * @see <a href="https://liftwizard.io/docs/graphql/bundle#liftwizardgraphqlbundle">https://liftwizard.io/docs/graphql/bundle#liftwizardgraphqlbundle</a>
 */
public class KlassGraphQLBundle<
    T extends Configuration & GraphQLFactoryProvider & DomainModelFactoryProvider & DataStoreFactoryProvider
>
    extends GraphQLBundle<T> {

    private MetricRegistry metricRegistry;
    private Environment environment;

    @Override
    public void initialize(@Nonnull Bootstrap<?> bootstrap) {
        try (MDCCloseable mdc = MDC.putCloseable("liftwizard.bundle", this.getClass().getSimpleName())) {
            this.initializeWithMdc(bootstrap);
        }
    }

    private void initializeWithMdc(@Nonnull Bootstrap<?> bootstrap) {
        this.metricRegistry = bootstrap.getMetricRegistry();

        bootstrap.addBundle(new AssetsBundle("/graphiql", "/graphiql", "index.htm", "graphiql"));

        bootstrap.addBundle(new AssetsBundle("/assets", "/graphql-playground", "index.htm", "graphql-playground"));
    }

    @Override
    public void run(T configuration, Environment environment) throws Exception {
        this.environment = environment;

        GraphQLFactory factory = this.getGraphQLFactory(configuration);

        PreparsedDocumentProvider provider = new CachingPreparsedDocumentProvider(
            factory.getQueryCache(),
            environment.metrics()
        );

        GraphQLSchema schema = factory.build();

        GraphQLQueryInvoker queryInvoker = GraphQLQueryInvoker.newBuilder()
            .withPreparsedDocumentProvider(provider)
            .withInstrumentation(factory.getInstrumentations())
            .build();

        GraphQLObjectMapper graphQLObjectMapper = GraphQLObjectMapper.newBuilder()
            .withGraphQLErrorHandler(new KlassGraphQLErrorHandler())
            .build();

        GraphQLConfiguration config = GraphQLConfiguration.with(schema)
            .with(queryInvoker)
            .with(graphQLObjectMapper)
            .build();

        GraphQLHttpServlet servlet = new ConfiguredGraphQLHttpServlet(config);

        Dynamic servletRegistration = environment.servlets().addServlet("graphql", servlet);
        servletRegistration.setAsyncSupported(false);
        servletRegistration.addMapping("/graphql", "/schema.json");
    }

    @Nonnull
    @Override
    public GraphQLFactory getGraphQLFactory(@Nonnull T configuration) {
        // the RuntimeWiring must be configured prior to the run()
        // methods being called so the schema is connected properly.
        GraphQLFactory factory = configuration.getGraphQLFactory();

        // TODO: Move the Clock to Configuration
        Clock clock = Clock.systemUTC();

        var metricsInstrumentation = new LiftwizardGraphQLMetricsInstrumentation(this.metricRegistry, clock);
        var loggingInstrumentation = new LiftwizardGraphQLLoggingInstrumentation();

        List<Instrumentation> instrumentations = List.of(metricsInstrumentation, loggingInstrumentation);
        factory.setInstrumentations(instrumentations);

        ObjectMapper objectMapper = this.environment.getObjectMapper();
        DomainModel domainModel = configuration.getDomainModelFactory().createDomainModel(objectMapper);
        ReladomoDataStore dataStore = (ReladomoDataStore) configuration.getDataStoreFactory().createDataStore();

        RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();
        builder
            .scalar(GraphQLTemporalScalar.INSTANT_INSTANCE)
            .scalar(GraphQLTemporalScalar.TEMPORAL_INSTANT_INSTANCE)
            .scalar(GraphQLTemporalScalar.TEMPORAL_RANGE_INSTANCE)
            .scalar(JavaPrimitives.GraphQLLong)
            .scalar(GraphQLLocalDateScalar.INSTANCE);

        TypeRuntimeWiring.Builder queryTypeBuilder = this.getQueryTypeBuilder(
            domainModel,
            dataStore,
            new ReladomoTreeGraphqlConverter(domainModel)
        );
        builder.type(queryTypeBuilder);

        domainModel.getClasses().select(Classifier::isAbstract).collect(this::getTypeResolver).each(builder::type);

        RuntimeWiring runtimeWiring = builder.build();
        factory.setRuntimeWiring(runtimeWiring);
        return factory;
    }

    @Nonnull
    private TypeRuntimeWiring.Builder getQueryTypeBuilder(
        DomainModel domainModel,
        ReladomoDataStore dataStore,
        ReladomoTreeGraphqlConverter reladomoTreeGraphqlConverter
    ) {
        TypeRuntimeWiring.Builder queryTypeBuilder = new TypeRuntimeWiring.Builder();
        queryTypeBuilder.typeName("Query");

        this.handleQueryAll(queryTypeBuilder, domainModel, dataStore, reladomoTreeGraphqlConverter);
        this.handleQueryByKey(queryTypeBuilder, domainModel, dataStore, reladomoTreeGraphqlConverter);
        this.handleQueryByOperation(queryTypeBuilder, domainModel, dataStore, reladomoTreeGraphqlConverter);
        this.handleQueryByFinder(queryTypeBuilder, domainModel, dataStore, reladomoTreeGraphqlConverter);

        return queryTypeBuilder;
    }

    private void handleQueryAll(
        TypeRuntimeWiring.Builder queryTypeBuilder,
        DomainModel domainModel,
        ReladomoDataStore dataStore,
        ReladomoTreeGraphqlConverter reladomoTreeGraphqlConverter
    ) {
        domainModel
            .getClasses()
            .each((eachKlass) ->
                this.handleQueryAll(queryTypeBuilder, dataStore, reladomoTreeGraphqlConverter, eachKlass)
            );
    }

    private void handleQueryAll(
        TypeRuntimeWiring.Builder queryTypeBuilder,
        ReladomoDataStore dataStore,
        ReladomoTreeGraphqlConverter reladomoTreeGraphqlConverter,
        Klass klass
    ) {
        String propertyName = this.getPropertyName(klass);
        AllDataFetcher allDataFetcher = new AllDataFetcher(klass, dataStore, reladomoTreeGraphqlConverter);
        queryTypeBuilder.dataFetcher(propertyName, allDataFetcher);
    }

    private String getPropertyName(Classifier classifier) {
        String classifierName = classifier.getName();

        String lowerUnderscore = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, classifierName);
        MutableList<String> splits = ArrayAdapter.adapt(lowerUnderscore.split("_"));

        return splits
            .collectWithIndex((eachSplit, index) -> this.capitalizeSplit(eachSplit, index, splits.size()))
            .makeString("");
    }

    private String capitalizeSplit(String eachSplit, int index, int splitsSize) {
        return this.getCapitalized(index, this.getPluralized(index, splitsSize, eachSplit));
    }

    private String getPluralized(int index, int splitsSize, String eachSplit) {
        return index == splitsSize - 1 ? English.plural(eachSplit) : eachSplit;
    }

    private String getCapitalized(int index, String eachSplit) {
        return index != 0 ? CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, eachSplit) : eachSplit;
    }

    private void handleQueryByKey(
        TypeRuntimeWiring.Builder queryTypeBuilder,
        DomainModel domainModel,
        ReladomoDataStore dataStore,
        ReladomoTreeGraphqlConverter reladomoTreeGraphqlConverter
    ) {
        domainModel
            .getClasses()
            .each((eachKlass) ->
                this.handleQueryByKey(queryTypeBuilder, dataStore, reladomoTreeGraphqlConverter, eachKlass)
            );
    }

    private void handleQueryByKey(
        TypeRuntimeWiring.Builder queryTypeBuilder,
        ReladomoDataStore dataStore,
        ReladomoTreeGraphqlConverter reladomoTreeGraphqlConverter,
        Klass klass
    ) {
        String propertyName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, klass.getName());

        ByKeyDataFetcher byKeyDataFetcher = new ByKeyDataFetcher(klass, dataStore, reladomoTreeGraphqlConverter);
        queryTypeBuilder.dataFetcher(propertyName, byKeyDataFetcher);
    }

    private void handleQueryByOperation(
        TypeRuntimeWiring.Builder queryTypeBuilder,
        DomainModel domainModel,
        ReladomoDataStore dataStore,
        ReladomoTreeGraphqlConverter reladomoTreeGraphqlConverter
    ) {
        domainModel
            .getClasses()
            .each((eachKlass) ->
                this.handleQueryByOperation(queryTypeBuilder, dataStore, reladomoTreeGraphqlConverter, eachKlass)
            );
    }

    private void handleQueryByOperation(
        TypeRuntimeWiring.Builder queryTypeBuilder,
        ReladomoDataStore dataStore,
        ReladomoTreeGraphqlConverter reladomoTreeGraphqlConverter,
        Klass klass
    ) {
        String propertyName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, klass.getName()) + "ByOperation";

        ByOperationDataFetcher byOperationDataFetcher = new ByOperationDataFetcher(
            klass,
            dataStore,
            reladomoTreeGraphqlConverter
        );
        queryTypeBuilder.dataFetcher(propertyName, byOperationDataFetcher);
    }

    private void handleQueryByFinder(
        TypeRuntimeWiring.Builder queryTypeBuilder,
        DomainModel domainModel,
        ReladomoDataStore dataStore,
        ReladomoTreeGraphqlConverter reladomoTreeGraphqlConverter
    ) {
        domainModel
            .getClasses()
            .each((eachKlass) ->
                this.handleQueryByFinder(queryTypeBuilder, dataStore, reladomoTreeGraphqlConverter, eachKlass)
            );
    }

    private void handleQueryByFinder(
        TypeRuntimeWiring.Builder queryTypeBuilder,
        ReladomoDataStore dataStore,
        ReladomoTreeGraphqlConverter reladomoTreeGraphqlConverter,
        Klass klass
    ) {
        String propertyName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, klass.getName()) + "ByFinder";

        ByFinderDataFetcher byFinderDataFetcher = new ByFinderDataFetcher(
            klass,
            dataStore,
            reladomoTreeGraphqlConverter
        );
        queryTypeBuilder.dataFetcher(propertyName, byFinderDataFetcher);
    }

    private TypeRuntimeWiring.Builder getTypeResolver(Klass klass) {
        TypeRuntimeWiring.Builder typeBuilder = new TypeRuntimeWiring.Builder();
        typeBuilder.typeName(klass.getName());

        typeBuilder.typeResolver(new KlassTypeResolver(klass));
        return typeBuilder;
    }
}
