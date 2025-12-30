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

package com.stackoverflow.dropwizard.test;

import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.stackoverflow.dropwizard.application.StackOverflowApplication;
import io.dropwizard.testing.ResourceHelpers;
import io.liftwizard.dropwizard.testing.junit.AbstractDropwizardAppTest;
import io.liftwizard.junit.extension.app.LiftwizardAppExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import io.liftwizard.reladomo.test.extension.ReladomoTestFile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionResourceManualTest extends AbstractDropwizardAppTest {

	@Nonnull
	@Override
	protected LiftwizardAppExtension<?> getDropwizardAppExtension() {
		return new LiftwizardAppExtension<>(
			StackOverflowApplication.class,
			ResourceHelpers.resourceFilePath("config-test.json5")
		);
	}

	@Test
	@ReladomoTestFile("test-data/existing-question.txt")
	void get_smoke_test() {
		Client client = this.getClient("get_smoke_test");

		this.assertQuestion1Unchanged(client, "assertQuestion1Unchanged_get_smoke_test");
	}

	protected void assertQuestion1Unchanged(@Nonnull Client client, String testName) {
		Response response = client
			.target("http://localhost:{port}/api/manual/question/{id}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("id", 1)
			.request()
			.get();

		this.assertResponse(testName, Status.OK, response);
	}

	@Test
	@ReladomoTestFile("test-data/existing-question.txt")
	void post_invalid_data() {
		Client client = this.getClient("post_invalid_data");

		String invalidJson = FileSlurper.slurp(
			this.getClass().getSimpleName() + ".invalid_data.json5",
			this.getClass()
		);

		Response response = client
			.target("http://localhost:{port}/api/manual/question/")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request()
			.header("Authorization", "Impersonation test user 1")
			.post(Entity.json(invalidJson));

		this.assertResponse("post_invalid_data", Status.BAD_REQUEST, response);
	}

	@Test
	@ReladomoTestFile("test-data/existing-question.txt")
	void post_valid_data() {
		Client client = this.getClient("post_valid_data");

		// <editor-fold desc="POST valid json, status: CREATED">
		{
			String validJson = FileSlurper.slurp(
				this.getClass().getSimpleName() + ".create_data.json5",
				this.getClass()
			);

			Response response = client
				.target("http://localhost:{port}/api/manual/question/")
				.resolveTemplate("port", this.appExtension.getLocalPort())
				.request()
				.header("Authorization", "Impersonation test user 1")
				.post(Entity.json(validJson));

			this.assertResponse("post_valid_data", Status.CREATED, response);
			assertThat(response.getLocation().getPath()).isEqualTo("/api/manual/question/2");
		}
		// </editor-fold>

		this.assertQuestion1Unchanged(client, "assertQuestion1Unchanged_post_valid_data");

		// <editor-fold desc="GET id: 2, status: ok">
		{
			Response response = client
				.target("http://localhost:{port}/api/manual/question/{id}")
				.resolveTemplate("port", this.appExtension.getLocalPort())
				.resolveTemplate("id", 2)
				.request()
				.get();

			this.assertResponse("post_valid_data_get", Status.OK, response);
		}
		// </editor-fold>
	}

	@Test
	@ReladomoTestFile("test-data/existing-question.txt")
	void put_invalid_id() {
		Client client = this.getClient("put_invalid_id");

		String json = FileSlurper.slurp(this.getClass().getSimpleName() + ".invalid_id_data.json5", this.getClass());

		Response response = client
			.target("http://localhost:{port}/api/manual/question/{id}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("id", 1)
			.queryParam("version", "2")
			.request()
			.header("Authorization", "Impersonation test user 1")
			.put(Entity.json(json));

		this.assertResponse("put_invalid_id", Status.BAD_REQUEST, response);
		this.assertQuestion1Unchanged(client, "assertQuestion1Unchanged_put_invalid_id");
	}

	@Test
	@ReladomoTestFile("test-data/existing-question.txt")
	void put_conflict() {
		Client client = this.getClient("put_conflict");

		String validJson = FileSlurper.slurp(
			this.getClass().getSimpleName() + ".valid_versioned_put_data.json5",
			this.getClass()
		);

		Response response = client
			.target("http://localhost:{port}/api/manual/question/{id}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("id", 1)
			.queryParam("version", "1")
			.request()
			.header("Authorization", "Impersonation test user 1")
			.put(Entity.json(validJson));

		this.assertResponse("put_conflict", Status.CONFLICT, response);

		this.assertQuestion1Unchanged(client, "assertQuestion1Unchanged_put_conflict");
	}

	@Test
	@ReladomoTestFile("test-data/existing-question.txt")
	void put() {
		Client client = this.getClient("put");

		// <editor-fold desc="PUT id: 1, version: 2, status: NO_CONTENT">
		{
			String validJson = FileSlurper.slurp(
				this.getClass().getSimpleName() + ".valid_versioned_put_data.json5",
				this.getClass()
			);

			Response response = client
				.target("http://localhost:{port}/api/manual/question/{id}")
				.resolveTemplate("port", this.appExtension.getLocalPort())
				.resolveTemplate("id", 1)
				.queryParam("version", "2")
				.request()
				.header("Authorization", "Impersonation test user 1")
				.put(Entity.json(validJson));

			this.assertResponse("put", Status.OK, response);
		}
		// </editor-fold>

		Response response = client
			.target("http://localhost:{port}/api/manual/question/{id}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("id", 1)
			.request()
			.get();

		this.assertResponse("put2", Status.OK, response);
		// TODO: PUT with owned children, with all four cases of unchanged, created, updated, deleted
	}

	@Test
	@ReladomoTestFile("test-data/existing-question.txt")
	void put_unchanged() {
		Client client = this.getClient("put_unchanged");

		String jsonName = this.getClass().getSimpleName() + ".put_unchanged.json5";
		String json = FileSlurper.slurp(jsonName, this.getClass());

		Response response = client
			.target("http://localhost:{port}/api/manual/question/{id}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("id", 1)
			.queryParam("version", "2")
			.request()
			.header("Authorization", "Impersonation test user 1")
			.put(Entity.json(json));

		this.assertResponse("put_unchanged", Status.OK, response);

		this.assertQuestion1Unchanged(client, "assertQuestion1Unchanged_put_unchanged");
	}

	@Test
	void restSet() {
		Client client = this.getClient("restSet");

		{
			Response response = client
				.target("http://localhost:{port}/api/manual/set")
				.resolveTemplate("port", this.appExtension.getLocalPort())
				.request()
				.get();

			this.assertResponse("restSet", Status.OK, response);
		}

		{
			Response response = client
				.target("http://localhost:{port}/api/manual/map")
				.resolveTemplate("port", this.appExtension.getLocalPort())
				.request()
				.get();

			this.assertResponse("restMap", Status.OK, response);
		}
	}
	// TODO: Should PUT return the version number as an indicator that something changed? Or some other HTTP code?
}
