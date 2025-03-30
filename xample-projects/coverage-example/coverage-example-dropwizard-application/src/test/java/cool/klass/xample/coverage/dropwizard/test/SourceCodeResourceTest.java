/*
 * Copyright 2024 Craig Motlin
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

import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SourceCodeResourceTest extends AbstractCoverageTest {

    @RegisterExtension
    final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

    @Test
    void smoke_test() {
        Client client = this.getClient("smoke_test");

        Response response = client
            .target("http://localhost:{port}/api/meta/code/element/{topLevelElementName}")
            .resolveTemplate("port", this.appExtension.getLocalPort())
            .resolveTemplate("topLevelElementName", "User")
            .request(MediaType.TEXT_HTML_TYPE)
            .header("Authorization", "Impersonation User ID")
            .get();

        this.assertResponseStatus(response, Status.OK);
        String responseHtml = response.readEntity(String.class);
        // assertEquals(responseHtml, expected, responseHtml);

        String expectedStringClasspathLocation = this.getClass().getCanonicalName() + "#smoke_test.html";
        this.fileMatchExtension.assertFileContents(expectedStringClasspathLocation, responseHtml);
    }
}
