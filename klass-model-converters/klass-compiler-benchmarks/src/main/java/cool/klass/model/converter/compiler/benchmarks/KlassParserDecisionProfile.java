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

import java.util.Arrays;
import java.util.Comparator;

import cool.klass.model.meta.grammar.KlassLexer;
import cool.klass.model.meta.grammar.KlassParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.DecisionInfo;
import org.antlr.v4.runtime.atn.PredictionMode;

/**
 * Parses the corpus with ANTLR's decision profiling on and prints the most expensive grammar decisions.
 *
 * <p>Lookahead and full-context fallback counts are deterministic, so they compare grammar changes without
 * depending on machine load. Usage: {@code KlassParserDecisionProfile [SLL|LL] [top]}.
 */
public final class KlassParserDecisionProfile
{
	private KlassParserDecisionProfile()
	{
		throw new AssertionError("Suppress default constructor for noninstantiability");
	}

	public static void main(String[] args)
	{
		PredictionMode predictionMode =
			args.length > 0 && "SLL".equals(args[0]) ? PredictionMode.SLL : PredictionMode.LL;
		int top = args.length > 1 ? Integer.parseInt(args[1]) : 15;

		DecisionInfo[] totals = null;
		int syntaxErrors = 0;
		for (String sourceName : KlassSources.ALL)
		{
			var lexer = new KlassLexer(CharStreams.fromString(KlassSources.read(sourceName), sourceName));
			var parser = new KlassParser(new CommonTokenStream(lexer));
			parser.getInterpreter().setPredictionMode(predictionMode);
			parser.setProfile(true);
			parser.compilationUnit();
			syntaxErrors += parser.getNumberOfSyntaxErrors();
			DecisionInfo[] decisions = parser.getParseInfo().getDecisionInfo();
			if (totals == null)
			{
				totals = decisions;
			}
			else
			{
				for (int i = 0; i < decisions.length; i++)
				{
					add(totals[i], decisions[i]);
				}
			}
		}

		long sllLook = Arrays.stream(totals)
			.mapToLong((d) -> d.SLL_TotalLook)
			.sum();
		long llLook = Arrays.stream(totals)
			.mapToLong((d) -> d.LL_TotalLook)
			.sum();
		long llFallback = Arrays.stream(totals)
			.mapToLong((d) -> d.LL_Fallback)
			.sum();
		long ambiguities = Arrays.stream(totals)
			.mapToLong((d) -> d.ambiguities.size())
			.sum();
		System.out.printf(
			"mode=%s syntaxErrors=%d SLL_TotalLook=%d LL_TotalLook=%d LL_Fallback=%d ambiguities=%d%n",
			predictionMode,
			syntaxErrors,
			sllLook,
			llLook,
			llFallback,
			ambiguities
		);

		Arrays.stream(totals)
			.sorted(Comparator.comparingLong((DecisionInfo d) -> d.SLL_TotalLook + d.LL_TotalLook).reversed())
			.limit(top)
			.forEach((d) ->
				System.out.printf(
					"  decision %3d %-32s invocations=%6d SLL_look=%7d (max %3d) LL_fallback=%5d LL_look=%7d (max %3d)%n",
					d.decision,
					KlassParser.ruleNames[KlassParser._ATN.getDecisionState(d.decision).ruleIndex],
					d.invocations,
					d.SLL_TotalLook,
					d.SLL_MaxLook,
					d.LL_Fallback,
					d.LL_TotalLook,
					d.LL_MaxLook
				)
			);
	}

	private static void add(DecisionInfo total, DecisionInfo decision)
	{
		total.invocations += decision.invocations;
		total.SLL_TotalLook += decision.SLL_TotalLook;
		total.SLL_MaxLook = Math.max(total.SLL_MaxLook, decision.SLL_MaxLook);
		total.LL_Fallback += decision.LL_Fallback;
		total.LL_TotalLook += decision.LL_TotalLook;
		total.LL_MaxLook = Math.max(total.LL_MaxLook, decision.LL_MaxLook);
		total.ambiguities.addAll(decision.ambiguities);
	}
}
