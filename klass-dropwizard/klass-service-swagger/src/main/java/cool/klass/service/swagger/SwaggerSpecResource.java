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

package cool.klass.service.swagger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS resource that serves the Swagger/OpenAPI specification file.
 */
@Path("/")
public class SwaggerSpecResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerSpecResource.class);
	private static final String SWAGGER_RESOURCE_PATH = "/swagger/swagger.json";

	@Timed
	@ExceptionMetered
	@GET
	@Path("/swagger.json")
	@Produces(MediaType.APPLICATION_JSON)
	public String getSwaggerSpec() {
		LOGGER.debug("Loading Swagger spec from: {}", SWAGGER_RESOURCE_PATH);

		try (InputStream inputStream = this.getClass().getResourceAsStream(SWAGGER_RESOURCE_PATH)) {
			if (inputStream == null) {
				LOGGER.warn("Swagger spec not found at: {}", SWAGGER_RESOURCE_PATH);
				throw new NotFoundException("Swagger specification not found");
			}

			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.error("Error reading Swagger spec from: {}", SWAGGER_RESOURCE_PATH, e);
			throw new RuntimeException("Error reading Swagger specification", e);
		}
	}
}
