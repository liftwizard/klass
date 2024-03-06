package com.stackoverflow.dropwizard.application;

import java.util.ServiceLoader;

import javax.annotation.Nonnull;

import com.stackoverflow.service.resource.QuestionResourceManual;
import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class StackOverflowApplication extends AbstractStackOverflowApplication
{
    public static void main(String[] args) throws Exception
    {
        new StackOverflowApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<StackOverflowConfiguration> bootstrap)
    {
        super.initialize(bootstrap);

        ServiceLoader<Bundle> bundleServiceLoader = ServiceLoader.load(Bundle.class);
        for (Bundle bundle : bundleServiceLoader)
        {
            bootstrap.addBundle(bundle);
        }

        // TODO: application initialization
    }

    @Override
    public void run(
            StackOverflowConfiguration configuration,
            @Nonnull Environment environment)
    {
        super.run(configuration, environment);

        environment.jersey().register(new QuestionResourceManual(this.domainModel));
    }
}
