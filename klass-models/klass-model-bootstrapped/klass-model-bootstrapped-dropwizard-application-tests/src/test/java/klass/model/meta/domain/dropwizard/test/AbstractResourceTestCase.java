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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.util.Duration;
import io.liftwizard.junit.extension.app.LiftwizardAppExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import io.liftwizard.junit.extension.match.json.JsonMatchExtension;
import klass.model.meta.domain.dropwizard.application.KlassBootstrappedMetaModelApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractResourceTestCase {

	@RegisterExtension
	protected final JsonMatchExtension jsonMatchExtension = new JsonMatchExtension(this.getClass());

	protected final LiftwizardAppExtension<?> appExtension = new LiftwizardAppExtension<>(
		KlassBootstrappedMetaModelApplication.class,
		ResourceHelpers.resourceFilePath("config-test.json5")
	);

	@BeforeAll
	void startApp() throws Exception {
		this.appExtension.before();
	}

	@AfterAll
	void stopApp() {
		this.appExtension.after();
	}

	protected Client getClient(@Nonnull String testName) {
		var jerseyClientConfiguration = new JerseyClientConfiguration();
		jerseyClientConfiguration.setTimeout(Duration.minutes(5));

		String className = this.getClass().getCanonicalName();
		String clientName = className + "." + testName;

		return new JerseyClientBuilder(this.appExtension.getEnvironment())
			.using(jerseyClientConfiguration)
			.build(clientName);
	}

	protected void assertResponseStatus(@Nonnull Response response, Status status) {
		assertThat(response.hasEntity()).isTrue();
		response.bufferEntity();
		String entityAsString = response.readEntity(String.class);
		assertThat(response.getStatusInfo()).as(entityAsString).isEqualTo(status);
	}

	protected void assertUrlReturns(@Nonnull String testName, @Nonnull String url) {
		Class<?> klass = this.getClass();
		String clientName = klass.getPackage().getName() + '.' + klass.getSimpleName() + '.' + testName;
		Client client = this.getClient(clientName);
		String resourceClassPathLocation = klass.getSimpleName() + '.' + testName + ".json";

		this.assertUrlReturns(client, url, resourceClassPathLocation);
	}

	protected void assertUrlReturns(Client client, @Nonnull String url, String resourceClassPathLocation) {
		Response response = client
			.target("http://localhost:{port}/api/" + url)
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request()
			.get();

		this.assertResponseStatus(response, Status.OK);
		String jsonResponse = response.readEntity(String.class);

		this.jsonMatchExtension.assertFileContents(resourceClassPathLocation, jsonResponse);
	}

	protected void assertUrlWrites(@Nonnull String testName, @Nonnull String url) {
		Class<?> klass = this.getClass();
		String clientName = klass.getPackage().getName() + '.' + klass.getSimpleName() + '.' + testName;
		Client client = this.getClient(clientName);
		String resourceClassPathLocation = klass.getSimpleName() + '.' + testName + ".json";

		this.assertUrlWrites(client, url, resourceClassPathLocation);
	}

	protected void assertUrlWrites(Client client, @Nonnull String url, String resourceClassPathLocation) {
		InputStream inputStream = this.getClass().getResourceAsStream(resourceClassPathLocation);
		Objects.requireNonNull(inputStream, () -> resourceClassPathLocation + " not found.");
		String expectedStringFromFile = FileSlurper.slurp(inputStream, StandardCharsets.UTF_8);

		Response response = client
			.target("http://localhost:{port}/api/" + url)
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request()
			.header("Authorization", "Impersonation test user 1")
			.put(Entity.json(expectedStringFromFile));

		if (response.hasEntity()) {
			response.bufferEntity();
			String entityAsString = response.readEntity(String.class);
			assertThat(response.getStatusInfo()).as(entityAsString).isEqualTo(Status.NO_CONTENT);
		} else {
			assertThat(response.getStatusInfo()).isEqualTo(Status.NO_CONTENT);
		}
	}

	protected void assertUrlDeletes(@Nonnull String testName, @Nonnull String url) {
		Class<?> klass = this.getClass();
		String clientName = klass.getPackage().getName() + '.' + klass.getSimpleName() + '.' + testName;
		Client client = this.getClient(clientName);

		Response response = client
			.target("http://localhost:{port}/api/" + url)
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request()
			.header("Authorization", "Impersonation test user 1")
			.delete();

		if (response.hasEntity()) {
			response.bufferEntity();
			String entityAsString = response.readEntity(String.class);
			assertThat(response.getStatusInfo()).as(entityAsString).isEqualTo(Status.NO_CONTENT);
		} else {
			assertThat(response.getStatusInfo()).isEqualTo(Status.NO_CONTENT);
		}
	}

	protected void assertUrlReturnsGone(@Nonnull String testName, @Nonnull String url) {
		Class<?> klass = this.getClass();
		String clientName = klass.getPackage().getName() + '.' + klass.getSimpleName() + '.' + testName;
		Client client = this.getClient(clientName);

		Response response = client
			.target("http://localhost:{port}/api/" + url)
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request()
			.get();

		assertThat(response.getStatusInfo()).isEqualTo(Status.GONE);
	}
}
