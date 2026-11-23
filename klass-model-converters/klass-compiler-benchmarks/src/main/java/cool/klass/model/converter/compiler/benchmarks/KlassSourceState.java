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
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * The Klass model each benchmark runs on, loaded from a domain-model module on the classpath once per trial.
 */
@State(Scope.Benchmark)
public class KlassSourceState
{
	@Param({
		"/klass/model/meta/domain/meta.klass",
		"/com/stackoverflow/stackoverflow.klass",
		"/cool/klass/xample/coverage/coverage-example.klass",
		"/com/repro/reladomo/tomanyabstract/reladomo-to-many-abstract.klass",
	})
	public String sourceName;

	private String sourceCodeText;

	@Setup
	public void setUp()
	{
		this.sourceCodeText = FileSlurper.slurp(this.sourceName, this.getClass());
	}

	public String getSourceName()
	{
		return this.sourceName;
	}

	public String getSourceCodeText()
	{
		return this.sourceCodeText;
	}
}
