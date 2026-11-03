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

class StackOverflowApplicationTest extends AbstractStackOverflowApplicationTest {

	@Test
	@ReladomoTestFile("test-data/existing-question.txt")
	void graphQLThenVoteThenAnswer() {
		this.smokeTest();
		this.post_vote();
		this.post_answer();
	}

	private void smokeTest() {
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

	private void post_vote() {
		Client client = this.getClient("post_vote");

		String json = FileSlurper.slurp(this.getClass().getSimpleName() + ".post_vote.json5", this.getClass());

		Response response = client
			.target("http://localhost:{port}/api/vote")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request()
			.header("Authorization", "Impersonation test user 1")
			.post(Entity.json(json));

		this.assertResponse("post_vote", Status.CREATED, response);
	}

	private void post_answer() {
		Client client = this.getClient("post_answer");

		String json = FileSlurper.slurp(this.getClass().getSimpleName() + ".post_answer.json5", this.getClass());

		Response response = client
			.target("http://localhost:{port}/api/answer")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request()
			.header("Authorization", "Impersonation test user 1")
			.post(Entity.json(json));

		this.assertEmptyResponse(Status.NO_CONTENT, response);

		// Verify the answer was created by fetching answers for question 1
		Response getResponse = client
			.target("http://localhost:{port}/api/question/{questionId}/answers")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("questionId", 1)
			.request()
			.get();

		this.assertResponse("post_answer_get", Status.OK, getResponse);
	}
}
