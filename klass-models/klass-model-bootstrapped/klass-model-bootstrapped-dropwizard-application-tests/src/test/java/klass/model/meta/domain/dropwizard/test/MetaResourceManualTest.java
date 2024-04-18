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

package klass.model.meta.domain.dropwizard.test;

import org.junit.jupiter.api.Test;

class MetaResourceManualTest
        extends AbstractResourceTestCase
{
    @Test
    void metaEnumeration()
    {
        this.assertUrlReturns("metaEnumeration", "/meta/enumeration/PrimitiveType");
    }

    @Test
    void metaInterface()
    {
        this.assertUrlReturns("metaInterface", "/meta/interface/NamedElement");
    }

    @Test
    void metaClass()
    {
        this.assertUrlReturns("metaClass", "/meta/class/Classifier");
    }

    @Test
    void metaAssociation()
    {
        this.assertUrlReturns("metaAssociation", "/meta/association/DataTypePropertyHasModifiers");
    }

    @Test
    void metaProjection()
    {
        this.assertUrlReturns("metaProjection", "/meta/projection/ProjectionElementProjection");
    }

    @Test
    void metaServiceGroup()
    {
        this.assertUrlReturns("metaServiceGroup", "/meta/serviceGroup/ServiceGroupResource");
    }
}
