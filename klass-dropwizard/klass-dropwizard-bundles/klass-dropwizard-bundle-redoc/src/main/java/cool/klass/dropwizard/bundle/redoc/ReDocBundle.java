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

package cool.klass.dropwizard.bundle.redoc;

import javax.annotation.Nonnull;

import com.google.auto.service.AutoService;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.liftwizard.dropwizard.bundle.prioritized.PrioritizedBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dropwizard bundle that serves ReDoc API documentation UI.
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

		bootstrap.addBundle(
			new AssetsBundle("/META-INF/resources/webjars/redoc/2.5.1", "/redoc-static", null, "redoc-static")
		);

		bootstrap.addBundle(new AssetsBundle("/redoc-custom", "/redoc", "index.html", "redoc"));
	}

	@Override
	public void runWithMdc(@Nonnull Object configuration, @Nonnull Environment environment) {
		LOGGER.info("Running {}.", this.getClass().getSimpleName());
	}
}
