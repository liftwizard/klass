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

package cool.klass.model.converter.compiler;

import java.util.Comparator;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.annotation.AbstractCompilerAnnotation;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationRenderer;
import cool.klass.model.converter.compiler.annotation.RootCompilerAnnotation;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import cool.klass.model.meta.grammar.KlassLexer;
import cool.klass.model.meta.grammar.KlassParser;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.DecisionInfo;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(LogMarkerTestExtension.class)
class KlassCompilerTest
{
	private static final ImmutableList<String> CORPUS = Lists.immutable.with(
		"/com/stackoverflow/stackoverflow.klass",
		"factorio-prints.klass",
		"emoji.klass",
		"projectionOnInterface.klass",
		"/cool/klass/xample/coverage/coverage-example.klass"
	);

	private static final int TOP_DECISIONS = 15;

	@RegisterExtension
	final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

	@Test
	void stackOverflow()
	{
		this.assertNoCompilerErrors("/com/stackoverflow/stackoverflow.klass");
	}

	@Test
	void factorioPrints()
	{
		this.assertNoCompilerErrors("factorio-prints.klass");
	}

	@Test
	void emoji()
	{
		this.assertNoCompilerErrors("emoji.klass");
	}

	@Test
	void projectionOnInterface()
	{
		this.assertNoCompilerErrors("projectionOnInterface.klass");
	}

	@Test
	void coverageExample()
	{
		this.assertNoCompilerErrors("/cool/klass/xample/coverage/coverage-example.klass");
	}

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
					KlassCompilerTest.add(totals[i], decisions[i]);
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
			.collect(KlassCompilerTest::formatDecision)
			.makeString("");
		return summary + topDecisions;
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

	private void assertNoCompilerErrors(@Nonnull String sourceCodeName)
	{
		String sourceCodeText = FileSlurper.slurp(sourceCodeName, this.getClass());
		CompilationUnit compilationUnit = CompilationUnit.createFromText(
			0,
			Optional.empty(),
			sourceCodeName,
			sourceCodeText
		);
		AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("dark");
		var compiler = new KlassCompiler(compilationUnit);
		CompilationResult compilationResult = compiler.compile();
		ImmutableList<RootCompilerAnnotation> compilerAnnotations = compilationResult
			.compilerAnnotations()
			.select(AbstractCompilerAnnotation::isError);

		var compilerAnnotationRenderer = new CompilerAnnotationRenderer(
			colorScheme,
			compilationResult.tokenCategories(),
			false
		);
		String message = compilerAnnotations.collect(compilerAnnotationRenderer::render).makeString("\n");
		assertThat(compilerAnnotations).as(message).isEqualTo(Lists.immutable.empty());

		assertThat(compilationResult.domainModelWithSourceCode()).isPresent();
	}
}
