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

package cool.klass.model.converter.compiler.annotation;

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.partition.set.PartitionSet;
import org.eclipse.collections.api.set.SetIterable;

/**
 * A snippet from a root compilation unit with the lines of an in-place macro's compilation unit spliced in.
 * An in-place macro's parse tree is appended as the last child of the context it is grafted under, so its lines go after the last root line at or before that context's stop token.
 */
sealed interface GraftedSnippetSource
	extends SnippetSource
	permits GraftedCodeSource, GraftedContextSource
{
	@Nonnull
	CompilationUnit rootCompilationUnit();

	@Nonnull
	CompilationUnit graftedCompilationUnit();

	static void validate(@Nonnull CompilationUnit rootCompilationUnit, @Nonnull CompilationUnit graftedCompilationUnit)
	{
		Objects.requireNonNull(rootCompilationUnit);
		Objects.requireNonNull(graftedCompilationUnit);
		if (rootCompilationUnit == graftedCompilationUnit)
		{
			throw new AssertionError("Expected two different compilation units, but got " + rootCompilationUnit);
		}
		if (rootCompilationUnit.isGrafted())
		{
			throw new AssertionError(
				"Expected " + rootCompilationUnit + " not to be grafted into another compilation unit"
			);
		}
		boolean graftedIntoRoot = graftedCompilationUnit
			.getGraftContext()
			.map((graftContext) -> graftContext.getStop().getTokenSource())
			.filter((tokenSource) -> tokenSource == rootCompilationUnit.getTokenStream().getTokenSource())
			.isPresent();
		if (!graftedIntoRoot)
		{
			throw new AssertionError(
				"Expected " + graftedCompilationUnit + " to be grafted into " + rootCompilationUnit
			);
		}
	}

	@Nonnull
	@Override
	default CompilationUnit getCompilationUnit(@Nonnull Token token)
	{
		if (token.getTokenSource() == this.rootCompilationUnit().getTokenStream().getTokenSource())
		{
			return this.rootCompilationUnit();
		}
		if (token.getTokenSource() == this.graftedCompilationUnit().getTokenStream().getTokenSource())
		{
			return this.graftedCompilationUnit();
		}
		throw new AssertionError(
			"Expected token " +
				token +
				" to come from " +
				this.rootCompilationUnit() +
				" or " +
				this.graftedCompilationUnit()
		);
	}

	@Nonnull
	@Override
	default ImmutableList<TokenLine> getTokenLines(@Nonnull SetIterable<Token> contextTokens)
	{
		PartitionSet<Token> partition = contextTokens.partition(
			(token) -> this.getCompilationUnit(token) == this.rootCompilationUnit()
		);
		ImmutableList<TokenLine> rootTokenLines = TokenLine.fromContextTokens(
			partition.getSelected(),
			this.rootCompilationUnit(),
			false
		);
		ImmutableList<TokenLine> graftedTokenLines = TokenLine.fromContextTokens(
			partition.getRejected(),
			this.graftedCompilationUnit(),
			true
		);

		int graftStopLine = this.graftedCompilationUnit().getGraftContext().orElseThrow().getStop().getLine();
		int spliceIndex = rootTokenLines.detectLastIndex((each) -> each.getLine() <= graftStopLine) + 1;
		return Lists.mutable
			.withAll(rootTokenLines.take(spliceIndex))
			.withAll(graftedTokenLines)
			.withAll(rootTokenLines.drop(spliceIndex))
			.toImmutable();
	}
}
