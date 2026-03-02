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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractMetaModelWriteResourceTest extends AbstractCoverageTest {

	@Override
	protected String getGoldenFilePrefix() {
		return "MetaModelWriteResourceTest";
	}

	@Test
	void deleteServiceGroupByNameNotFound() {
		Client client = this.getClient("deleteServiceGroupByNameNotFound");

		Response response = client
			.target("http://localhost:{port}/api/meta/serviceGroup/{name}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("name", "NonExistent")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.header("Authorization", "Impersonation test-user")
			.delete();

		assertThat(response.getStatus()).isEqualTo(Status.GONE.getStatusCode());
	}

	@Test
	void deleteProjectionByNameNotFound() {
		Client client = this.getClient("deleteProjectionByNameNotFound");

		Response response = client
			.target("http://localhost:{port}/api/meta/projection/{name}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("name", "NonExistent")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.header("Authorization", "Impersonation test-user")
			.delete();

		assertThat(response.getStatus()).isEqualTo(Status.GONE.getStatusCode());
	}

	@Test
	void deleteAssociationByNameNotFound() {
		Client client = this.getClient("deleteAssociationByNameNotFound");

		Response response = client
			.target("http://localhost:{port}/api/meta/association/{name}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("name", "NonExistent")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.header("Authorization", "Impersonation test-user")
			.delete();

		assertThat(response.getStatus()).isEqualTo(Status.GONE.getStatusCode());
	}

	@Test
	void deleteClassByNameNotFound() {
		Client client = this.getClient("deleteClassByNameNotFound");

		Response response = client
			.target("http://localhost:{port}/api/meta/class/{name}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("name", "NonExistent")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.header("Authorization", "Impersonation test-user")
			.delete();

		assertThat(response.getStatus()).isEqualTo(Status.GONE.getStatusCode());
	}

	@Test
	void deleteMetaModelInOrder() {
		Client serviceGroupClient = this.getClient("deleteMetaModelInOrder_serviceGroups");
		Response serviceGroupResponse = serviceGroupClient
			.target("http://localhost:{port}/api/meta/serviceGroup")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request(MediaType.APPLICATION_JSON_TYPE)
			.header("Authorization", "Impersonation test-user")
			.delete();
		assertThat(serviceGroupResponse.getStatus()).isEqualTo(Status.NO_CONTENT.getStatusCode());

		Client projectionClient = this.getClient("deleteMetaModelInOrder_projections");
		Response projectionResponse = projectionClient
			.target("http://localhost:{port}/api/meta/projection")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request(MediaType.APPLICATION_JSON_TYPE)
			.header("Authorization", "Impersonation test-user")
			.delete();
		assertThat(projectionResponse.getStatus()).isEqualTo(Status.NO_CONTENT.getStatusCode());

		Client associationClient = this.getClient("deleteMetaModelInOrder_associations");
		Response associationResponse = associationClient
			.target("http://localhost:{port}/api/meta/association")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request(MediaType.APPLICATION_JSON_TYPE)
			.header("Authorization", "Impersonation test-user")
			.delete();
		assertThat(associationResponse.getStatus()).isEqualTo(Status.NO_CONTENT.getStatusCode());

		Client deleteClassClient = this.getClient("deleteMetaModelInOrder_deleteClass");
		Response deleteClassResponse = deleteClassClient
			.target("http://localhost:{port}/api/meta/class/{name}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("name", "User")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.header("Authorization", "Impersonation test-user")
			.delete();
		assertThat(deleteClassResponse.getStatus()).isEqualTo(Status.NO_CONTENT.getStatusCode());

		Client getClassClient = this.getClient("deleteMetaModelInOrder_getClass");
		Response getClassResponse = getClassClient
			.target("http://localhost:{port}/api/meta/class/{name}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("name", "User")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();
		assertThat(getClassResponse.getStatus()).isEqualTo(Status.GONE.getStatusCode());
	}

	@Test
	void putClassByName_validationRejectsRoundTrip() {
		Client getClient = this.getClient("putClassByName_get");

		Response getResponse = getClient
			.target("http://localhost:{port}/api/meta/class/{name}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("name", "User")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponseStatus(getResponse, Status.OK);
		String classJson = getResponse.readEntity(String.class);

		Client putClient = this.getClient("putClassByName_put");

		Response putResponse = putClient
			.target("http://localhost:{port}/api/meta/class/{name}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("name", "User")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.header("Authorization", "Impersonation test-user")
			.put(Entity.json(classJson));

		this.assertResponseStatus(putResponse, Status.BAD_REQUEST);
		String errors = putResponse.readEntity(String.class);
		assertThat(errors).contains("No such property 'DataTypeProperty.__typename'");
	}

	@Test
	void putClassByName_emptyBodyRejected() {
		Client client = this.getClient("putClassByName_emptyBodyRejected");

		Response response = client
			.target("http://localhost:{port}/api/meta/class/{name}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("name", "User")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.header("Authorization", "Impersonation test-user")
			.put(Entity.json("{}"));

		assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
	}
}
