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
            case COMMENT:
                throw new AssertionError("Comment categories will always be more specific.");
            case BLOCK_COMMENT: {
                colorScheme.blockComment(ansi);
                return;
            }
            case LINE_COMMENT: {
                colorScheme.lineComment(ansi);
                return;
            }
            case KEYWORD: {
                colorScheme.keyword(ansi);
                return;
            }
            case WORD_OPERATOR: {
                colorScheme.wordOperator(ansi);
                return;
            }
            case OPERATOR_IN: {
                colorScheme.operatorIn(ansi);
                return;
            }
            case OPERATOR_STRING: {
                colorScheme.operatorString(ansi);
                return;
            }
            case PACKAGE_KEYWORD: {
                colorScheme.packageKeyword(ansi);
                return;
            }
            case KEYWORD_ENUMERATION: {
                colorScheme.keywordEnumeration(ansi);
                return;
            }
            case KEYWORD_INTERFACE: {
                colorScheme.keywordInterface(ansi);
                return;
            }
            case KEYWORD_USER: {
                colorScheme.keywordUser(ansi);
                return;
            }
            case KEYWORD_CLASS: {
                colorScheme.keywordClass(ansi);
                return;
            }
            case KEYWORD_PROJECTION: {
                colorScheme.keywordProjection(ansi);
                return;
            }
            case KEYWORD_SERVICE: {
                colorScheme.keywordService(ansi);
                return;
            }
            case KEYWORD_ABSTRACT: {
                colorScheme.keywordAbstract(ansi);
                return;
            }
            case KEYWORD_EXTENDS: {
                colorScheme.keywordExtends(ansi);
                return;
            }
            case KEYWORD_IMPLEMENTS: {
                colorScheme.keywordImplements(ansi);
                return;
            }
            case KEYWORD_INHERITANCE_TYPE: {
                colorScheme.keywordInheritanceType(ansi);
                return;
            }
            case KEYWORD_ASSOCIATION: {
                colorScheme.keywordAssociation(ansi);
                return;
            }
            case KEYWORD_RELATIONSHIP: {
                colorScheme.keywordRelationship(ansi);
                return;
            }
            case KEYWORD_ORDER_BY: {
                colorScheme.keywordOrderBy(ansi);
                return;
            }
            case KEYWORD_ORDER_BY_DIRECTION: {
                colorScheme.keywordOrderByDirection(ansi);
                return;
            }
            case KEYWORD_ON: {
                colorScheme.keywordOn(ansi);
                return;
            }
            case KEYWORD_MULTIPLICITY: {
                colorScheme.keywordMultiplicity(ansi);
                return;
            }
            case KEYWORD_MULTIPLICITY_CHOICE: {
                colorScheme.keywordMultiplicityChoice(ansi);
                return;
            }
            case KEYWORD_SERVICE_CRITERIA: {
                colorScheme.keywordServiceCriteria(ansi);
                return;
            }
            case PRIMITIVE_TYPE: {
                colorScheme.primitiveType(ansi);
                return;
            }
            case VERB: {
                colorScheme.verb(ansi);
                return;
            }
            case VERB_GET: {
                colorScheme.verbGet(ansi);
                return;
            }
            case VERB_POST: {
                colorScheme.verbPost(ansi);
                return;
            }
            case VERB_PUT: {
                colorScheme.verbPut(ansi);
                return;
            }
            case VERB_PATCH: {
                colorScheme.verbPatch(ansi);
                return;
            }
            case VERB_DELETE: {
                colorScheme.verbDelete(ansi);
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
            case PUNCTUATION: {
                colorScheme.punctuation(ansi);
                return;
            }
            case COLON: {
                colorScheme.colon(ansi);
                return;
            }
            case SLASH: {
                colorScheme.slash(ansi);
                return;
            }
            case QUESTION: {
                colorScheme.question(ansi);
                return;
            }
            case AMPERSAND: {
                colorScheme.ampersand(ansi);
                return;
            }
            case PAIRED_PUNCTUATION: {
                colorScheme.pairedPunctuation(ansi);
                return;
            }
            case PARENTHESES:
            case PARENTHESIS_LEFT:
            case PARENTHESIS_RIGHT: {
                colorScheme.parentheses(ansi);
                return;
            }
            case CURLY_BRACES:
            case CURLY_LEFT:
            case CURLY_RIGHT: {
                colorScheme.curlyBraces(ansi);
                return;
            }
            case SQUARE_BRACKETS:
            case SQUARE_BRACKET_LEFT:
            case SQUARE_BRACKET_RIGHT: {
                colorScheme.squareBrackets(ansi);
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
            case OPERATOR: {
                colorScheme.operator(ansi);
                return;
            }
            case OPERATOR_EQUALS: {
                colorScheme.operatorEquals(ansi);
                return;
            }
            case OPERATOR_NOT_EQUALS: {
                colorScheme.operatorNotEquals(ansi);
                return;
            }
            case OPERATOR_LESS_THAN: {
                colorScheme.operatorLessThan(ansi);
                return;
            }
            case OPERATOR_GREATER_THAN: {
                colorScheme.operatorGreaterThan(ansi);
                return;
            }
            case OPERATOR_LESS_THAN_OR_EQUAL: {
                colorScheme.operatorLessThanOrEqual(ansi);
                return;
            }
            case OPERATOR_GREATER_THAN_OR_EQUAL: {
                colorScheme.operatorGreaterThanOrEqual(ansi);
                return;
            }
            case OPERATOR_AND: {
                colorScheme.operatorAnd(ansi);
                return;
            }
            case OPERATOR_OR: {
                colorScheme.operatorOr(ansi);
                return;
            }
            case URL_CONSTANT: {
                colorScheme.urlConstant(ansi);
                return;
            }
            case INVISIBLE_TOKEN: {
                throw new AssertionError("Invisible token categories will always be more specific.");
            }
            case WHITESPACE: {
                colorScheme.whitespace(ansi);
                return;
            }
            case NEWLINE: {
                colorScheme.newline(ansi);
                return;
            }
            case END_OF_FILE: {
                colorScheme.endOfFile(ansi);
                return;
            }
            default: {
                throw new AssertionError(tokenCategory);
            }
        }
    }
}
