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
import org.junit.jupiter.api.Test;

class EveryTypeKeyPropertyTest extends AbstractCoverageTest {

    @Test
    void get() {
        Client client = this.getClient("get");

        Response response = client
            .target("http://localhost:{port}/api/everyTypeKeyProperty")
            .resolveTemplate("port", this.appExtension.getLocalPort())
            .request()
            .get();

        this.assertResponse("get", Status.OK, response);
    }

    @Test
    void post() {
        Client client = this.getClient("post");
        String jsonName = this.getClass().getSimpleName() + ".post.json5";
        String json = FileSlurper.slurp(jsonName, this.getClass());

        Response response = client
            .target("http://localhost:{port}/api/everyTypeKeyProperty")
            .resolveTemplate("port", this.appExtension.getLocalPort())
            .request()
            .post(Entity.json(json));

        this.assertResponse("post", Status.METHOD_NOT_ALLOWED, response);
    }

    @Test
    void put() {
        Client client = this.getClient("put");

        {
            Response response = client
                .target(
                    "http://localhost:{port}/api/everyTypeKeyProperty/EveryTypeKeyProperty keyString 1 ☝/1/1/1.0123456789/1.0123457/true/1999-12-31T23:59:00Z/1999-12-31"
                )
                .resolveTemplate("port", this.appExtension.getLocalPort())
                .request()
                .get();

            this.assertResponse("put1", Status.OK, response);
        }

        {
            String jsonName = this.getClass().getSimpleName() + ".put.json5";
            String json = FileSlurper.slurp(jsonName, this.getClass());

            Response response = client
                .target(
                    "http://localhost:{port}/api/everyTypeKeyProperty/EveryTypeKeyProperty keyString 1 ☝/1/1/1.0123456789/1.0123457/true/1999-12-31T23:59:00Z/1999-12-31"
                )
                .resolveTemplate("port", this.appExtension.getLocalPort())
                .request()
                .put(Entity.json(json));

            this.assertEmptyResponse(Status.NO_CONTENT, response);
        }

        Response response = client
            .target(
                "http://localhost:{port}/api/everyTypeKeyProperty/EveryTypeKeyProperty keyString 1 ☝/1/1/1.0123456789/1.0123457/true/1999-12-31T23:59:00Z/1999-12-31"
            )
            .resolveTemplate("port", this.appExtension.getLocalPort())
            .request()
            .get();

        this.assertResponse("put3", Status.OK, response);
    }
}
