package ${package}.dropwizard.application;

import javax.annotation.Nonnull;
import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smoketurner.dropwizard.graphql.GraphQLFactory;
import cool.klass.dropwizard.configuration.AbstractKlassConfiguration;
import io.liftwizard.dropwizard.configuration.graphql.GraphQLFactoryProvider;
import io.liftwizard.servlet.config.spa.SinglePageRedirectFilterFactory;
import io.liftwizard.servlet.config.spa.SinglePageRedirectFilterFactoryProvider;

public class ${name}Configuration
        extends AbstractKlassConfiguration
        implements GraphQLFactoryProvider,
        SinglePageRedirectFilterFactoryProvider
{
    @Nonnull
    private @Valid GraphQLFactory graphQL = new GraphQLFactory();

    private SinglePageRedirectFilterFactory singlePageRedirectFilterFactory = new SinglePageRedirectFilterFactory();

    @Override
    @Nonnull
    @JsonProperty("graphQL")
    public GraphQLFactory getGraphQLFactory()
    {
        return this.graphQL;
    }

    @JsonProperty("graphQL")
    public void setGraphQLFactory(@Nonnull GraphQLFactory factory)
    {
        this.graphQL = factory;
    }

    @Override
    @JsonProperty("singlePageRedirectFilter")
    public SinglePageRedirectFilterFactory getSinglePageRedirectFilterFactory()
    {
        return this.singlePageRedirectFilterFactory;
    }

    @JsonProperty("singlePageRedirectFilter")
    public void setSinglePageRedirectFilterFactory(SinglePageRedirectFilterFactory singlePageRedirectFilterFactory)
    {
        this.singlePageRedirectFilterFactory = singlePageRedirectFilterFactory;
    }
}
