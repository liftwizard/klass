/*
 * Copyright 2025 Craig Motlin
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

package cool.klass.model.converter.compiler.syntax.highlighter.ansi.util;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.token.categories.TokenCategory;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.map.MapIterable;

/**
 * Utility class for formatting tokens and resolving token categories.
 * Consolidates common token-related logic used across ANSI syntax highlighting classes.
 */
public final class TokenFormatUtils {

    private TokenFormatUtils() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Formats token text for display, applying special formatting for whitespace and newlines.
     *
     * @param token    The token to format
     * @param category The token's category (may be null)
     * @return The formatted token text
     */
    @Nonnull
    public static String formatTokenText(@Nonnull Token token, @Nullable TokenCategory category) {
        String tokenText = token.getText();

        if (category == TokenCategory.WHITESPACE) {
            return tokenText.replace(' ', '·');
        } else if (category == TokenCategory.NEWLINE) {
            return "¶";
        }

        return tokenText;
    }

    /**
     * Resolves the token category from lexer and parser maps.
     * Ensures that a token doesn't have categories from both sources.
     *
     * @param token                     The token to resolve
     * @param tokenCategoriesFromLexer  Categories identified by the lexer
     * @param tokenCategoriesFromParser Categories identified by the parser
     * @return The token's category, or empty if not found
     * @throws AssertionError if the token has both lexer and parser categories
     */
    @Nonnull
    public static Optional<TokenCategory> getTokenCategory(
        @Nonnull Token token,
        @Nonnull MapIterable<Token, TokenCategory> tokenCategoriesFromLexer,
        @Nonnull MapIterable<Token, TokenCategory> tokenCategoriesFromParser
    ) {
        TokenCategory lexerCategory = tokenCategoriesFromLexer.get(token);
        TokenCategory parserCategory = tokenCategoriesFromParser.get(token);

        if (lexerCategory != null && parserCategory != null) {
            throw new AssertionError("Token has both lexer and parser categories: " + token.getText());
        }

        if (lexerCategory != null) {
            return Optional.of(lexerCategory);
        }

        if (parserCategory != null) {
            return Optional.of(parserCategory);
        }

        return Optional.empty();
    }
}
