package klass.model.meta.domain.dropwizard.test;

import cool.klass.data.store.DataStore;
import cool.klass.dropwizard.configuration.KlassFactory;
import cool.klass.model.converter.bootstrap.writer.KlassBootstrapWriter;
import cool.klass.model.meta.domain.api.DomainModel;
import org.junit.Before;

public class AbstractBootstrappedResourceTestCase
        extends AbstractResourceTestCase
{
    @Before
    public void bootstrap()
    {
        KlassFactory klassFactory = this.appRule.getConfiguration().getKlassFactory();
        DataStore    dataStore    = klassFactory.getDataStoreFactory().createDataStore();
        DomainModel  domainModel  = klassFactory.getDomainModelFactory().createDomainModel();

        KlassBootstrapWriter klassBootstrapWriter = new KlassBootstrapWriter(domainModel, dataStore);
        klassBootstrapWriter.bootstrapMetaModel();
    }
}