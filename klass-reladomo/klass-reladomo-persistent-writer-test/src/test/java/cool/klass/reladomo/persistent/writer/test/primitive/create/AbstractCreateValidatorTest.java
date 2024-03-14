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

package cool.klass.reladomo.persistent.writer.test.primitive.create;

import java.time.Instant;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.node.ObjectNode;
import cool.klass.deserializer.json.JsonTypeCheckingValidator;
import cool.klass.deserializer.json.RequiredPropertiesValidator;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.reladomo.persistent.writer.IncomingCreateDataModelValidator;
import cool.klass.reladomo.persistent.writer.MutationContext;
import cool.klass.reladomo.persistent.writer.test.AbstractValidatorTest;
import io.liftwizard.reladomo.test.rule.ReladomoTestRuleBuilder;
import org.eclipse.collections.api.map.ImmutableMap;
import org.junit.Rule;
import org.junit.rules.TestRule;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractCreateValidatorTest
        extends AbstractValidatorTest
{
    @Rule
    public final TestRule reladomoTestRule = new ReladomoTestRuleBuilder()
            .setRuntimeConfigurationPath("reladomo-runtime-configuration/ReladomoRuntimeConfiguration.xml")
            .setTestDataFileNames("test-data/User.txt")
            .build();

    @Override
    protected final void validate(@Nonnull ObjectNode incomingInstance, Object persistentInstance)
    {
        Klass klass = this.getKlass();
        ImmutableMap<DataTypeProperty, Object> propertyDataFromUrl = this.getPropertyDataFromUrl();
        propertyDataFromUrl.forEachKey(property -> assertThat(property.getOwningClassifier(), sameInstance(klass)));

        JsonTypeCheckingValidator.validate(
                incomingInstance,
                klass,
                this.actualErrors);

        RequiredPropertiesValidator.validate(
                klass,
                incomingInstance,
                this.getMode(),
                this.actualErrors,
                this.actualWarnings);

        MutationContext mutationContext = new MutationContext(
                Optional.of("test user 1"),
                Instant.parse("1999-12-31T23:59:59.999Z"),
                propertyDataFromUrl);

        IncomingCreateDataModelValidator.validate(
                this.reladomoDataStore,
                this.domainModel.getUserClass().get(),
                klass,
                mutationContext,
                incomingInstance,
                this.actualErrors,
                this.actualWarnings);
    }
}
