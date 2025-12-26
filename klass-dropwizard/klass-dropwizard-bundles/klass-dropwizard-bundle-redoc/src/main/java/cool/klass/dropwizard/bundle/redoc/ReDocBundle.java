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

package cool.klass.dropwizard.bundle.redoc;

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
 * A Dropwizard bundle that serves ReDoc API documentation UI.
 * ReDoc provides a clean, responsive three-panel layout for viewing OpenAPI/Swagger specifications.
 *
 * <p>This bundle serves:
 * <ul>
 *   <li>ReDoc library files from the webjar at /redoc-static</li>
 *   <li>A custom HTML page at /redoc that initializes ReDoc with the Swagger spec</li>
 * </ul>
 *
 * <p>The bundle uses AutoService for automatic discovery and registration.
 *
 * @see cool.klass.service.swagger.SwaggerSpecResource
 */
@AutoService(PrioritizedBundle.class)
public class ReDocBundle implements PrioritizedBundle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReDocBundle.class);

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void initializeWithMdc(@Nonnull Bootstrap<?> bootstrap) {
        LOGGER.info("Initializing {}.", this.getClass().getSimpleName());

        // Serve ReDoc library from webjar
        bootstrap.addBundle(new AssetsBundle(
            "/META-INF/resources/webjars/redoc/2.1.5/bundles",
            "/redoc-static",
            null,
            "redoc-static"
        ));

        // Serve custom ReDoc initialization page
        bootstrap.addBundle(new AssetsBundle(
            "/redoc-custom",
            "/redoc",
            "index.html",
            "redoc"
        ));
    }

    @Override
    public void runWithMdc(@Nonnull Object configuration, @Nonnull Environment environment) {
        LOGGER.info("Running {}.", this.getClass().getSimpleName());
    }
}
