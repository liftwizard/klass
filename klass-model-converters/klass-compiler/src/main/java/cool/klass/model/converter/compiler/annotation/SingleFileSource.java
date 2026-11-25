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
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.SetIterable;

/**
 * A snippet from one token stream with nothing spliced in, which covers almost every error.
 */
public record SingleFileSource(@Nonnull CompilationUnit compilationUnit)
	implements SnippetSource
{
	public SingleFileSource
	{
		Objects.requireNonNull(compilationUnit);
		if (compilationUnit.getParserContext().getParent() != null)
		{
			throw new AssertionError(
				"Expected " + compilationUnit + " not to be grafted into another compilation unit"
			);
		}
	}

	@Nonnull
	@Override
	public CompilationUnit annotatedCompilationUnit()
	{
		return this.compilationUnit;
	}

	@Nonnull
	@Override
	public CompilationUnit getCompilationUnit(@Nonnull Token token)
	{
		if (token.getTokenSource() != this.compilationUnit.getTokenStream().getTokenSource())
		{
			throw new AssertionError("Expected token " + token + " to come from " + this.compilationUnit);
		}
		return this.compilationUnit;
	}

	@Nonnull
	@Override
	public ImmutableList<TokenLine> getTokenLines(@Nonnull SetIterable<Token> contextTokens)
	{
		return TokenLine.fromContextTokens(contextTokens, this.compilationUnit, false);
	}
}
