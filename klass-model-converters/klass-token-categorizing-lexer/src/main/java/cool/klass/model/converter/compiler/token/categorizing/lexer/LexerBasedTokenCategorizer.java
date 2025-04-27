/*
 * Copyright 2024 Craig Motlin
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

        return switch (token.getType()) {
            case KlassLexer.StringLiteral -> TokenCategory.STRING_LITERAL;
            case KlassLexer.IntegerLiteral -> TokenCategory.INTEGER_LITERAL;
            case KlassLexer.BooleanLiteral -> TokenCategory.BOOLEAN_LITERAL;
            case KlassLexer.CharacterLiteral -> TokenCategory.CHARACTER_LITERAL;
            case KlassLexer.FloatingPointLiteral -> TokenCategory.FLOATING_POINT_LITERAL;
            case KlassLexer.PUNCTUATION_LPAREN -> TokenCategory.PARENTHESIS_LEFT;
            case KlassLexer.PUNCTUATION_RPAREN -> TokenCategory.PARENTHESIS_RIGHT;
            case KlassLexer.PUNCTUATION_LBRACE -> TokenCategory.CURLY_LEFT;
            case KlassLexer.PUNCTUATION_RBRACE -> TokenCategory.CURLY_RIGHT;
            case KlassLexer.PUNCTUATION_LBRACK -> TokenCategory.SQUARE_BRACKET_LEFT;
            case KlassLexer.PUNCTUATION_RBRACK -> TokenCategory.SQUARE_BRACKET_RIGHT;
            case KlassLexer.PUNCTUATION_COLON -> TokenCategory.COLON;
            case KlassLexer.PUNCTUATION_SLASH -> TokenCategory.SLASH;
            case KlassLexer.PUNCTUATION_QUESTION -> TokenCategory.QUESTION;
            case KlassLexer.PUNCTUATION_COMMA -> TokenCategory.COMMA;
            case KlassLexer.PUNCTUATION_DOTDOT -> TokenCategory.DOTDOT;
            case KlassLexer.PUNCTUATION_DOT -> TokenCategory.DOT;
            case KlassLexer.PUNCTUATION_SEMI -> TokenCategory.SEMICOLON;
            case KlassLexer.PUNCTUATION_AMPERSAND -> TokenCategory.AMPERSAND;
            case KlassLexer.OPERATOR_EQ -> TokenCategory.OPERATOR_EQ;
            case KlassLexer.OPERATOR_NE -> TokenCategory.OPERATOR_NE;
            case KlassLexer.OPERATOR_LT -> TokenCategory.OPERATOR_LT;
            case KlassLexer.OPERATOR_GT -> TokenCategory.OPERATOR_GT;
            case KlassLexer.OPERATOR_LE -> TokenCategory.OPERATOR_LE;
            case KlassLexer.OPERATOR_GE -> TokenCategory.OPERATOR_GE;
            case KlassLexer.OPERATOR_AND -> TokenCategory.OPERATOR_AND;
            case KlassLexer.OPERATOR_OR -> TokenCategory.OPERATOR_OR;
            default -> null;
        };
    }
}
