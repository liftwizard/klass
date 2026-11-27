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

package cool.klass.model.converter.compiler.decision;

import java.util.Comparator;

import cool.klass.model.meta.grammar.KlassLexer;
import cool.klass.model.meta.grammar.KlassParser;
import io.liftwizard.junit.extension.match.FileSlurper;
import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.DecisionInfo;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.dfa.DFA;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Records ANTLR's decision profile for the compiler test corpus in its own package, so its golden file shares a directory with no other golden file.
 */
class ParserDecisionProfileTest
{
	private static final ImmutableList<String> CORPUS = Lists.immutable.with(
		"/com/stackoverflow/stackoverflow.klass",
		"/cool/klass/model/converter/compiler/factorio-prints.klass",
		"/cool/klass/model/converter/compiler/emoji.klass",
		"/cool/klass/model/converter/compiler/projectionOnInterface.klass",
		"/cool/klass/xample/coverage/coverage-example.klass"
	);

	private static final int TOP_DECISIONS = 15;

	@RegisterExtension
	final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

	/**
	 * Records ANTLR's decision profile for the corpus, so a grammar change that adds lookahead or full-context fallback shows up as a golden-file diff.
	 */
	@Test
	void parserDecisionProfile()
	{
		String profile = Lists.immutable
			.with(PredictionMode.SLL, PredictionMode.LL)
			.collect(this::getDecisionProfile)
			.makeString("\n");
		this.fileMatchExtension.assertFileContents(
			this.getClass().getSimpleName() + ".parserDecisionProfile.txt",
			profile
		);
	}

	private String getDecisionProfile(PredictionMode predictionMode)
	{
		DecisionInfo[] totals = null;
		var syntaxErrors = 0;
		for (String sourceCodeName : CORPUS)
		{
			String sourceCodeText = FileSlurper.slurp(sourceCodeName, this.getClass());
			var lexer = new KlassLexer(CharStreams.fromString(sourceCodeText, sourceCodeName));
			var parser = new KlassParser(new CommonTokenStream(lexer));
			// A fresh DFA cache, so lookahead counts don't depend on what other tests parsed earlier in the JVM
			parser.setInterpreter(ParserDecisionProfileTest.getUncachedInterpreter(parser));
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
				for (var i = 0; i < decisions.length; i++)
				{
					ParserDecisionProfileTest.add(totals[i], decisions[i]);
				}
			}
		}

		MutableList<DecisionInfo> decisionInfos = ArrayAdapter.adapt(totals);
		String summary =
			"mode=%s syntaxErrors=%d SLL_TotalLook=%d LL_TotalLook=%d LL_Fallback=%d ambiguities=%d%n".formatted(
				predictionMode,
				syntaxErrors,
				decisionInfos.sumOfLong((d) -> d.SLL_TotalLook),
				decisionInfos.sumOfLong((d) -> d.LL_TotalLook),
				decisionInfos.sumOfLong((d) -> d.LL_Fallback),
				decisionInfos.sumOfLong((d) -> d.ambiguities.size())
			);
		String topDecisions = decisionInfos
			.toSortedList(
				Comparator.comparingLong((DecisionInfo d) -> d.SLL_TotalLook + d.LL_TotalLook)
					.reversed()
					.thenComparingInt((DecisionInfo d) -> d.decision)
			)
			.take(TOP_DECISIONS)
			.collect(ParserDecisionProfileTest::formatDecision)
			.makeString("");
		return summary + topDecisions;
	}

	private static ParserATNSimulator getUncachedInterpreter(KlassParser parser)
	{
		ATN atn = parser.getATN();
		DFA[] decisionToDFA = new DFA[atn.getNumberOfDecisions()];
		for (var i = 0; i < decisionToDFA.length; i++)
		{
			decisionToDFA[i] = new DFA(atn.getDecisionState(i), i);
		}
		return new ParserATNSimulator(parser, atn, decisionToDFA, new PredictionContextCache());
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

	private static String formatDecision(DecisionInfo d)
	{
		return "  decision %3d %-32s invocations=%6d SLL_look=%7d (max %3d) LL_fallback=%5d LL_look=%7d (max %3d)%n".formatted(
			d.decision,
			KlassParser.ruleNames[KlassParser._ATN.getDecisionState(d.decision).ruleIndex],
			d.invocations,
			d.SLL_TotalLook,
			d.SLL_MaxLook,
			d.LL_Fallback,
			d.LL_TotalLook,
			d.LL_MaxLook
		);
	}
}
