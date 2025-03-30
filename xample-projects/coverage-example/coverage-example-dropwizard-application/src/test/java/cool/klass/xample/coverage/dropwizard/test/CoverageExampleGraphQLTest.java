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

package cool.klass.xample.coverage.dropwizard.test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.liftwizard.junit.extension.match.FileSlurper;
import org.eclipse.collections.impl.factory.Maps;
import org.junit.jupiter.api.Test;

class CoverageExampleGraphQLTest extends AbstractCoverageTest {

    @Test
    void graphqlSmokeTest() {
        Client client = this.getClient("graphqlSmokeTest");
        String graphqlQueryName = this.getClass().getSimpleName() + ".graphqlSmokeTest.graphql";
        String graphqlQuery = FileSlurper.slurp(graphqlQueryName, this.getClass());

        Response response = client
            .target("http://localhost:{port}/graphql")
            .resolveTemplate("port", this.appExtension.getLocalPort())
            .request()
            .post(Entity.json(Maps.mutable.with("query", graphqlQuery)));

        this.assertResponse("graphqlSmokeTest", Status.OK, response);
    }
}
