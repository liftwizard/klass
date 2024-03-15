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

package cool.klass.dropwizard.configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import cool.klass.dropwizard.configuration.data.store.DataStoreFactory;
import cool.klass.dropwizard.configuration.data.store.reladomo.ReladomoDataStoreFactory;
import cool.klass.dropwizard.configuration.domain.model.loader.DomainModelFactory;
import io.liftwizard.dropwizard.configuration.clock.ClockFactory;
import io.liftwizard.dropwizard.configuration.clock.fixed.FixedClockFactory;

public class KlassFactory
{
    private @NotNull @Valid ClockFactory       clockFactory       = new FixedClockFactory();
    private @NotNull @Valid DomainModelFactory domainModelFactory;
    private @NotNull @Valid DataStoreFactory   dataStoreFactory   = new ReladomoDataStoreFactory();

    @JsonProperty("clock")
    public ClockFactory getClockFactory()
    {
        return this.clockFactory;
    }

    @JsonProperty("clock")
    public void setClockFactory(ClockFactory clockFactory)
    {
        this.clockFactory = clockFactory;
    }

    @JsonProperty("domainModel")
    public DomainModelFactory getDomainModelFactory()
    {
        return this.domainModelFactory;
    }

    @JsonProperty("domainModel")
    public void setDomainModelFactory(DomainModelFactory domainModelFactory)
    {
        this.domainModelFactory = domainModelFactory;
    }

    @JsonProperty("dataStore")
    public DataStoreFactory getDataStoreFactory()
    {
        return this.dataStoreFactory;
    }

    @JsonProperty("dataStore")
    public void setDataStoreFactory(DataStoreFactory dataStoreFactory)
    {
        this.dataStoreFactory = dataStoreFactory;
    }
}
