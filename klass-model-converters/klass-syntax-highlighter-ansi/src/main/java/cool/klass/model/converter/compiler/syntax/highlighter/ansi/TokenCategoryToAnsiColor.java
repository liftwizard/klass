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

package cool.klass.model.converter.compiler.syntax.highlighter.ansi;

import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.token.categories.TokenCategory;
import org.fusesource.jansi.Ansi;

public final class TokenCategoryToAnsiColor {

    private TokenCategoryToAnsiColor() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static void applyColor(TokenCategory tokenCategory, Ansi ansi, AnsiColorScheme colorScheme) {
        switch (tokenCategory) {
            case BLOCK_COMMENT: {
                colorScheme.blockComment(ansi);
                return;
            }
            case LINE_COMMENT: {
                colorScheme.lineComment(ansi);
                return;
            }
            case KEYWORD:
            case WORD_OPERATOR:
            case OPERATOR_IN:
            case OPERATOR_STRING: {
                colorScheme.keyword(ansi);
                return;
            }
            case PACKAGE_KEYWORD: {
                colorScheme.packageKeyword(ansi);
                return;
            }
            case KEYWORD_ENUMERATION:
            case KEYWORD_INTERFACE:
            case KEYWORD_USER:
            case KEYWORD_CLASS:
            case KEYWORD_PROJECTION:
            case KEYWORD_SERVICE:
            case KEYWORD_ABSTRACT:
            case KEYWORD_EXTENDS:
            case KEYWORD_IMPLEMENTS:
            case KEYWORD_INHERITANCE_TYPE:
            case KEYWORD_ASSOCIATION:
            case KEYWORD_RELATIONSHIP:
            case KEYWORD_ORDER_BY:
            case KEYWORD_ORDER_BY_DIRECTION:
            case KEYWORD_ON:
            case KEYWORD_MULTIPLICITY:
            case KEYWORD_MULTIPLICITY_CHOICE:
            case KEYWORD_SERVICE_CRITERIA: {
                colorScheme.keyword(ansi);
                return;
            }
            case PRIMITIVE_TYPE: {
                colorScheme.primitiveType(ansi);
                return;
            }
            case VERB:
            case VERB_GET:
            case VERB_POST:
            case VERB_PUT:
            case VERB_PATCH:
            case VERB_DELETE: {
                colorScheme.verb(ansi);
                return;
            }
            case MODIFIER: {
                colorScheme.modifier(ansi);
                return;
            }
            case CLASSIFIER_MODIFIER: {
                colorScheme.classifierModifier(ansi);
                return;
            }
            case DATA_TYPE_PROPERTY_MODIFIER: {
                colorScheme.dataTypePropertyModifier(ansi);
                return;
            }
            case ASSOCIATION_END_MODIFIER: {
                colorScheme.associationEndModifier(ansi);
                return;
            }
            case PARAMETERIZED_PROPERTY_MODIFIER: {
                colorScheme.parameterizedPropertyModifier(ansi);
                return;
            }
            case PARAMETER_MODIFIER: {
                colorScheme.parameterModifier(ansi);
                return;
            }
            case VALIDATION_MODIFIER: {
                colorScheme.validationModifier(ansi);
                return;
            }
            case SERVICE_CATEGORY_MODIFIER: {
                colorScheme.serviceCategoryModifier(ansi);
                return;
            }
            case IDENTIFIER: {
                colorScheme.identifier(ansi);
                return;
            }
            case PACKAGE_NAME: {
                colorScheme.packageName(ansi);
                return;
            }
            case TOP_LEVEL_ELEMENT_NAME: {
                colorScheme.topLevelElementName(ansi);
                return;
            }
            case ENUMERATION_NAME: {
                colorScheme.enumerationName(ansi);
                return;
            }
            case CLASSIFIER_NAME: {
                colorScheme.classifierName(ansi);
                return;
            }
            case INTERFACE_NAME: {
                colorScheme.interfaceName(ansi);
                return;
            }
            case CLASS_NAME: {
                colorScheme.className(ansi);
                return;
            }
            case ASSOCIATION_NAME: {
                colorScheme.associationName(ansi);
                return;
            }
            case PROJECTION_NAME: {
                colorScheme.projectionName(ansi);
                return;
            }
            case SERVICE_NAME: {
                colorScheme.serviceName(ansi);
                return;
            }
            case ENUMERATION_LITERAL_NAME: {
                colorScheme.enumerationLiteralName(ansi);
                return;
            }
            case PARAMETER_NAME: {
                colorScheme.parameterName(ansi);
                return;
            }
            case PROPERTY_NAME: {
                colorScheme.propertyName(ansi);
                return;
            }
            case DATA_TYPE_PROPERTY_NAME: {
                colorScheme.dataTypePropertyName(ansi);
                return;
            }
            case PRIMITIVE_PROPERTY_NAME: {
                colorScheme.primitivePropertyName(ansi);
                return;
            }
            case ENUMERATION_PROPERTY_NAME: {
                colorScheme.enumerationPropertyName(ansi);
                return;
            }
            case REFERENCE_PROPERTY_NAME: {
                colorScheme.referencePropertyName(ansi);
                return;
            }
            case PARAMETERIZED_PROPERTY_NAME: {
                colorScheme.parameterizedPropertyName(ansi);
                return;
            }
            case ASSOCIATION_END_NAME: {
                colorScheme.associationEndName(ansi);
                return;
            }
            case ENUMERATION_REFERENCE: {
                colorScheme.enumerationReference(ansi);
                return;
            }
            case CLASSIFIER_REFERENCE: {
                colorScheme.classifierReference(ansi);
                return;
            }
            case INTERFACE_REFERENCE: {
                colorScheme.interfaceReference(ansi);
                return;
            }
            case CLASS_REFERENCE: {
                colorScheme.classReference(ansi);
                return;
            }
            case PROJECTION_REFERENCE: {
                colorScheme.projectionReference(ansi);
                return;
            }
            case DATA_TYPE_PROPERTY_REFERENCE: {
                colorScheme.dataTypePropertyReference(ansi);
                return;
            }
            case ASSOCIATION_END_REFERENCE: {
                colorScheme.associationEndReference(ansi);
                return;
            }
            case PARAMETERIZED_PROPERTY_REFERENCE: {
                colorScheme.parameterizedPropertyReference(ansi);
                return;
            }
            case PROPERTY_REFERENCE: {
                colorScheme.propertyReference(ansi);
                return;
            }
            case PARAMETER_REFERENCE: {
                colorScheme.parameterReference(ansi);
                return;
            }
            case LITERAL: {
                colorScheme.literal(ansi);
                return;
            }
            case LITERAL_THIS: {
                colorScheme.literalThis(ansi);
                return;
            }
            case LITERAL_NATIVE: {
                colorScheme.literalNative(ansi);
                return;
            }
            case STRING_LITERAL: {
                colorScheme.stringLiteral(ansi);
                return;
            }
            case INTEGER_LITERAL:
            case ASTERISK_LITERAL: {
                colorScheme.integerLiteral(ansi);
                return;
            }
            case BOOLEAN_LITERAL: {
                colorScheme.booleanLiteral(ansi);
                return;
            }
            case CHARACTER_LITERAL: {
                colorScheme.characterLiteral(ansi);
                return;
            }
            case FLOATING_POINT_LITERAL: {
                colorScheme.floatingPointLiteral(ansi);
                return;
            }
            case PUNCTUATION:
            case COLON:
            case SLASH:
            case QUESTION:
            case AMPERSAND:
            case PAIRED_PUNCTUATION:
            case PARENTHESES:
            case PARENTHESIS_LEFT:
            case PARENTHESIS_RIGHT:
            case CURLY_BRACES:
            case CURLY_LEFT:
            case CURLY_RIGHT:
            case SQUARE_BRACKETS:
            case SQUARE_BRACKET_LEFT:
            case SQUARE_BRACKET_RIGHT: {
                colorScheme.punctuation(ansi);
                return;
            }
            case COMMA: {
                colorScheme.comma(ansi);
                return;
            }
            case DOT: {
                colorScheme.dot(ansi);
                return;
            }
            case DOTDOT: {
                colorScheme.dotDot(ansi);
                return;
            }
            case SEMICOLON: {
                colorScheme.semi(ansi);
                return;
            }
            case OPERATOR:
            case OPERATOR_EQ:
            case OPERATOR_NE:
            case OPERATOR_LT:
            case OPERATOR_GT:
            case OPERATOR_LE:
            case OPERATOR_GE:
            case OPERATOR_AND:
            case OPERATOR_OR: {
                colorScheme.operator(ansi);
                return;
            }
            case URL_CONSTANT: {
                colorScheme.urlConstant(ansi);
                return;
            }
            default: {
                throw new AssertionError(tokenCategory);
            }
        }
    }
}
