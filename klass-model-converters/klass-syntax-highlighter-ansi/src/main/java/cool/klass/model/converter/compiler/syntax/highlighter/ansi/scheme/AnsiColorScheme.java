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

package cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme;

import org.fusesource.jansi.Ansi;

public interface AnsiColorScheme {
    void background(Ansi ansi);

    void blockComment(Ansi ansi);

    default void lineComment(Ansi ansi) {
        this.blockComment(ansi);
    }

    void keyword(Ansi ansi);

    default void packageKeyword(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordEnumeration(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordInterface(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordUser(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordClass(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordProjection(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordService(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordAbstract(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordExtends(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordImplements(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordInheritanceType(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordAssociation(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordRelationship(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordOrderBy(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordOrderByDirection(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordOn(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordMultiplicity(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordMultiplicityChoice(Ansi ansi) {
        this.keyword(ansi);
    }

    default void keywordServiceCriteria(Ansi ansi) {
        this.keyword(ansi);
    }

    default void primitiveType(Ansi ansi) {
        this.keyword(ansi);
    }

    default void verb(Ansi ansi) {
        this.keyword(ansi);
    }

    default void verbGet(Ansi ansi) {
        this.verb(ansi);
    }

    default void verbPost(Ansi ansi) {
        this.verb(ansi);
    }

    default void verbPut(Ansi ansi) {
        this.verb(ansi);
    }

    default void verbPatch(Ansi ansi) {
        this.verb(ansi);
    }

    default void verbDelete(Ansi ansi) {
        this.verb(ansi);
    }

    default void modifier(Ansi ansi) {
        this.keyword(ansi);
    }

    default void classifierModifier(Ansi ansi) {
        this.modifier(ansi);
    }

    default void dataTypePropertyModifier(Ansi ansi) {
        this.modifier(ansi);
    }

    default void associationEndModifier(Ansi ansi) {
        this.modifier(ansi);
    }

    default void parameterizedPropertyModifier(Ansi ansi) {
        this.modifier(ansi);
    }

    default void parameterModifier(Ansi ansi) {
        this.modifier(ansi);
    }

    default void validationModifier(Ansi ansi) {
        this.modifier(ansi);
    }

    default void serviceCategoryModifier(Ansi ansi) {
        this.modifier(ansi);
    }

    void identifier(Ansi ansi);

    default void packageName(Ansi ansi) {
        this.identifier(ansi);
    }

    default void topLevelElementName(Ansi ansi) {
        this.identifier(ansi);
    }

    default void enumerationName(Ansi ansi) {
        this.topLevelElementName(ansi);
    }

    default void classifierName(Ansi ansi) {
        this.topLevelElementName(ansi);
    }

    default void className(Ansi ansi) {
        this.classifierName(ansi);
    }

    default void interfaceName(Ansi ansi) {
        this.classifierName(ansi);
    }

    default void associationName(Ansi ansi) {
        this.topLevelElementName(ansi);
    }

    default void projectionName(Ansi ansi) {
        this.topLevelElementName(ansi);
    }

    default void serviceName(Ansi ansi) {
        this.topLevelElementName(ansi);
    }

    default void enumerationLiteralName(Ansi ansi) {
        this.identifier(ansi);
    }

    default void parameterName(Ansi ansi) {
        this.identifier(ansi);
    }

    default void propertyName(Ansi ansi) {
        this.identifier(ansi);
    }

    default void dataTypePropertyName(Ansi ansi) {
        this.propertyName(ansi);
    }

    default void primitivePropertyName(Ansi ansi) {
        this.dataTypePropertyName(ansi);
    }

    default void enumerationPropertyName(Ansi ansi) {
        this.dataTypePropertyName(ansi);
    }

    default void referencePropertyName(Ansi ansi) {
        this.propertyName(ansi);
    }

    default void parameterizedPropertyName(Ansi ansi) {
        this.referencePropertyName(ansi);
    }

    default void associationEndName(Ansi ansi) {
        this.referencePropertyName(ansi);
    }

    default void enumerationReference(Ansi ansi) {
        this.enumerationName(ansi);
    }

    default void classifierReference(Ansi ansi) {
        this.classifierName(ansi);
    }

    default void interfaceReference(Ansi ansi) {
        this.interfaceName(ansi);
    }

    default void classReference(Ansi ansi) {
        this.className(ansi);
    }

    default void projectionReference(Ansi ansi) {
        this.projectionName(ansi);
    }

    default void dataTypePropertyReference(Ansi ansi) {
        this.propertyName(ansi);
    }

    default void associationEndReference(Ansi ansi) {
        this.associationEndName(ansi);
    }

    default void parameterizedPropertyReference(Ansi ansi) {
        this.parameterizedPropertyName(ansi);
    }

    default void propertyReference(Ansi ansi) {
        this.propertyName(ansi);
    }

    default void parameterReference(Ansi ansi) {
        this.parameterName(ansi);
    }

    void literal(Ansi ansi);

    default void stringLiteral(Ansi ansi) {
        this.literal(ansi);
    }

    default void integerLiteral(Ansi ansi) {
        this.literal(ansi);
    }

    default void booleanLiteral(Ansi ansi) {
        this.literal(ansi);
    }

    default void characterLiteral(Ansi ansi) {
        this.literal(ansi);
    }

    default void floatingPointLiteral(Ansi ansi) {
        this.literal(ansi);
    }

    default void literalThis(Ansi ansi) {
        this.literal(ansi);
    }

    default void literalNative(Ansi ansi) {
        this.literal(ansi);
    }

    void punctuation(Ansi ansi);

    default void comma(Ansi ansi) {
        this.punctuation(ansi);
    }

    default void dot(Ansi ansi) {
        this.punctuation(ansi);
    }

    default void dotDot(Ansi ansi) {
        this.dot(ansi);
    }

    default void semi(Ansi ansi) {
        this.punctuation(ansi);
    }

    default void colon(Ansi ansi) {
        this.punctuation(ansi);
    }

    default void slash(Ansi ansi) {
        this.punctuation(ansi);
    }

    default void question(Ansi ansi) {
        this.punctuation(ansi);
    }

    default void ampersand(Ansi ansi) {
        this.punctuation(ansi);
    }

    default void pairedPunctuation(Ansi ansi) {
        this.punctuation(ansi);
    }

    default void parentheses(Ansi ansi) {
        this.pairedPunctuation(ansi);
    }

    default void curlyBraces(Ansi ansi) {
        this.pairedPunctuation(ansi);
    }

    default void squareBrackets(Ansi ansi) {
        this.pairedPunctuation(ansi);
    }

    default void operator(Ansi ansi) {
        this.punctuation(ansi);
    }

    default void wordOperator(Ansi ansi) {
        this.operator(ansi);
    }

    default void operatorEquals(Ansi ansi) {
        this.operator(ansi);
    }

    default void operatorNotEquals(Ansi ansi) {
        this.operator(ansi);
    }

    default void operatorLessThan(Ansi ansi) {
        this.operator(ansi);
    }

    default void operatorGreaterThan(Ansi ansi) {
        this.operator(ansi);
    }

    default void operatorLessThanOrEqual(Ansi ansi) {
        this.operator(ansi);
    }

    default void operatorGreaterThanOrEqual(Ansi ansi) {
        this.operator(ansi);
    }

    default void operatorAnd(Ansi ansi) {
        this.operator(ansi);
    }

    default void operatorOr(Ansi ansi) {
        this.operator(ansi);
    }

    default void operatorIn(Ansi ansi) {
        this.operator(ansi);
    }

    default void operatorString(Ansi ansi) {
        this.operator(ansi);
    }

    default void urlConstant(Ansi ansi) {
        this.identifier(ansi);
    }

    default void invisibleToken(Ansi ansi) {
        // Invisible tokens are not colored
    }

    default void whitespace(Ansi ansi) {
        this.invisibleToken(ansi);
    }

    default void newline(Ansi ansi) {
        this.invisibleToken(ansi);
    }

    default void endOfFile(Ansi ansi) {
        this.invisibleToken(ansi);
    }
}
