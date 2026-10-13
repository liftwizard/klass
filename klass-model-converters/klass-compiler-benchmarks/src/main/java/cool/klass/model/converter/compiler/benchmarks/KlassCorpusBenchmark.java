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

import java.util.concurrent.TimeUnit;

import cool.klass.model.converter.compiler.CompilationResult;
import org.eclipse.collections.api.list.ImmutableList;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Measures compiling every Klass model in the repository, one domain model at a time.
 *
 * <p>To profile the compiler, run {@code just bench-profile}, which runs this benchmark under JMH's JFR profiler; pass another JMH profiler name such as {@code async} or {@code gc} to use it instead.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 3)
@Measurement(iterations = 10, time = 3)
@Fork(3)
public class KlassCorpusBenchmark
{
	private KlassCorpusCompiler corpusCompiler;

	@Setup
	public void setUp()
	{
		this.corpusCompiler = new KlassCorpusCompiler();
	}

	@Benchmark
	public ImmutableList<CompilationResult> compileCorpus()
	{
		return this.corpusCompiler.compileAll();
	}
}
