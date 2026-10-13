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

package cool.klass.model.converter.compiler.benchmarks;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

final class KlassSources {

	static final String STACKOVERFLOW = "/com/stackoverflow/stackoverflow.klass";
	static final String COVERAGE_EXAMPLE = "/cool/klass/xample/coverage/coverage-example.klass";
	static final String META = "/klass/model/meta/domain/meta.klass";

	private static final String CORPUS = "/cool/klass/model/converter/compiler/benchmarks/corpus/";

	/**
	 * Every Klass model in the repository that compiles without errors.
	 */
	static final ImmutableList<String> ALL = Lists.immutable.with(
		META,
		CORPUS + "factorio-prints.klass",
		COVERAGE_EXAMPLE,
		STACKOVERFLOW,
		CORPUS + "projectionOnInterface.klass",
		CORPUS + "emoji.klass",
		CORPUS + "reladomo-to-many-abstract.klass"
	);

	private KlassSources() {
		throw new AssertionError("Suppress default constructor for noninstantiability");
	}

	static String read(String resourceName) {
		try (InputStream inputStream = KlassSources.class.getResourceAsStream(resourceName)) {
			if (inputStream == null) {
				throw new IllegalArgumentException("Klass source not found on the classpath: " + resourceName);
			}
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
