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

/**
 * Classpath locations of the Klass models that the benchmarks compile, loaded from the domain-model modules' resources.
 */
final class KlassSources
{
	static final String META = "/klass/model/meta/domain/meta.klass";
	static final String STACKOVERFLOW = "/com/stackoverflow/stackoverflow.klass";
	static final String COVERAGE_EXAMPLE = "/cool/klass/xample/coverage/coverage-example.klass";
	static final String RELADOMO_TO_MANY_ABSTRACT =
		"/com/repro/reladomo/tomanyabstract/reladomo-to-many-abstract.klass";

	private KlassSources()
	{
		throw new AssertionError("Suppress default constructor for noninstantiability");
	}
}
