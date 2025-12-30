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

package cool.klass.xample.coverage.dropwizard.test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.liftwizard.junit.extension.match.FileSlurper;
import org.junit.jupiter.api.Test;

class PropertiesOptionalTest extends AbstractCoverageTest {

	@Test
	void getFirst() {
		Client client = this.getClient("getFirst");

		Response response = client
			.target("http://localhost:{port}/api/propertiesOptional/{id}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("id", 1)
			.request()
			.get();

		this.assertResponse("getFirst", Status.OK, response);
	}

	@Test
	void getSecond() {
		Client client = this.getClient("getSecond");

		Response response = client
			.target("http://localhost:{port}/api/propertiesOptional/{id}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("id", 2)
			.request()
			.get();

		this.assertResponse("getSecond", Status.OK, response);
	}

	@Test
	void putFirst() {
		Client client = this.getClient("putFirst");
		String jsonName = this.getClass().getSimpleName() + ".putFirst.json5";
		String json = FileSlurper.slurp(jsonName, this.getClass());

		Response putResponse = client
			.target("http://localhost:{port}/api/propertiesOptional/{id}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("id", 1)
			.request()
			.header("Authorization", "Impersonation User userId 1")
			.put(Entity.json(json));

		this.assertEmptyResponse(Status.NO_CONTENT, putResponse);

		Response getResponse = client
			.target("http://localhost:{port}/api/propertiesOptional/{id}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("id", 1)
			.request()
			.get();

		this.assertResponse("putFirst2", Status.OK, getResponse);
	}
}
