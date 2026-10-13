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

import io.liftwizard.junit.extension.match.FileSlurper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

final class KlassSources
{
	private static final String TEST_MODELS = "/cool/klass/model/converter/compiler/";

	/**
	 * The Klass models the benchmark compiles: every model in the repository except emoji.klass, which only checks that emoji parse.
	 */
	static final ImmutableList<String> ALL = Lists.immutable.with(
		"/klass/model/meta/domain/meta.klass",
		TEST_MODELS + "factorio-prints.klass",
		"/cool/klass/xample/coverage/coverage-example.klass",
		"/com/stackoverflow/stackoverflow.klass",
		TEST_MODELS + "projectionOnInterface.klass",
		"/com/repro/reladomo/tomanyabstract/reladomo-to-many-abstract.klass"
	);

	private KlassSources()
	{
		throw new AssertionError("Suppress default constructor for noninstantiability");
	}

	static String read(String resourceName)
	{
		return FileSlurper.slurp(resourceName, KlassSources.class);
	}
}
