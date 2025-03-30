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

import javax.ws.rs.client.Client;

import org.junit.jupiter.api.Test;

class ClassResourceManualTest extends AbstractResourceTestCase {

    @Test
    void getAllMeta() {
        Class<?> klass = this.getClass();
        String clientName = klass.getPackage().getName() + '.' + klass.getSimpleName() + '.' + "getAllMeta";
        Client client = this.getClient(clientName);
        String resourceClassPathLocation = klass.getSimpleName() + '.' + "getAllMeta" + ".json";

        this.assertUrlReturns(client, "/meta/class", resourceClassPathLocation);
    }
}
