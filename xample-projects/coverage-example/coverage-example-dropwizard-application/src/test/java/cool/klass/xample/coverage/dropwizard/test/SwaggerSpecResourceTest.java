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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SwaggerSpecResourceTest extends AbstractCoverageTest {

    @Test
    void swagger_spec_is_served() throws Exception {
        Client client = this.getClient("swagger_spec_is_served");

        Response response = client
            .target("http://localhost:{port}/api/swagger.json")
            .resolveTemplate("port", this.appExtension.getLocalPort())
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header("Authorization", "Impersonation User ID")
            .get();

        this.assertResponseStatus(response, Status.OK);

        String responseJson = response.readEntity(String.class);
        assertNotNull(responseJson, "Swagger spec should not be null");
        assertTrue(responseJson.length() > 100, "Swagger spec should have meaningful content");

        // Parse and validate it's valid JSON with expected Swagger structure
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode swaggerSpec = objectMapper.readTree(responseJson);

        assertNotNull(swaggerSpec.get("swagger"), "Should have swagger version");
        assertNotNull(swaggerSpec.get("info"), "Should have info section");
        assertNotNull(swaggerSpec.get("paths"), "Should have paths section");

        // Verify it's Swagger 2.0 format
        assertEquals("2.0", swaggerSpec.get("swagger").asText(), "Should be Swagger 2.0");
    }

    @Test
    void swagger_spec_by_package() throws Exception {
        Client client = this.getClient("swagger_spec_by_package");

        Response response = client
            .target("http://localhost:{port}/api/swagger/{packageName}.json")
            .resolveTemplate("port", this.appExtension.getLocalPort())
            .resolveTemplate("packageName", "cool.klass.xample.coverage")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header("Authorization", "Impersonation User ID")
            .get();

        this.assertResponseStatus(response, Status.OK);

        String responseJson = response.readEntity(String.class);
        assertNotNull(responseJson, "Swagger spec should not be null");

        // Parse and validate
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode swaggerSpec = objectMapper.readTree(responseJson);

        assertNotNull(swaggerSpec.get("info"), "Should have info section");
        assertTrue(swaggerSpec.get("info").get("title").asText().contains("Coverage"),
            "Title should mention Coverage");
    }
}
