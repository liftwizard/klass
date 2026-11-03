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

class CoverageExampleApplicationTest extends AbstractCoverageTest {

	private static final String EVERY_TYPE_KEY_PROPERTY_URL =
		"http://localhost:{port}/api/everyTypeKeyProperty/EveryTypeKeyProperty keyString 1 ☝/1/1/1.0123456789/1.0123457/true/1999-12-31T23:59:00Z/1999-12-31";

	@RegisterExtension
	final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

	@Test
	void readsThenWrites() throws Exception {
		this.swaggerSpecIsServed();
		this.sourceCodeSmokeTest();
		this.graphqlSmokeTest();

		this.assertMetaUrlReturns("metaModel.getPackageableElements", "packageableElement");
		this.assertMetaUrlReturns("metaModel.getPackageableElementByName", "packageableElement/User");
		this.assertMetaUrlReturns("metaModel.getClasses", "class");
		this.assertMetaUrlReturns("metaModel.getClassByName", "class/User");
		this.assertMetaUrlReturns("metaModel.getEnumerations", "enumeration");
		this.assertMetaUrlReturns("metaModel.getEnumerationByName", "enumeration/Multiplicity");
		this.assertMetaUrlReturns("metaModel.getAssociations", "association");
		this.assertMetaUrlReturns(
			"metaModel.getAssociationByName",
			"association/EveryTypeKeyPropertyHasEveryTypeForeignKeyProperty"
		);
		this.assertMetaUrlReturns("metaModel.getClassifiers", "classifier");
		this.assertMetaUrlReturns("metaModel.getClassifierByName", "classifier/User");
		this.assertMetaUrlReturns("metaModel.getInterfaces", "interface");
		this.assertMetaUrlReturns("metaModel.getInterfaceByName", "interface/NamedElement");
		this.assertMetaUrlReturns("metaModel.getProjections", "projection");
		this.assertMetaUrlReturns("metaModel.getProjectionByName", "projection/UserProjection");
		this.assertMetaUrlReturns("metaModel.getServiceGroups", "serviceGroup");
		this.assertMetaUrlReturns("metaModel.getServiceGroupByName", "serviceGroup/UserResource");

		this.assertUrlReturns("propertiesOptional.getFirst", "propertiesOptional/1");
		this.assertUrlReturns("propertiesOptional.getSecond", "propertiesOptional/2");
		this.assertUrlReturns("propertiesRequired.getFirst", "propertiesRequired/1");
		this.assertUrlReturns("propertiesRequired.getSecond", "propertiesRequired/2");
		this.assertUrlReturns("everyTypeKeyProperty.get", "everyTypeKeyProperty");

		this.propertiesOptionalPutFirst();
		this.propertiesRequiredPutFirst();
		this.everyTypeKeyPropertyPut();
		this.everyTypeKeyPropertyPostWithProjection("postSingleWithProjection", "singleWithProjection");
		this.everyTypeKeyPropertyPostWithoutBody("postMultiple", "multiple");
		this.everyTypeKeyPropertyPostWithProjection("postMultipleWithProjection", "multipleWithProjection");
		this.everyTypeKeyPropertyPostWithoutBody("postSingle", "single");
	}

	private void swaggerSpecIsServed() throws Exception {
		Client client = this.getClient("readOnlySmoke.swagger_spec_is_served");

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

	private void sourceCodeSmokeTest() {
		Client client = this.getClient("readOnlySmoke.smoke_test");

		Response response = client
			.target("http://localhost:{port}/api/meta/code/element/{topLevelElementName}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("topLevelElementName", "User")
			.request(MediaType.TEXT_HTML_TYPE)
			.header("Authorization", "Impersonation User ID")
			.get();

		this.assertResponseStatus(response, Status.OK);
		String responseHtml = response.readEntity(String.class);

		String expectedStringClasspathLocation = this.getClass().getCanonicalName() + "#readOnlySmoke.smoke_test.html";
		this.fileMatchExtension.assertFileContents(expectedStringClasspathLocation, responseHtml);
	}

	private void graphqlSmokeTest() {
		var testName = "readOnlySmoke.graphqlSmokeTest";
		Client client = this.getClient(testName);
		String graphqlQuery = this.slurp(testName + ".graphql");

		Response response = client
			.target("http://localhost:{port}/graphql")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request()
			.post(Entity.json(Maps.mutable.with("query", graphqlQuery)));

		this.assertResponse(testName, Status.OK, response);
	}

	private void assertMetaUrlReturns(String testName, String path) {
		Client client = this.getClient(testName);

		Response response = client
			.target("http://localhost:{port}/api/meta/" + path)
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponse(testName, Status.OK, response);
	}

	private void propertiesOptionalPutFirst() {
		Client client = this.getClient("propertiesOptional.putFirst");

		Response putResponse = this.putPropertiesById(
			client,
			"propertiesOptional",
			this.slurp("propertiesOptional.putFirst.json5")
		);
		this.assertEmptyResponse(Status.NO_CONTENT, putResponse);

		Response getResponse = this.getPropertiesById(client, "propertiesOptional");
		this.assertResponse("propertiesOptional.putFirst2", Status.OK, getResponse);

		// Now PUT with all optional primitives set to null
		Response putNullResponse = this.putPropertiesById(
			client,
			"propertiesOptional",
			this.slurp("propertiesOptional.putFirstNull.json5")
		);
		this.assertEmptyResponse(Status.NO_CONTENT, putNullResponse);

		Response getNullResponse = this.getPropertiesById(client, "propertiesOptional");
		this.assertResponse("propertiesOptional.putFirstNull2", Status.OK, getNullResponse);
	}

	private void propertiesRequiredPutFirst() {
		Client client = this.getClient("propertiesRequired.putFirst");

		Response putResponse = this.putPropertiesById(
			client,
			"propertiesRequired",
			this.slurp("propertiesRequired.putFirst.json5")
		);
		this.assertEmptyResponse(Status.NO_CONTENT, putResponse);

		Response getResponse = this.getPropertiesById(client, "propertiesRequired");
		this.assertResponse("propertiesRequired.putFirst2", Status.OK, getResponse);
	}

	private Response putPropertiesById(Client client, String resource, String json) {
		return client
			.target("http://localhost:{port}/api/" + resource + "/{id}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("id", 1)
			.request()
			.header("Authorization", "Impersonation User userId 1")
			.put(Entity.json(json));
	}

	private Response getPropertiesById(Client client, String resource) {
		return client
			.target("http://localhost:{port}/api/" + resource + "/{id}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("id", 1)
			.request()
			.get();
	}

	private void everyTypeKeyPropertyPut() {
		Client client = this.getClient("everyTypeKeyProperty.put");

		Response getBeforeResponse = client
			.target(EVERY_TYPE_KEY_PROPERTY_URL)
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request()
			.get();
		this.assertResponse("everyTypeKeyProperty.put1", Status.OK, getBeforeResponse);

		Response putResponse = client
			.target(EVERY_TYPE_KEY_PROPERTY_URL)
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request()
			.header("Authorization", "Impersonation User userId 1")
			.put(Entity.json(this.slurp("everyTypeKeyProperty.put.json5")));
		this.assertEmptyResponse(Status.NO_CONTENT, putResponse);

		Response getAfterResponse = client
			.target(EVERY_TYPE_KEY_PROPERTY_URL)
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request()
			.get();
		this.assertResponse("everyTypeKeyProperty.put3", Status.OK, getAfterResponse);
	}

	private void everyTypeKeyPropertyPostWithProjection(String name, String path) {
		String testName = "everyTypeKeyProperty." + name;
		Response response = this.everyTypeKeyPropertyPost(testName, path);
		this.assertResponse(testName, Status.CREATED, response);
	}

	private void everyTypeKeyPropertyPostWithoutBody(String name, String path) {
		String testName = "everyTypeKeyProperty." + name;
		Response response = this.everyTypeKeyPropertyPost(testName, path);
		this.assertEmptyResponse(Status.NO_CONTENT, response);
	}

	private Response everyTypeKeyPropertyPost(String testName, String path) {
		return this.getClient(testName)
			.target("http://localhost:{port}/api/everyTypeKeyProperty/" + path)
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request()
			.header("Authorization", "Impersonation test-user")
			.post(Entity.json(this.slurp(testName + ".json5")));
	}

	private String slurp(String suffix) {
		return FileSlurper.slurp(this.getClass().getSimpleName() + '.' + suffix, this.getClass());
	}
}
