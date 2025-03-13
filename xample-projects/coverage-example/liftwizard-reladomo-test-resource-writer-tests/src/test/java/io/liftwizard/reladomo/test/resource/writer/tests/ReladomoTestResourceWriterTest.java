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

package io.liftwizard.reladomo.test.resource.writer.tests;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import cool.klass.dropwizard.configuration.domain.model.loader.compiler.DomainModelCompilerFactory;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.NamedElement;
import io.liftwizard.junit.extension.liquibase.migrations.LiquibaseTestExtension;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import io.liftwizard.reladomo.test.extension.ReladomoInitializeExtension;
import io.liftwizard.reladomo.test.extension.ReladomoLoadDataExtension;
import io.liftwizard.reladomo.test.extension.ReladomoTestFile;
import io.liftwizard.reladomo.test.resource.writer.ReladomoTestResourceWriter;
import io.liftwizard.serialization.jackson.config.ObjectMapperConfig;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(LogMarkerTestExtension.class)
public class ReladomoTestResourceWriterTest
{
    @Order(1)
    @RegisterExtension
    final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

    @Order(2)
    @RegisterExtension
    private final LiquibaseTestExtension liquibaseTestExtension = new LiquibaseTestExtension(
            "cool/klass/xample/coverage/migrations.xml");

    @Order(3)
    @RegisterExtension
    private final ReladomoInitializeExtension initializeTestExtension =
            new ReladomoInitializeExtension("reladomo-runtime-configuration/ReladomoRuntimeConfiguration.xml");

    @Order(4)
    @RegisterExtension
    private final ReladomoLoadDataExtension loadDataTestExtension = new ReladomoLoadDataExtension();

    private final ObjectMapper objectMapper = ObjectMapperConfig.configure(new ObjectMapper());
    private final DomainModel  domainModel  = getDomainModel(this.objectMapper);

    @Test
    @ReladomoTestFile("test-data/ReladomoTestResourceWriterTest.txt")
    void reladomoTestResourceWriter()
    {
        ImmutableList<String> classNames = this.domainModel.getClasses().collect(NamedElement::getName);
        String                actual     = ReladomoTestResourceWriter.generate(classNames);

        String resourceClassPathLocation = this.getClass().getSimpleName() + ".reladomoTestResourceWriter.txt";
        this.fileMatchExtension.assertFileContents(resourceClassPathLocation, actual);
    }

    private static DomainModel getDomainModel(ObjectMapper objectMapper)
    {
        DomainModelCompilerFactory domainModelCompilerFactory = new DomainModelCompilerFactory();
        domainModelCompilerFactory.setSourcePackages(List.of("cool.klass.xample.coverage"));
        domainModelCompilerFactory.setColorScheme("dark");
        return domainModelCompilerFactory.createDomainModel(objectMapper);
    }
}
