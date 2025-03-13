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

package cool.klass.reladomo.persistent.writer.test;

import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cool.klass.data.store.reladomo.ReladomoDataStore;
import cool.klass.deserializer.json.OperationMode;
import cool.klass.dropwizard.configuration.domain.model.loader.compiler.DomainModelCompilerFactory;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.reladomo.persistent.writer.IncomingCreateDataModelValidator;
import io.dropwizard.jackson.Jackson;
import io.liftwizard.dropwizard.configuration.uuid.seed.SeedUUIDSupplier;
import io.liftwizard.junit.extension.liquibase.migrations.LiquibaseTestExtension;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import io.liftwizard.junit.extension.match.json.JsonMatchExtension;
import io.liftwizard.serialization.jackson.config.ObjectMapperConfig;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.jupiter.api.extension.RegisterExtension;

public abstract class AbstractValidatorTest
{
    @RegisterExtension
    protected final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

    @RegisterExtension
    protected final LogMarkerTestExtension logMarkerTestExtension = new LogMarkerTestExtension();

    @RegisterExtension
    protected final JsonMatchExtension jsonMatchExtension = new JsonMatchExtension(this.getClass());

    @RegisterExtension
    protected final LiquibaseTestExtension liquibaseTestExtension = new LiquibaseTestExtension(
            "cool/klass/reladomo/persistent/writer/test/migrations.xml");

    protected final MutableList<String> actualErrors      = Lists.mutable.empty();
    protected final MutableList<String> actualWarnings    = Lists.mutable.empty();
    protected final ReladomoDataStore   reladomoDataStore = this.getReladomoDataStore();
    protected final ObjectMapper        objectMapper      = AbstractValidatorTest.getObjectMapper();
    protected final DomainModel         domainModel       = AbstractValidatorTest.getDomainModel(this.objectMapper);

    protected void validate(String testName)
            throws JsonProcessingException
    {
        this.validate(testName, null);
    }

    protected void validate(String testName, Object persistentInstance)
            throws JsonProcessingException
    {
        String incomingJsonName = this.getClass().getSimpleName() + '.' + testName + ".json5";
        String incomingJson     = FileSlurper.slurp(incomingJsonName, this.getClass());

        ObjectNode incomingInstance = (ObjectNode) this.objectMapper.readTree(incomingJson);
        this.validate(incomingInstance, persistentInstance);

        this.jsonMatchExtension.assertFileContents(
                this.getClass().getSimpleName() + '.' + testName + ".errors.json",
                this.objectMapper.writeValueAsString(this.actualErrors));

        this.jsonMatchExtension.assertFileContents(
                this.getClass().getSimpleName() + '.' + testName + ".warnings.json",
                this.objectMapper.writeValueAsString(this.actualWarnings));
    }

    protected abstract void validate(@Nonnull ObjectNode incomingInstance, Object persistentInstance);

    @Nonnull
    private static ObjectMapper getObjectMapper()
    {
        ObjectMapper objectMapper = Jackson.newObjectMapper();
        ObjectMapperConfig.configure(objectMapper);
        return objectMapper;
    }

    @Nonnull
    protected abstract Klass getKlass();

    @Nonnull
    protected abstract OperationMode getMode();

    @Nonnull
    private ReladomoDataStore getReladomoDataStore()
    {
        String           seed         = IncomingCreateDataModelValidator.class.getSimpleName();
        SeedUUIDSupplier uuidSupplier = new SeedUUIDSupplier(seed);
        return new ReladomoDataStore(uuidSupplier, 1);
    }

    private static DomainModel getDomainModel(ObjectMapper objectMapper)
    {
        DomainModelCompilerFactory domainModelCompilerFactory = new DomainModelCompilerFactory();
        domainModelCompilerFactory.setSourcePackages(List.of("cool.klass.xample.coverage"));
        domainModelCompilerFactory.setColorScheme("dark");
        return domainModelCompilerFactory.createDomainModel(objectMapper);
    }

    protected ImmutableMap<DataTypeProperty, Object> getPropertyDataFromUrl()
    {
        return Maps.immutable.empty();
    }
}
