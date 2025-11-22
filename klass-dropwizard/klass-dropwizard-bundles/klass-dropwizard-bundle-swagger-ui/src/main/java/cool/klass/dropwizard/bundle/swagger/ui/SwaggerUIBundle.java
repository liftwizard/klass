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

package cool.klass.dropwizard.bundle.swagger.ui;

import javax.annotation.Nonnull;

import com.google.auto.service.AutoService;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.liftwizard.dropwizard.bundle.prioritized.PrioritizedBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

/**
 * Dropwizard bundle that serves Swagger UI for interactive API documentation.
 * <p>
 * This bundle serves Swagger UI from WebJars at /swagger-ui, which provides an
 * interactive interface for exploring and testing the API documented in swagger.json.
 * </p>
 * <p>
 * The Swagger UI is configured to load the OpenAPI specification from /swagger.json
 * endpoint, which is served by the SwaggerSpecResource.
 * </p>
 *
 * @see cool.klass.service.swagger.SwaggerSpecResource
 */
@AutoService(PrioritizedBundle.class)
public class SwaggerUIBundle implements PrioritizedBundle {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerUIBundle.class);

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void initializeWithMdc(@Nonnull Bootstrap<?> bootstrap) {
        LOGGER.info("Initializing {}.", this.getClass().getSimpleName());

        // Serve Swagger UI from webjar
        bootstrap.addBundle(new AssetsBundle(
            "/META-INF/resources/webjars/swagger-ui/5.17.14",
            "/swagger-ui",
            "index.html",
            "swagger-ui"
        ));

        // Serve our custom initialization page
        bootstrap.addBundle(new AssetsBundle(
            "/swagger-ui-custom",
            "/swagger",
            "index.html",
            "swagger"
        ));
    }

    @Override
    public void runWithMdc(@Nonnull Object configuration, @Nonnull Environment environment) {
        LOGGER.info("Running {}.", this.getClass().getSimpleName());
    }
}
