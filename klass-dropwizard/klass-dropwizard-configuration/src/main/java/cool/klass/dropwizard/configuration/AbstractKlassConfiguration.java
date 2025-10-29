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

package cool.klass.dropwizard.configuration;

import java.util.List;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import cool.klass.dropwizard.configuration.data.store.DataStoreFactory;
import cool.klass.dropwizard.configuration.data.store.DataStoreFactoryProvider;
import cool.klass.dropwizard.configuration.domain.model.loader.DomainModelFactory;
import cool.klass.dropwizard.configuration.domain.model.loader.DomainModelFactoryProvider;
import cool.klass.dropwizard.configuration.sample.data.SampleDataFactory;
import cool.klass.dropwizard.configuration.sample.data.SampleDataFactoryProvider;
import io.liftwizard.dropwizard.configuration.connectionmanager.ConnectionManagerProvider;
import io.liftwizard.dropwizard.configuration.connectionmanager.ConnectionManagersFactory;
import io.liftwizard.dropwizard.configuration.datasource.NamedDataSourceProvider;
import io.liftwizard.dropwizard.configuration.datasource.NamedDataSourcesFactory;
import io.liftwizard.dropwizard.configuration.ddl.executor.DdlExecutorFactory;
import io.liftwizard.dropwizard.configuration.ddl.executor.DdlExecutorFactoryProvider;
import io.liftwizard.dropwizard.configuration.enabled.EnabledFactory;
import io.liftwizard.dropwizard.configuration.h2.H2Factory;
import io.liftwizard.dropwizard.configuration.h2.H2FactoryProvider;
import io.liftwizard.dropwizard.configuration.liquibase.migration.LiquibaseMigrationFactory;
import io.liftwizard.dropwizard.configuration.liquibase.migration.LiquibaseMigrationFactoryProvider;
import io.liftwizard.dropwizard.configuration.parent.AbstractLiftwizardConfiguration;
import io.liftwizard.dropwizard.configuration.reladomo.ReladomoFactory;
import io.liftwizard.dropwizard.configuration.reladomo.ReladomoFactoryProvider;
import io.liftwizard.dropwizard.configuration.uuid.UUIDSupplierFactory;
import io.liftwizard.dropwizard.configuration.uuid.UUIDSupplierFactoryProvider;

@JsonPropertyOrder(
    {
        "server",
        "logging",
        "metrics",
        "klass",
        "configLogging",
        "objectMapper",
        "cors",
        "authFilters",
        "jerseyHttpLogging",
        "h2",
        "dataSources",
        "ddlExecutors",
        "reladomo",
        "connectionManagers",
        "sampleData",
        "bootstrap",
    }
)
public abstract class AbstractKlassConfiguration
    extends AbstractLiftwizardConfiguration
    implements
        H2FactoryProvider,
        DdlExecutorFactoryProvider,
        ReladomoFactoryProvider,
        SampleDataFactoryProvider,
        DataStoreFactoryProvider,
        DomainModelFactoryProvider,
        UUIDSupplierFactoryProvider,
        NamedDataSourceProvider,
        ConnectionManagerProvider,
        LiquibaseMigrationFactoryProvider {

    private @Valid KlassFactory klassFactory;

    // Data
    private @Valid H2Factory h2Factory;
    private @Valid @NotNull List<DdlExecutorFactory> ddlExecutorFactories = List.of();
    private @Valid @NotNull ReladomoFactory reladomoFactory = new ReladomoFactory();
    private @Valid @NotNull SampleDataFactory sampleDataFactory = new SampleDataFactory();
    private @Valid @NotNull EnabledFactory bootstrapFactory = new EnabledFactory();
    private @Valid @NotNull LiquibaseMigrationFactory liquibaseMigrationFactory = new LiquibaseMigrationFactory();

    @JsonUnwrapped
    private @Valid @NotNull NamedDataSourcesFactory namedDataSourcesFactory = new NamedDataSourcesFactory();

    @JsonUnwrapped
    private @Valid @NotNull ConnectionManagersFactory connectionManagersFactory = new ConnectionManagersFactory();

    @JsonProperty("klass")
    public KlassFactory getKlassFactory() {
        return this.klassFactory;
    }

    @JsonProperty("klass")
    public void setKlassFactory(KlassFactory klassFactory) {
        this.klassFactory = klassFactory;
    }

    // TODO: Error messages contain the word factory because of the field name
    // config-test.json5 has the following errors:
    //   * h2Factory.tcpPort may not be null
    //   * h2Factory.webPort may not be null
    @Override
    @JsonProperty("h2")
    public H2Factory getH2Factory() {
        return this.h2Factory;
    }

    @JsonProperty("h2")
    public void setH2(H2Factory h2Factory) {
        this.h2Factory = h2Factory;
    }

    @JsonProperty("dataSources")
    @JsonUnwrapped
    @Override
    public NamedDataSourcesFactory getNamedDataSourcesFactory() {
        return this.namedDataSourcesFactory;
    }

    @JsonProperty("dataSources")
    @JsonUnwrapped
    public void setNamedDataSourcesFactory(NamedDataSourcesFactory namedDataSourcesFactory) {
        this.namedDataSourcesFactory = namedDataSourcesFactory;
    }

    @JsonProperty("connectionManagers")
    @JsonUnwrapped
    @Override
    public ConnectionManagersFactory getConnectionManagersFactory() {
        return this.connectionManagersFactory;
    }

    @JsonProperty("connectionManagers")
    @JsonUnwrapped
    public void setConnectionManagersFactory(ConnectionManagersFactory connectionManagersFactory) {
        this.connectionManagersFactory = connectionManagersFactory;
    }

    @Override
    @JsonProperty("ddlExecutors")
    public List<DdlExecutorFactory> getDdlExecutorFactories() {
        return this.ddlExecutorFactories;
    }

    @JsonProperty("ddlExecutors")
    public void setDdlExecutorFactories(List<DdlExecutorFactory> ddlExecutorFactories) {
        this.ddlExecutorFactories = ddlExecutorFactories;
    }

    @Override
    @JsonProperty("reladomo")
    public ReladomoFactory getReladomoFactory() {
        return this.reladomoFactory;
    }

    @JsonProperty("reladomo")
    public void setReladomo(ReladomoFactory reladomoFactory) {
        this.reladomoFactory = reladomoFactory;
    }

    @Override
    @JsonProperty("sampleData")
    public SampleDataFactory getSampleDataFactory() {
        return this.sampleDataFactory;
    }

    @JsonProperty("sampleData")
    public void setSampleData(SampleDataFactory sampleDataFactory) {
        this.sampleDataFactory = sampleDataFactory;
    }

    @JsonProperty("bootstrap")
    public EnabledFactory getBootstrapFactory() {
        return this.bootstrapFactory;
    }

    @JsonProperty("bootstrap")
    public void setBootstrap(EnabledFactory bootstrapFactory) {
        this.bootstrapFactory = bootstrapFactory;
    }

    @JsonProperty("liquibase")
    @Override
    public LiquibaseMigrationFactory getLiquibaseMigrationFactory() {
        return this.liquibaseMigrationFactory;
    }

    @JsonProperty("liquibase")
    public void setLiquibaseMigrationFactory(LiquibaseMigrationFactory liquibaseMigrationFactory) {
        this.liquibaseMigrationFactory = liquibaseMigrationFactory;
    }

    @Override
    @JsonIgnore
    public DataStoreFactory getDataStoreFactory() {
        return this.klassFactory.getDataStoreFactory();
    }

    @Override
    @JsonIgnore
    public DomainModelFactory getDomainModelFactory() {
        return this.klassFactory.getDomainModelFactory();
    }

    @JsonIgnore
    @Override
    public UUIDSupplierFactory getUuidSupplierFactory() {
        return this.getDataStoreFactory().getUuidFactory();
    }

    @JsonIgnore
    @Nullable
    public String getColorScheme() {
        return this.klassFactory == null ? null : this.klassFactory.getColorScheme();
    }
}
