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

package com.stackoverflow.dropwizard.test;

import javax.annotation.Nonnull;

import com.stackoverflow.dropwizard.application.StackOverflowApplication;
import io.dropwizard.testing.ResourceHelpers;
import io.liftwizard.dropwizard.testing.junit.AbstractDropwizardAppTest;
import io.liftwizard.junit.extension.app.LiftwizardAppExtension;

public abstract class AbstractStackOverflowApplicationTest extends AbstractDropwizardAppTest {

	@Nonnull
	@Override
	protected LiftwizardAppExtension<?> getDropwizardAppExtension() {
		return new LiftwizardAppExtension<>(
			StackOverflowApplication.class,
			ResourceHelpers.resourceFilePath("config-test.json5")
		);
	}
}
