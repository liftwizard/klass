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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;

class MetaModelResourceTest extends AbstractCoverageTest {

	// PackageableElement tests
	@Test
	void getPackageableElements() {
		Client client = this.getClient("getPackageableElements");

		Response response = client
			.target("http://localhost:{port}/api/meta/packageableElement")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponse("getPackageableElements", Status.OK, response);
	}

	@Test
	void getPackageableElementByName() {
		Client client = this.getClient("getPackageableElementByName");

		Response response = client
			.target("http://localhost:{port}/api/meta/packageableElement/{name}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("name", "User")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponse("getPackageableElementByName", Status.OK, response);
	}

	// Class tests
	@Test
	void getClasses() {
		Client client = this.getClient("getClasses");

		Response response = client
			.target("http://localhost:{port}/api/meta/class")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponse("getClasses", Status.OK, response);
	}

	@Test
	void getClassByName() {
		Client client = this.getClient("getClassByName");

		Response response = client
			.target("http://localhost:{port}/api/meta/class/{name}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("name", "User")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponse("getClassByName", Status.OK, response);
	}

	// Enumeration tests
	@Test
	void getEnumerations() {
		Client client = this.getClient("getEnumerations");

		Response response = client
			.target("http://localhost:{port}/api/meta/enumeration")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponse("getEnumerations", Status.OK, response);
	}

	@Test
	void getEnumerationByName() {
		Client client = this.getClient("getEnumerationByName");

		Response response = client
			.target("http://localhost:{port}/api/meta/enumeration/{name}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("name", "Priority")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponse("getEnumerationByName", Status.OK, response);
	}

	// Association tests
	@Test
	void getAssociations() {
		Client client = this.getClient("getAssociations");

		Response response = client
			.target("http://localhost:{port}/api/meta/association")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponse("getAssociations", Status.OK, response);
	}

	@Test
	void getAssociationByName() {
		Client client = this.getClient("getAssociationByName");

		Response response = client
			.target("http://localhost:{port}/api/meta/association/{name}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("name", "User.tags")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponse("getAssociationByName", Status.OK, response);
	}

	// Classifier tests
	@Test
	void getClassifiers() {
		Client client = this.getClient("getClassifiers");

		Response response = client
			.target("http://localhost:{port}/api/meta/classifier")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponse("getClassifiers", Status.OK, response);
	}

	@Test
	void getClassifierByName() {
		Client client = this.getClient("getClassifierByName");

		Response response = client
			.target("http://localhost:{port}/api/meta/classifier/{name}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("name", "User")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponse("getClassifierByName", Status.OK, response);
	}

	// Interface tests
	@Test
	void getInterfaces() {
		Client client = this.getClient("getInterfaces");

		Response response = client
			.target("http://localhost:{port}/api/meta/interface")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponse("getInterfaces", Status.OK, response);
	}

	@Test
	void getInterfaceByName() {
		Client client = this.getClient("getInterfaceByName");

		Response response = client
			.target("http://localhost:{port}/api/meta/interface/{name}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("name", "Timestamped")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponse("getInterfaceByName", Status.OK, response);
	}

	// Projection tests
	@Test
	void getProjections() {
		Client client = this.getClient("getProjections");

		Response response = client
			.target("http://localhost:{port}/api/meta/projection")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponse("getProjections", Status.OK, response);
	}

	@Test
	void getProjectionByName() {
		Client client = this.getClient("getProjectionByName");

		Response response = client
			.target("http://localhost:{port}/api/meta/projection/{name}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("name", "UserProjection")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponse("getProjectionByName", Status.OK, response);
	}

	// ServiceGroup tests
	@Test
	void getServiceGroups() {
		Client client = this.getClient("getServiceGroups");

		Response response = client
			.target("http://localhost:{port}/api/meta/serviceGroup")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponse("getServiceGroups", Status.OK, response);
	}

	@Test
	void getServiceGroupByName() {
		Client client = this.getClient("getServiceGroupByName");

		Response response = client
			.target("http://localhost:{port}/api/meta/serviceGroup/{name}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("name", "UserService")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponse("getServiceGroupByName", Status.OK, response);
	}
}
