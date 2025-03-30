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

package cool.klass.model.converter.compiler.annotation;

import javax.annotation.Nonnull;

import cool.klass.model.meta.grammar.KlassLexer;
import org.antlr.v4.runtime.Token;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

public final class LexicalColorizer {

    private LexicalColorizer() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static String colorize(@Nonnull Token token) {
        String text = token.getText();

        if (token.getChannel() == KlassLexer.WHITESPACE_CHANNEL) {
            return text;
        }

        switch (token.getType()) {
            case KlassLexer.MODIFIER_CLASSIFIER_SYSTEM_TEMPORAL:
            case KlassLexer.MODIFIER_CLASSIFIER_VALID_TEMPORAL:
            case KlassLexer.MODIFIER_CLASSIFIER_BITEMPORAL:
            case KlassLexer.MODIFIER_CLASSIFIER_VERSIONED:
            case KlassLexer.MODIFIER_CLASSIFIER_AUDITED:
            case KlassLexer.MODIFIER_CLASSIFIER_TRANSIENT:
            case KlassLexer.MODIFIER_PROPERTY_KEY:
            case KlassLexer.MODIFIER_PROPERTY_PRIVATE:
            case KlassLexer.MODIFIER_PROPERTY_VALID:
            case KlassLexer.MODIFIER_PROPERTY_SYSTEM:
            case KlassLexer.MODIFIER_PROPERTY_FROM:
            case KlassLexer.MODIFIER_PROPERTY_TO:
            case KlassLexer.MODIFIER_PROPERTY_CREATED_BY:
            case KlassLexer.MODIFIER_PROPERTY_CREATED_ON:
            case KlassLexer.MODIFIER_PROPERTY_LAST_UPDATED_BY:
            case KlassLexer.MODIFIER_PROPERTY_DERIVED: {
                return getStringBright(text, Color.GREEN);
            }
            case KlassLexer.MODIFIER_ASSOCIATION_END_OWNED:
            case KlassLexer.MODIFIER_ASSOCIATION_END_FINAL: {
                return getStringBright(text, Color.GREEN);
            }
            case KlassLexer.MODIFIER_VERSION:
            case KlassLexer.MODIFIER_USER_ID:
            case KlassLexer.MODIFIER_ID: {
                return getStringBright(text, Color.GREEN);
            }
            case KlassLexer.VALIDATION_MIN_LENGTH:
            case KlassLexer.VALIDATION_MINIMUM_LENGTH:
            case KlassLexer.VALIDATION_MAX_LENGTH:
            case KlassLexer.VALIDATION_MAXIMUM_LENGTH:
            case KlassLexer.VALIDATION_MIN:
            case KlassLexer.VALIDATION_MINIMUM:
            case KlassLexer.VALIDATION_MAX:
            case KlassLexer.VALIDATION_MAXIMUM: {
                return getStringBright(text, Color.GREEN);
            }
            // Keywords
            case KlassLexer.KEYWORD_PACKAGE:
            case KlassLexer.KEYWORD_ENUMERATION:
            case KlassLexer.KEYWORD_INTERFACE:
            case KlassLexer.KEYWORD_CLASS:
            case KlassLexer.KEYWORD_ASSOCIATION:
            case KlassLexer.KEYWORD_PROJECTION:
            case KlassLexer.KEYWORD_SERVICE: {
                return getStringDim(text, Color.MAGENTA);
            }
            case KlassLexer.KEYWORD_USER:
            case KlassLexer.KEYWORD_NATIVE:
            case KlassLexer.KEYWORD_RELATIONSHIP:
            case KlassLexer.KEYWORD_MULTIPLICITY:
            case KlassLexer.KEYWORD_ORDER_BY:
            case KlassLexer.KEYWORD_CRITERIA:
            case KlassLexer.KEYWORD_ON:
            case KlassLexer.KEYWORD_ABSTRACT:
            case KlassLexer.KEYWORD_EXTENDS:
            case KlassLexer.KEYWORD_IMPLEMENTS:
            case KlassLexer.KEYWORD_TABLE_PER_CLASS:
            case KlassLexer.KEYWORD_TABLE_PER_SUBCLASS:
            case KlassLexer.KEYWORD_TABLE_FOR_ALL_SUBCLASSES: {
                return getStringBright(text, Color.MAGENTA);
            }
            case KlassLexer.LITERAL_NULL:
            case KlassLexer.LITERAL_THIS: {
                return getStringBright(text, Color.GREEN);
            }
            // Primitives
            case KlassLexer.PRIMITIVE_TYPE_BOOLEAN:
            case KlassLexer.PRIMITIVE_TYPE_INTEGER:
            case KlassLexer.PRIMITIVE_TYPE_LONG:
            case KlassLexer.PRIMITIVE_TYPE_DOUBLE:
            case KlassLexer.PRIMITIVE_TYPE_FLOAT:
            case KlassLexer.PRIMITIVE_TYPE_STRING:
            case KlassLexer.PRIMITIVE_TYPE_INSTANT:
            case KlassLexer.PRIMITIVE_TYPE_LOCAL_DATE:
            case KlassLexer.PRIMITIVE_TYPE_TEMPORAL_INSTANT:
            case KlassLexer.PRIMITIVE_TYPE_TEMPORAL_RANGE: {
                return getStringBright(text, Color.MAGENTA);
            }
            // Literals
            case KlassLexer.StringLiteral:
            case KlassLexer.IntegerLiteral:
            case KlassLexer.BooleanLiteral:
            case KlassLexer.CharacterLiteral:
            case KlassLexer.FloatingPointLiteral:
            case KlassLexer.PUNCTUATION_ASTERISK: {
                return getStringBright(text, Color.BLUE);
            }
            case KlassLexer.Identifier: {
                return getStringBright(text, Color.DEFAULT);
            }
            case KlassLexer.PUNCTUATION_LPAREN:
            case KlassLexer.PUNCTUATION_RPAREN:
            case KlassLexer.PUNCTUATION_LBRACE:
            case KlassLexer.PUNCTUATION_RBRACE:
            case KlassLexer.PUNCTUATION_LBRACK:
            case KlassLexer.PUNCTUATION_RBRACK:
            case KlassLexer.PUNCTUATION_SEMI:
            case KlassLexer.PUNCTUATION_COLON:
            case KlassLexer.PUNCTUATION_COMMA:
            case KlassLexer.PUNCTUATION_DOT:
            case KlassLexer.PUNCTUATION_DOTDOT:
            case KlassLexer.PUNCTUATION_SLASH:
            case KlassLexer.PUNCTUATION_QUESTION: {
                return getStringDim(text, Color.CYAN);
            }
            // Operators
            case KlassLexer.OPERATOR_EQ: {
                return getStringDim(text, Color.MAGENTA);
            }
            // Verbs
            case KlassLexer.VERB_GET:
            case KlassLexer.VERB_POST:
            case KlassLexer.VERB_PUT:
            case KlassLexer.VERB_PATCH:
            case KlassLexer.VERB_DELETE: {
                return getStringBright(text, Color.GREEN);
            }
            default: {
                return getStringBright(text, Color.RED);
            }
        }
    }

    private static String getStringBright(String text, @Nonnull Color color) {
        return Ansi.ansi().fg(color).a(text).toString();
    }

    private static String getStringDim(String text, @Nonnull Color color) {
        return Ansi.ansi().fg(color).a(text).toString();
    }
}
