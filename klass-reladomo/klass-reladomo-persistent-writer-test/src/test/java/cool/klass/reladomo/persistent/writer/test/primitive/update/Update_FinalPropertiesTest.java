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

package cool.klass.reladomo.persistent.writer.test.primitive.update;

import java.io.IOException;

import javax.annotation.Nonnull;

import cool.klass.deserializer.json.OperationMode;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import io.liftwizard.reladomo.test.extension.ReladomoExtensionBuilder;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class Update_FinalPropertiesTest
        extends AbstractUpdateValidatorTest
{
    @RegisterExtension
    public final ReladomoExtensionBuilder reladomoTestExtension = new ReladomoExtensionBuilder()
            .setRuntimeConfigurationPath("reladomo-runtime-configuration/ReladomoRuntimeConfiguration.xml")
            .setTestDataFileNames("test-data/User.txt", "test-data/Update_FinalPropertiesTest.txt");

    private Object persistentInstance;

    @BeforeEach
    void setUp()
    {
        Klass            klass       = this.getKlass();
        DataTypeProperty keyProperty = (DataTypeProperty) klass.getPropertyByName("id").get();

        ImmutableMap<DataTypeProperty, Object> keys = Maps.immutable.with(keyProperty, 1L);

        this.persistentInstance = this.reladomoDataStore.findByKey(klass, keys);
    }

    @Test
    void validate_mutate_final()
            throws IOException
    {
        this.validate("validate_mutate_final", this.persistentInstance);
    }

    @Nonnull
    @Override
    protected Klass getKlass()
    {
        return this.domainModel.getClassByName("FinalProperties");
    }

    @Nonnull
    @Override
    protected OperationMode getMode()
    {
        return OperationMode.REPLACE;
    }

    @Override
    protected ImmutableMap<DataTypeProperty, Object> getPropertyDataFromUrl()
    {
        DataTypeProperty dataTypeProperty = this.getKlass().getDataTypePropertyByName("id");
        return Maps.immutable.with(dataTypeProperty, 1L);
    }
}
