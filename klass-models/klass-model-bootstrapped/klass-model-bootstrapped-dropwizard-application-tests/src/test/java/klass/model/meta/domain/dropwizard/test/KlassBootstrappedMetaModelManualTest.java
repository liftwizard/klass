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

package klass.model.meta.domain.dropwizard.test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.liftwizard.junit.extension.match.FileSlurper;
import org.eclipse.collections.api.factory.Maps;
import org.junit.jupiter.api.Test;

class KlassBootstrappedMetaModelManualTest extends AbstractResourceTestCase {

	@Test
	void readsThenDelete() {
		this.assertGraphQL("graphql.packageableElements");
		this.assertGraphQL("graphql.enumerations");
		this.assertGraphQL("graphql.interfaces");
		this.assertGraphQL("graphql.classes");
		this.assertGraphQL("graphql.associations");
		this.assertGraphQL("graphql.projectionElements");
		this.assertGraphQL("graphql.namedProjections");
		this.assertGraphQL("graphql.expressionValue");
		this.assertGraphQL("graphql.criteria");
		this.assertGraphQL("graphql.serviceGroups");

		this.assertUrlReturns("packageableElement.getAllMeta", "/meta/packageableElement");
		this.assertUrlReturns("packageableElement.getByName", "/meta/packageableElement/Association");
		this.assertUrlReturns("packageableElement.getByNameEnumeration", "/meta/packageableElement/Enumeration");

		this.assertUrlReturns("enumeration.getAllMeta", "/meta/enumeration");
		this.assertUrlReturns("enumeration.getByName", "/meta/enumeration/PrimitiveType");
		this.assertUrlReturns("enumeration.getByNameVerb", "/meta/enumeration/Verb");
		this.assertUrlReturns("enumeration.getByNameMultiplicity", "/meta/enumeration/Multiplicity");

		this.assertUrlReturns("classifier.getAllMeta", "/meta/classifier");
		this.assertUrlReturns("classifier.getByName", "/meta/classifier/Klass");
		this.assertUrlReturns("classifier.getByNameInterface", "/meta/classifier/NamedElement");

		this.assertUrlReturns("interface.getAllMeta", "/meta/interface");
		this.assertUrlReturns("interface.getByName", "/meta/interface/NamedElement");
		this.assertUrlReturns("interface.getByNameElement", "/meta/interface/Element");

		this.assertUrlReturns("class.getAllMeta", "/meta/class");
		this.assertUrlReturns("class.getByName", "/meta/class/Klass");
		this.assertUrlReturns("class.metaClass", "/meta/class/Classifier");

		this.assertUrlReturns("association.getAllMeta", "/meta/association");
		this.assertUrlReturns("association.getByName", "/meta/association/ClassHasSuperClass");
		this.assertUrlReturns("association.getByNameEnumerationHasLiterals", "/meta/association/EnumerationHasLiterals");
		this.assertUrlReturns(
			"association.getByNameDataTypePropertyHasModifiers",
			"/meta/association/DataTypePropertyHasModifiers"
		);

		this.assertUrlReturns("projection.getAllMeta", "/meta/projection");
		this.assertUrlReturns("projection.getByName", "/meta/projection/KlassProjection");
		this.assertUrlReturns("projection.getByNameAssociationProjection", "/meta/projection/AssociationProjection");
		this.assertUrlReturns("projection.metaProjection", "/meta/projection/ProjectionElementProjection");

		this.assertUrlReturns("serviceGroup.getAllMeta", "/meta/serviceGroup");
		this.assertUrlReturns("serviceGroup.getByName", "/meta/serviceGroup/ServiceGroupResource");
		this.assertUrlReturns("serviceGroup.getByNameKlassResource", "/meta/serviceGroup/KlassResource");
		this.assertUrlDeletes("serviceGroup.deleteByName", "/meta/serviceGroup/KlassResource");
		this.assertUrlReturnsGone("serviceGroup.getByNameAfterDelete", "/meta/serviceGroup/KlassResource");
	}

	private void assertGraphQL(String testName) {
		Client client = this.getClient(testName);

		Class<?> callingClass = this.getClass();
		String sourceName = callingClass.getSimpleName() + "." + testName + ".graphql";
		String query = FileSlurper.slurp(sourceName, callingClass);

		Response response = client
			.target("http://localhost:{port}/graphql")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request()
			.post(Entity.json(Maps.mutable.with("query", query)));

		this.assertResponse(testName, Status.OK, response);
	}
}
