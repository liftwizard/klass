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

package cool.klass.generator.swagger;

import java.nio.file.Path;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import cool.klass.generator.perpackage.AbstractPerPackageGenerator;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.TopLevelElement;
import io.swagger.models.Info;
import io.swagger.models.Swagger;

public class SwaggerSpecGenerator extends AbstractPerPackageGenerator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    public SwaggerSpecGenerator(@Nonnull DomainModel domainModel) {
        super(domainModel);
    }

    @Nonnull
    @Override
    protected Path getPluginRelativePath(Path path) {
        return path.resolve("swagger");
    }

    @Nonnull
    @Override
    protected String getFileName() {
        return "swagger.json";
    }

    @Nonnull
    @Override
    protected String getPackageSourceCode(@Nonnull String fullyQualifiedPackage) {
        Swagger swagger = new Swagger();
        swagger.swagger("2.0");

        Info info = new Info();
        info.setTitle(this.getPackageTitle(fullyQualifiedPackage));
        info.setVersion("1.0.0");
        info.setDescription("Generated from Klass model");
        swagger.setInfo(info);

        swagger.setBasePath("/api");
        swagger.addConsumes("application/json");
        swagger.addProduces("application/json");

        // Visit all top-level elements and populate the Swagger model
        var visitor = new ServiceGroupToSwaggerSpecVisitor(swagger, this.domainModel);
        this.domainModel.getTopLevelElements()
            .forEach(element -> element.visit(visitor));

        try {
            return OBJECT_MAPPER.writeValueAsString(swagger);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize Swagger specification", e);
        }
    }

    private String getPackageTitle(@Nonnull String fullyQualifiedPackage) {
        String[] parts = fullyQualifiedPackage.split("\\.");
        String lastPart = parts[parts.length - 1];
        return this.capitalize(lastPart) + " API";
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
