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

package cool.klass.model.converter.compiler.token.categorizing.lexer;

import java.util.LinkedHashMap;

import cool.klass.model.converter.compiler.token.categories.TokenCategory;
import cool.klass.model.meta.grammar.KlassLexer;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMapIterable;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;

public final class LexerBasedTokenCategorizer {

	private LexerBasedTokenCategorizer() {
		throw new AssertionError("Suppress default constructor for noninstantiability");
	}

	public static MapIterable<Token, TokenCategory> findTokenCategoriesFromLexer(BufferedTokenStream tokenStream) {
		MutableMapIterable<Token, TokenCategory> tokenCategories = OrderedMapAdapter.adapt(new LinkedHashMap<>());
		findTokenCategoriesFromLexer(tokenStream, tokenCategories);
		return tokenCategories.asUnmodifiable();
	}

	public static void findTokenCategoriesFromLexer(
		BufferedTokenStream tokenStream,
		MutableMapIterable<Token, TokenCategory> tokenCategories
	) {
		for (Token token : tokenStream.getTokens()) {
			LexerBasedTokenCategorizer.findTokenCategoriesFromLexer(token, tokenCategories);
		}
	}

	private static void findTokenCategoriesFromLexer(
		Token token,
		MutableMapIterable<Token, TokenCategory> tokenCategories
	) {
		TokenCategory tokenCategory = LexerBasedTokenCategorizer.getTokenCategory(token);
		if (tokenCategory == null) {
			return;
		}

		TokenCategory duplicate = tokenCategories.put(token, tokenCategory);
		if (duplicate != null) {
			throw new AssertionError(token);
		}
	}

	private static TokenCategory getTokenCategory(Token token) {
		int channel = token.getChannel();
		if (channel == KlassLexer.COMMENTS_CHANNEL) {
			return TokenCategory.BLOCK_COMMENT;
		}
		if (channel == KlassLexer.LINE_COMMENTS_CHANNEL) {
			return TokenCategory.LINE_COMMENT;
		}

		int tokenType = token.getType();
		if (tokenType == Token.EOF) {
			return TokenCategory.END_OF_FILE;
		}

		String symbolicName = KlassLexer.VOCABULARY.getSymbolicName(tokenType);
		if (symbolicName == null) {
			return null;
		}
		return switch (symbolicName) {
			case "StringLiteral" -> TokenCategory.STRING_LITERAL;
			case "IntegerLiteral" -> TokenCategory.INTEGER_LITERAL;
			case "BooleanLiteral" -> TokenCategory.BOOLEAN_LITERAL;
			case "CharacterLiteral" -> TokenCategory.CHARACTER_LITERAL;
			case "FloatingPointLiteral" -> TokenCategory.FLOATING_POINT_LITERAL;
			case "PUNCTUATION_LPAREN" -> TokenCategory.PARENTHESIS_LEFT;
			case "PUNCTUATION_RPAREN" -> TokenCategory.PARENTHESIS_RIGHT;
			case "PUNCTUATION_LBRACE" -> TokenCategory.CURLY_LEFT;
			case "PUNCTUATION_RBRACE" -> TokenCategory.CURLY_RIGHT;
			case "PUNCTUATION_LBRACK" -> TokenCategory.SQUARE_BRACKET_LEFT;
			case "PUNCTUATION_RBRACK" -> TokenCategory.SQUARE_BRACKET_RIGHT;
			case "PUNCTUATION_COLON" -> TokenCategory.COLON;
			case "PUNCTUATION_SLASH" -> TokenCategory.SLASH;
			case "PUNCTUATION_QUESTION" -> TokenCategory.QUESTION;
			case "PUNCTUATION_COMMA" -> TokenCategory.COMMA;
			case "PUNCTUATION_DOTDOT" -> TokenCategory.DOTDOT;
			case "PUNCTUATION_DOT" -> TokenCategory.DOT;
			case "PUNCTUATION_SEMI" -> TokenCategory.SEMICOLON;
			case "PUNCTUATION_AMPERSAND" -> TokenCategory.AMPERSAND;
			case "PUNCTUATION_ASTERISK" -> TokenCategory.ASTERISK_LITERAL;
			case "OPERATOR_EQUALS" -> TokenCategory.OPERATOR_EQUALS;
			case "OPERATOR_NOT_EQUALS" -> TokenCategory.OPERATOR_NOT_EQUALS;
			case "OPERATOR_LESS_THAN" -> TokenCategory.OPERATOR_LESS_THAN;
			case "OPERATOR_GREATER_THAN" -> TokenCategory.OPERATOR_GREATER_THAN;
			case "OPERATOR_LESS_THAN_OR_EQUAL" -> TokenCategory.OPERATOR_LESS_THAN_OR_EQUAL;
			case "OPERATOR_GREATER_THAN_OR_EQUAL" -> TokenCategory.OPERATOR_GREATER_THAN_OR_EQUAL;
			case "OPERATOR_AND" -> TokenCategory.OPERATOR_AND;
			case "OPERATOR_OR" -> TokenCategory.OPERATOR_OR;
			case "WHITESPACE" -> TokenCategory.WHITESPACE;
			case "NEWLINE" -> TokenCategory.NEWLINE;
			// Let parser categorize UrlIdentifiers based on context
			case "UrlIdentifier" -> null;
			// Let parser categorize Identifiers based on context
			case "Identifier" -> null;
			default -> null;
		};
	}
}
