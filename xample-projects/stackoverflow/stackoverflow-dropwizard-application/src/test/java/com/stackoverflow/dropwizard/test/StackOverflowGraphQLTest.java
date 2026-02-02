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

package com.stackoverflow.dropwizard.test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.liftwizard.junit.extension.match.FileSlurper;
import io.liftwizard.reladomo.test.extension.ReladomoTestFile;
import org.eclipse.collections.api.factory.Maps;
import org.junit.jupiter.api.Test;

class StackOverflowGraphQLTest extends AbstractStackOverflowApplicationTest {

	@Test
	@ReladomoTestFile("test-data/existing-question.txt")
	void smokeTest() {
		Client client = this.getClient("graphqlSmokeTest");

		String queryName = this.getClass().getSimpleName() + ".smokeTest.graphql";
		String query = FileSlurper.slurp(queryName, this.getClass());

		Response response = client
			.target("http://localhost:{port}/graphql")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request()
			.post(Entity.json(Maps.mutable.with("query", query)));

		this.assertResponse("graphqlSmokeTest", Status.OK, response);
	}
}
