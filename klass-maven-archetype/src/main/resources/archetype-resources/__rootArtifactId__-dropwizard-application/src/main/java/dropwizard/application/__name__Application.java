package ${package}.dropwizard.application;

import java.util.ServiceLoader;

import javax.annotation.Nonnull;

import cool.klass.data.store.reladomo.ReladomoDataStore;
import cool.klass.dropwizard.bundle.test.data.TestDataGeneratorBundle;
import cool.klass.model.converter.bootstrap.writer.KlassBootstrapWriter;
import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ${name}Application extends Abstract${name}Application
{
    public static void main(String[] args) throws Exception
    {
        new ${name}Application().run(args);
    }

    @Override
    public void initialize(@Nonnull Bootstrap<${name}Configuration> bootstrap)
    {
        super.initialize(bootstrap);

        ServiceLoader<Bundle> bundleServiceLoader = ServiceLoader.load(Bundle.class);
        for (Bundle bundle : bundleServiceLoader)
        {
            bootstrap.addBundle(bundle);
        }

        bootstrap.addBundle(new TestDataGeneratorBundle(this.domainModel, this.dataStore));

        // TODO: application initialization
    }

    @Override
    public void run(
            ${name}Configuration configuration,
            @Nonnull Environment environment)
    {
        super.run(configuration, environment);

        // TODO: Move up to generated superclass
        KlassBootstrapWriter klassBootstrapWriter = new KlassBootstrapWriter(this.domainModel);
        klassBootstrapWriter.bootstrapMetaModel();
    }
}
