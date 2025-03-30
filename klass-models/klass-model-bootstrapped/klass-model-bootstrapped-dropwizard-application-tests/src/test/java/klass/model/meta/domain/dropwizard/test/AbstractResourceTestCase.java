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

package klass.model.meta.domain.dropwizard.test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.dropwizard.testing.ResourceHelpers;
import io.liftwizard.dropwizard.testing.junit.AbstractDropwizardAppTest;
import io.liftwizard.junit.extension.app.LiftwizardAppExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import klass.model.meta.domain.dropwizard.application.KlassBootstrappedMetaModelApplication;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractResourceTestCase extends AbstractDropwizardAppTest {

    @Nonnull
    @Override
    protected LiftwizardAppExtension<?> getDropwizardAppExtension() {
        return new LiftwizardAppExtension<>(
            KlassBootstrappedMetaModelApplication.class,
            ResourceHelpers.resourceFilePath("config-test.json5")
        );
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
            .put(Entity.json(expectedStringFromFile));

        this.assertResponseStatus(response, Status.OK);
        String jsonResponse = response.readEntity(String.class);
        assertEquals("", jsonResponse);
    }
}
