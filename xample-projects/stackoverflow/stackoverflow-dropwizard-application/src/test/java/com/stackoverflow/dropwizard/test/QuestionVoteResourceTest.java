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
import org.junit.jupiter.api.Test;

class QuestionVoteResourceTest extends AbstractStackOverflowApplicationTest {

	@Test
	@ReladomoTestFile("test-data/existing-question.txt")
	void post_vote() {
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
}
