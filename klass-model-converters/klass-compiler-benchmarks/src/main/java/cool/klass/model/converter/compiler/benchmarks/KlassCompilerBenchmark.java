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

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import cool.klass.model.converter.compiler.CompilationResult;
import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.KlassCompiler;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Measures a full compilation of a Klass source file, from lexing through every compiler phase.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Fork(3)
public class KlassCompilerBenchmark {

	@Param({KlassSources.STACKOVERFLOW, KlassSources.COVERAGE_EXAMPLE})
	public String sourceName;

	private String sourceCodeText;
	private AnsiColorScheme colorScheme;

	@Setup
	public void setUp() {
		this.sourceCodeText = KlassSources.read(this.sourceName);
		this.colorScheme = ColorSchemeProvider.getByName("dark");
	}

	@Benchmark
	public CompilationResult compile() {
		CompilationUnit compilationUnit = CompilationUnit.createFromText(
			0,
			Optional.empty(),
			this.sourceName,
			this.sourceCodeText
		);
		return new KlassCompiler(compilationUnit, this.colorScheme).compile();
	}
}
