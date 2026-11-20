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

import java.sql.Timestamp;
import java.time.Instant;

import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.gs.fw.common.mithra.MithraManagerProvider;
import com.stackoverflow.Question;
import com.stackoverflow.QuestionFinder;
import com.stackoverflow.QuestionList;
import com.stackoverflow.QuestionVote;
import io.liftwizard.junit.extension.match.FileSlurper;
import io.liftwizard.reladomo.test.extension.ReladomoTestFile;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StackOverflowApplicationTest
	extends AbstractStackOverflowApplicationTest
{
	@Test
	@ReladomoTestFile("test-data/existing-question.txt")
	void readsThenWrites()
	{
		this.smokeTest();
		this.get_smoke_test();
		this.restSet();
		this.post_invalid_data();
		this.put_invalid_id();
		this.put_conflict();
		this.put_unchanged();
		this.post_valid_data();
		this.put();
		this.post_vote();
		this.post_answer();
	}

	/**
	 * Question.numberOfVotes is declared as {@code Long derived = count(this.votes)}.
	 * It is not stored. The database computes it in a view, so the database can sort and filter by it.
	 * Question 1 has one current vote and three terminated votes, question 2 has three current votes, question 3 has none.
	 */
	@Test
	@ReladomoTestFile("test-data/questions-with-votes.txt")
	void derivedCountIsSortedAndFilteredInTheDatabase()
	{
		// With a row limit, Reladomo cannot sort in memory, so the right answer proves the database sorted by the count
		assertThat(this.findQuestionIdsByVotes(1)).containsExactly(2L);
		assertThat(this.findQuestionIdsByVotes(3)).containsExactly(2L, 1L, 3L);

		assertThat(QuestionFinder.findOne(QuestionFinder.id().eq(1L)).getNumberOfVotes()).isEqualTo(1L);
		assertThat(
			QuestionFinder.findMany(QuestionFinder.derived().numberOfVotes().greaterThanEquals(1L))
				.asEcList()
				.collect(Question::getId)
		).containsExactlyInAnyOrder(1L, 2L);

		Client client = this.getClient("derived_count");
		this.assertQuestionsByVotes(client, 0, "derived_count_all");
		this.assertQuestionsByVotes(client, 2, "derived_count_minimum_2");

		// No code maintains a counter. New votes show up in the count, the sort order and the filter.
		MithraManagerProvider.getMithraManager().executeTransactionalCommand((tx) ->
		{
			for (String userId : Lists.immutable.with("test user 1", "test user 2", "test user 3", "test user 4"))
			{
				var vote = new QuestionVote();
				vote.setQuestionId(3L);
				vote.setCreatedById(userId);
				vote.setCreatedOn(Timestamp.from(Instant.parse("2001-01-01T00:00:00Z")));
				vote.setLastUpdatedById(userId);
				vote.setDirection("up");
				vote.insert();
			}
			return null;
		});

		assertThat(this.findQuestionIdsByVotes(1)).containsExactly(3L);
		assertThat(this.findQuestionIdsByVotes(3)).containsExactly(3L, 2L, 1L);
		assertThat(QuestionFinder.findOne(QuestionFinder.id().eq(3L)).getNumberOfVotes()).isEqualTo(4L);
		this.assertQuestionsByVotes(client, 2, "derived_count_after_votes");
	}

	private ImmutableList<Long> findQuestionIdsByVotes(int maxObjectsToRetrieve)
	{
		QuestionList questions = QuestionFinder.findMany(QuestionFinder.all());
		questions.setOrderBy(QuestionFinder.derived().numberOfVotes().descendingOrderBy());
		// Reladomo's query cache does not know when votes change, so queries on derived properties bypass it
		questions.setBypassCache(true);
		questions.setMaxObjectsToRetrieve(maxObjectsToRetrieve);
		return questions.asEcList().collect(Question::getId).toImmutable();
	}

	private void assertQuestionsByVotes(Client client, long minimumVotes, String testName)
	{
		Response response = client
			.target("http://localhost:{port}/api/questionsByVotes")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.queryParam("minimumVotes", minimumVotes)
			.request()
			.get();

		this.assertResponse(testName, Status.OK, response);
	}

	private void smokeTest()
	{
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

	private void post_vote()
	{
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

	private void post_answer()
	{
		Client client = this.getClient("post_answer");

		String json = FileSlurper.slurp(this.getClass().getSimpleName() + ".post_answer.json5", this.getClass());

		Response postResponse = client
			.target("http://localhost:{port}/api/answer")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request()
			.header("Authorization", "Impersonation test user 1")
			.post(Entity.json(json));

		this.assertEmptyResponse(Status.NO_CONTENT, postResponse);

		// Verify the answer was created by fetching answers for question 1
		Response getResponse = client
			.target("http://localhost:{port}/api/question/{questionId}/answers")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("questionId", 1)
			.request()
			.get();

		this.assertResponse("post_answer_get", Status.OK, getResponse);
	}

	private void get_smoke_test()
	{
		Client client = this.getClient("get_smoke_test");

		this.assertQuestion1Unchanged(client, "assertQuestion1Unchanged_get_smoke_test");
	}

	protected void assertQuestion1Unchanged(@Nonnull Client client, String testName)
	{
		Response response = client
			.target("http://localhost:{port}/api/manual/question/{id}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("id", 1)
			.request()
			.get();

		this.assertResponse(testName, Status.OK, response);
	}

	private void post_invalid_data()
	{
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

	private void post_valid_data()
	{
		Client client = this.getClient("post_valid_data");

		// <editor-fold desc="POST valid json, status: CREATED">
		{
			String validJson = FileSlurper.slurp(
				this.getClass().getSimpleName() + ".create_data.json5",
				this.getClass()
			);

			Response postResponse = client
				.target("http://localhost:{port}/api/manual/question/")
				.resolveTemplate("port", this.appExtension.getLocalPort())
				.request()
				.header("Authorization", "Impersonation test user 1")
				.post(Entity.json(validJson));

			this.assertResponse("post_valid_data", Status.CREATED, postResponse);
			assertThat(postResponse.getLocation().getPath()).isEqualTo("/api/manual/question/2");
		}
		// </editor-fold>

		this.assertQuestion1Unchanged(client, "assertQuestion1Unchanged_post_valid_data");

		// <editor-fold desc="GET id: 2, status: ok">
		{
			Response getResponse = client
				.target("http://localhost:{port}/api/manual/question/{id}")
				.resolveTemplate("port", this.appExtension.getLocalPort())
				.resolveTemplate("id", 2)
				.request()
				.get();

			this.assertResponse("post_valid_data_get", Status.OK, getResponse);
		}
		// </editor-fold>
	}

	private void put_invalid_id()
	{
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

	private void put_conflict()
	{
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

	private void put()
	{
		Client client = this.getClient("put");

		// <editor-fold desc="PUT id: 1, version: 2, status: OK">
		{
			String validJson = FileSlurper.slurp(
				this.getClass().getSimpleName() + ".valid_versioned_put_data.json5",
				this.getClass()
			);

			Response putResponse = client
				.target("http://localhost:{port}/api/manual/question/{id}")
				.resolveTemplate("port", this.appExtension.getLocalPort())
				.resolveTemplate("id", 1)
				.queryParam("version", "2")
				.request()
				.header("Authorization", "Impersonation test user 1")
				.put(Entity.json(validJson));

			this.assertResponse("put", Status.OK, putResponse);
		}
		// </editor-fold>

		Response getResponse = client
			.target("http://localhost:{port}/api/manual/question/{id}")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.resolveTemplate("id", 1)
			.request()
			.get();

		this.assertResponse("put2", Status.OK, getResponse);
		// TODO: PUT with owned children, with all four cases of unchanged, created, updated, deleted
	}

	private void put_unchanged()
	{
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

	private void restSet()
	{
		Client client = this.getClient("restSet");

		{
			Response setResponse = client
				.target("http://localhost:{port}/api/manual/set")
				.resolveTemplate("port", this.appExtension.getLocalPort())
				.request()
				.get();

			this.assertResponse("restSet", Status.OK, setResponse);
		}

		{
			Response mapResponse = client
				.target("http://localhost:{port}/api/manual/map")
				.resolveTemplate("port", this.appExtension.getLocalPort())
				.request()
				.get();

			this.assertResponse("restMap", Status.OK, mapResponse);
		}
	}

	// TODO: Should PUT return the version number as an indicator that something changed? Or some other HTTP code?
}
