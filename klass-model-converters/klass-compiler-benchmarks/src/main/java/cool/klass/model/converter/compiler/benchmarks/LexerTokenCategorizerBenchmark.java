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

import cool.klass.model.converter.compiler.token.categories.TokenCategory;
import cool.klass.model.converter.compiler.token.categorizing.lexer.LexerBasedTokenCategorizer;
import cool.klass.model.meta.grammar.KlassLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.map.MapIterable;
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
 * Measures lexer-based token categorization alone, over a token stream that is lexed once up front.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
public class LexerTokenCategorizerBenchmark
{
	@Param({ KlassSources.STACKOVERFLOW, KlassSources.COVERAGE_EXAMPLE })
	public String sourceName;

	private CommonTokenStream tokenStream;

	@Setup
	public void setUp()
	{
		String sourceCodeText = KlassSources.read(this.sourceName);
		var lexer = new KlassLexer(CharStreams.fromString(sourceCodeText, this.sourceName));
		this.tokenStream = new CommonTokenStream(lexer);
		this.tokenStream.fill();
	}

	@Benchmark
	public MapIterable<Token, TokenCategory> categorizeTokens()
	{
		return LexerBasedTokenCategorizer.findTokenCategoriesFromLexer(this.tokenStream);
	}
}
