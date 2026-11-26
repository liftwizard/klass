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

package cool.klass.model.converter.compiler.token.categories;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MapIterable;

/**
 * The token categories of a compilation, from the lexer and the parser, which never categorize the same token.
 */
public final class TokenCategories
{
	@Nonnull
	private final MapIterable<Token, TokenCategory> tokenCategoriesFromLexer;

	@Nonnull
	private final MapIterable<Token, TokenCategory> tokenCategoriesFromParser;

	public TokenCategories(
		@Nonnull MapIterable<Token, TokenCategory> tokenCategoriesFromLexer,
		@Nonnull MapIterable<Token, TokenCategory> tokenCategoriesFromParser
	)
	{
		this.tokenCategoriesFromLexer = Objects.requireNonNull(tokenCategoriesFromLexer);
		this.tokenCategoriesFromParser = Objects.requireNonNull(tokenCategoriesFromParser);

		ImmutableList<Token> duplicateTokens = tokenCategoriesFromLexer
			.keysView()
			.select(tokenCategoriesFromParser::containsKey)
			.toImmutableList();
		if (duplicateTokens.notEmpty())
		{
			throw new AssertionError(duplicateTokens);
		}
	}

	@Nonnull
	public Optional<TokenCategory> getTokenCategory(@Nonnull Token token)
	{
		TokenCategory lexerCategory = this.tokenCategoriesFromLexer.get(token);
		if (lexerCategory != null)
		{
			return Optional.of(lexerCategory);
		}
		return Optional.ofNullable(this.tokenCategoriesFromParser.get(token));
	}
}
