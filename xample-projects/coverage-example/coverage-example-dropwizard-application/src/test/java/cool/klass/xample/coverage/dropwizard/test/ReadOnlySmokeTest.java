/*
 * Copyright 2026 Craig Motlin
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.liftwizard.junit.extension.match.FileSlurper;
import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import org.eclipse.collections.api.factory.Maps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadOnlySmokeTest extends AbstractCoverageTest {

	@RegisterExtension
	final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

	@Test
	void readOnlyEndpoints() throws Exception {
		this.swagger_spec_is_served();
		this.smoke_test();
		this.graphqlSmokeTest();
	}

	private void swagger_spec_is_served() throws Exception {
		Client client = this.getClient("swagger_spec_is_served");

		Response response = client
			.target("http://localhost:{port}/api/swagger.json")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request(MediaType.APPLICATION_JSON_TYPE)
			.header("Authorization", "Impersonation User ID")
			.get();

		this.assertResponseStatus(response, Status.OK);

		String responseJson = response.readEntity(String.class);
		assertNotNull(responseJson, "Swagger spec should not be null");
		assertTrue(responseJson.length() > 100, "Swagger spec should have meaningful content");

		// Parse and validate it's valid JSON with expected Swagger structure
		var objectMapper = new ObjectMapper();
		JsonNode swaggerSpec = objectMapper.readTree(responseJson);

		assertNotNull(swaggerSpec.get("swagger"), "Should have swagger version");
		assertNotNull(swaggerSpec.get("info"), "Should have info section");
		assertNotNull(swaggerSpec.get("paths"), "Should have paths section");

		// Verify it's Swagger 2.0 format
		assertEquals("2.0", swaggerSpec.get("swagger").asText(), "Should be Swagger 2.0");
	}

	private void smoke_test() {
		Client client = this.getClient("smoke_test");

		Response response = client
			.target("http://localhost:{port}/api/meta/code/element/{topLevelElementName}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("topLevelElementName", "User")
			.request(MediaType.TEXT_HTML_TYPE)
			.header("Authorization", "Impersonation User ID")
			.get();

		this.assertResponseStatus(response, Status.OK);
		String responseHtml = response.readEntity(String.class);
		// assertEquals(responseHtml, expected, responseHtml);

		String expectedStringClasspathLocation = this.getClass().getCanonicalName() + "#smoke_test.html";
		this.fileMatchExtension.assertFileContents(expectedStringClasspathLocation, responseHtml);
	}

	private void graphqlSmokeTest() {
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
