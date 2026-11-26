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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import cool.klass.model.converter.compiler.CompilationUnit;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.TokenStream;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.list.mutable.ListAdapter;

public class TokenLine
{
	private final int line;
	private final ImmutableList<Token> tokens;
	private final CompilationUnit compilationUnit;
	private final boolean generated;

	public TokenLine(int line, ImmutableList<Token> tokens, CompilationUnit compilationUnit, boolean generated)
	{
		this.line = line;
		this.tokens = Objects.requireNonNull(tokens);
		this.compilationUnit = Objects.requireNonNull(compilationUnit);
		this.generated = generated;

		TokenSource tokenSource = compilationUnit.getTokenStream().getTokenSource();
		if (tokens.anySatisfy((token) -> token.getTokenSource() != tokenSource))
		{
			throw new AssertionError("Expected every token on line " + line + " to come from " + compilationUnit);
		}
	}

	/**
	 * {@return the lines of one compilation unit containing the context tokens, each widened to the whole source line}
	 */
	static ImmutableList<TokenLine> fromContextTokens(
		SetIterable<Token> unsortedContextTokens,
		CompilationUnit compilationUnit,
		boolean generated
	)
	{
		if (unsortedContextTokens.isEmpty())
		{
			throw new AssertionError("Expected context tokens in " + compilationUnit);
		}

		ListIterable<Token> contextTokens = unsortedContextTokens.toSortedListBy(Token::getTokenIndex);
		var tokenStream = (CommonTokenStream) compilationUnit.getTokenStream();
		Deque<Token> currentLine = new ArrayDeque<>();
		Token currentToken = null;
		MutableList<TokenLine> tokenLines = Lists.mutable.empty();
		for (Token nextToken : contextTokens)
		{
			if (currentLine.isEmpty())
			{
				TokenLine.startLine(currentLine, nextToken, tokenStream);
				currentToken = nextToken;
			}
			else if (currentToken.getLine() == nextToken.getLine())
			{
				TokenLine.endLine(currentLine, nextToken, tokenStream);
				currentToken = nextToken;
			}
			else
			{
				tokenLines.add(
					new TokenLine(
						currentToken.getLine(),
						Lists.immutable.withAll(currentLine),
						compilationUnit,
						generated
					)
				);
				currentLine.clear();

				TokenLine.startLine(currentLine, nextToken, tokenStream);
				currentToken = nextToken;
			}
		}
		tokenLines.add(
			new TokenLine(currentToken.getLine(), Lists.immutable.withAll(currentLine), compilationUnit, generated)
		);
		return tokenLines.toImmutable();
	}

	private static void startLine(Deque<Token> currentLine, Token nextToken, CommonTokenStream tokenStream)
	{
		int beginTokenIndex = TokenLine.getBeginTokenIndex(nextToken.getLine(), nextToken.getTokenIndex(), tokenStream);
		List<Token> tokenRange = tokenStream.get(beginTokenIndex, nextToken.getTokenIndex() - 1);
		MutableList<Token> tokens = tokenRange == null ? Lists.mutable.empty() : ListAdapter.adapt(tokenRange);
		currentLine.addAll(tokens);
		currentLine.add(nextToken);
	}

	private static void endLine(Deque<Token> currentLine, Token nextToken, CommonTokenStream tokenStream)
	{
		int endTokenIndex = TokenLine.getEndTokenIndex(nextToken.getLine(), nextToken.getTokenIndex(), tokenStream);
		List<Token> tokenRange = tokenStream.get(endTokenIndex, nextToken.getTokenIndex() - 1);
		MutableList<Token> tokens = tokenRange == null ? Lists.mutable.empty() : ListAdapter.adapt(tokenRange);
		currentLine.add(nextToken);
		currentLine.addAll(tokens);
	}

	private static int getBeginTokenIndex(int startLine, int startTokenIndex, TokenStream tokenStream)
	{
		int beginTokenIndex = startTokenIndex;
		while (beginTokenIndex > 0 && tokenStream.get(beginTokenIndex - 1).getLine() == startLine)
		{
			beginTokenIndex -= 1;
		}
		return beginTokenIndex;
	}

	private static int getEndTokenIndex(int stopLine, int stopTokenIndex, TokenStream tokenStream)
	{
		int endTokenIndex = stopTokenIndex;
		while (endTokenIndex + 1 < tokenStream.size() && tokenStream.get(endTokenIndex + 1).getLine() == stopLine)
		{
			endTokenIndex += 1;
		}
		return endTokenIndex;
	}

	public int getLine()
	{
		return this.line;
	}

	public ImmutableList<Token> getTokens()
	{
		return this.tokens;
	}

	public CompilationUnit getCompilationUnit()
	{
		return this.compilationUnit;
	}

	/**
	 * {@return whether this line comes from an in-place macro spliced into the surrounding unit's lines}
	 */
	public boolean isGenerated()
	{
		return this.generated;
	}

	@Override
	public String toString()
	{
		return String.format("%2d: %s", this.line, this.tokens.collect(Token::getText).makeString(""));
	}
}
