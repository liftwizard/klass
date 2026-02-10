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

package cool.klass.generator.swagger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cool.klass.model.meta.domain.api.DomainModel;
import io.swagger.models.Info;
import io.swagger.models.Swagger;

/**
 * Generates a single swagger.json file containing the OpenAPI specification
 * for all service groups in the domain model.
 */
public class SwaggerSpecGenerator {

	@Nonnull
	private final ObjectMapper objectMapper;

	@Nonnull
	private final DomainModel domainModel;

	@Nonnull
	private final String applicationName;

	public SwaggerSpecGenerator(
		@Nonnull ObjectMapper objectMapper,
		@Nonnull DomainModel domainModel,
		@Nonnull String applicationName
	) {
		this.objectMapper = Objects.requireNonNull(objectMapper);
		this.domainModel = Objects.requireNonNull(domainModel);
		this.applicationName = Objects.requireNonNull(applicationName);
	}

	public void writeFiles(@Nonnull Path outputPath) {
		Path swaggerPath = outputPath.resolve("swagger");
		swaggerPath.toFile().mkdirs();

		Path filePath = swaggerPath.resolve("swagger.json");
		String content = this.generateSwaggerSpec();
		this.printStringToFile(filePath, content);
	}

	@Nonnull
	private String generateSwaggerSpec() {
		Swagger swagger = new Swagger();
		swagger.setSwagger("2.0");

		Info info = new Info();
		info.setTitle(this.applicationName + " API");
		info.setVersion("1.0.0");
		info.setDescription("Generated from Klass model");
		swagger.setInfo(info);

		swagger.setBasePath("/api");
		swagger.addConsumes("application/json");
		swagger.addProduces("application/json");

		// Visit all top-level elements and populate the Swagger model
		var visitor = new ServiceGroupToSwaggerSpecVisitor(swagger);
		this.domainModel.getTopLevelElements().forEach((element) -> element.visit(visitor));

		try {
			return this.objectMapper.writeValueAsString(swagger);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private void printStringToFile(@Nonnull Path path, String contents) {
		try (
			PrintStream printStream = new PrintStream(new FileOutputStream(path.toFile()), true, StandardCharsets.UTF_8)
		) {
			printStream.print(contents);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
