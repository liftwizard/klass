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

import java.util.LinkedHashSet;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.dto.ColorSchemeDefinition;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.dto.ColorSchemeRule;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.dto.StyleSettings;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.util.AnsiColorUtils;
import cool.klass.model.converter.compiler.token.categories.TokenCategory;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonAnsiColorScheme implements AnsiColorScheme {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonAnsiColorScheme.class);

    private static final ImmutableMap<String, String> FALLBACK_RULES = getFallbacks();
    private static final Converter<String, String> UPPER_UNDERSCORE_TO_LOWER_CAMEL =
        CaseFormat.UPPER_UNDERSCORE.converterTo(CaseFormat.LOWER_CAMEL);

    private final ColorSchemeDefinition definition;
    private final MapIterable<String, ColorSchemeRule> ruleMap;

    public JsonAnsiColorScheme(ColorSchemeDefinition definition) {
        this.definition = Objects.requireNonNull(definition);
        this.ruleMap = definition.toRuleMap();
    }

    public static ImmutableSet<String> getValidRuleNames() {
        MutableSet<String> validRules = Sets.mutable.empty();

        validRules.addAllIterable(FALLBACK_RULES.keysView());
        validRules.addAllIterable(FALLBACK_RULES.valuesView());

        validRules.add("foreground");
        validRules.add("background");
        validRules.add("errorAnnotation");
        validRules.add("warningAnnotation");
        validRules.add("errorCaret");
        validRules.add("warningCaret");
        validRules.add("causeCaret");
        validRules.add("annotationCode");
        validRules.add("metadataLabel");

        return validRules.toImmutable();
    }

    private static ImmutableMap<String, String> getFallbacks() {
        MutableMap<String, String> fallbacks = Maps.mutable.empty();

        // Invisible token fallbacks
        fallbacks.put("invisibleToken", "foreground");
        fallbacks.put("whitespace", "invisibleToken");
        fallbacks.put("newline", "invisibleToken");
        fallbacks.put("endOfFile", "invisibleToken");

        // Comment fallbacks
        fallbacks.put("comment", "foreground");
        fallbacks.put("lineComment", "comment");
        fallbacks.put("blockComment", "comment");

        // Annotation fallbacks
        fallbacks.put("errorAnnotation", "foreground");
        fallbacks.put("warningAnnotation", "foreground");

        // Compiler annotation color fallbacks
        fallbacks.put("errorCaret", "errorAnnotation");
        fallbacks.put("warningCaret", "warningAnnotation");
        fallbacks.put("causeCaret", "foreground");
        fallbacks.put("annotationCode", "foreground");
        fallbacks.put("metadataLabel", "foreground");

        // Keyword fallbacks
        fallbacks.put("primitiveType", "keyword");

        // Keyword specific fallbacks
        fallbacks.put("keyword", "foreground");
        fallbacks.put("packageKeyword", "keyword");
        fallbacks.put("keywordEnumeration", "keyword");
        fallbacks.put("keywordInterface", "keyword");
        fallbacks.put("keywordUser", "keyword");
        fallbacks.put("keywordClass", "keyword");
        fallbacks.put("keywordProjection", "keyword");
        fallbacks.put("keywordService", "keyword");
        fallbacks.put("keywordAbstract", "keyword");
        fallbacks.put("keywordExtends", "keyword");
        fallbacks.put("keywordImplements", "keyword");
        fallbacks.put("keywordInheritanceType", "keyword");
        fallbacks.put("keywordAssociation", "keyword");
        fallbacks.put("keywordRelationship", "keyword");
        fallbacks.put("keywordOrderBy", "keyword");
        fallbacks.put("keywordOrderByDirection", "keyword");
        fallbacks.put("keywordOn", "keyword");
        fallbacks.put("keywordMultiplicity", "keyword");
        fallbacks.put("keywordMultiplicityChoice", "keyword");
        fallbacks.put("keywordServiceCriteria", "keyword");

        // Verb specific fallbacks
        fallbacks.put("verb", "keyword");
        fallbacks.put("verbGet", "verb");
        fallbacks.put("verbPost", "verb");
        fallbacks.put("verbPut", "verb");
        fallbacks.put("verbPatch", "verb");
        fallbacks.put("verbDelete", "verb");

        // Modifier fallbacks
        fallbacks.put("modifier", "keyword");
        fallbacks.put("classifierModifier", "modifier");
        fallbacks.put("dataTypePropertyModifier", "modifier");
        fallbacks.put("associationEndModifier", "modifier");
        fallbacks.put("parameterizedPropertyModifier", "modifier");
        fallbacks.put("parameterModifier", "modifier");
        fallbacks.put("validationModifier", "modifier");
        fallbacks.put("serviceCategoryModifier", "modifier");

        // Identifier fallbacks
        fallbacks.put("identifier", "foreground");
        fallbacks.put("packageName", "identifier");
        fallbacks.put("topLevelElementName", "identifier");
        fallbacks.put("enumerationLiteralName", "identifier");
        fallbacks.put("parameterName", "identifier");
        fallbacks.put("propertyName", "identifier");
        fallbacks.put("urlConstant", "identifier");

        // TopLevelElementName fallbacks
        fallbacks.put("enumerationName", "topLevelElementName");
        fallbacks.put("classifierName", "topLevelElementName");
        fallbacks.put("associationName", "topLevelElementName");
        fallbacks.put("projectionName", "topLevelElementName");
        fallbacks.put("serviceName", "topLevelElementName");

        // ClassifierName fallbacks
        fallbacks.put("className", "classifierName");
        fallbacks.put("interfaceName", "classifierName");

        // PropertyName fallbacks
        fallbacks.put("dataTypePropertyName", "propertyName");
        fallbacks.put("referencePropertyName", "propertyName");

        // DataTypePropertyName fallbacks
        fallbacks.put("primitivePropertyName", "dataTypePropertyName");
        fallbacks.put("enumerationPropertyName", "dataTypePropertyName");

        // ReferencePropertyName fallbacks
        fallbacks.put("parameterizedPropertyName", "referencePropertyName");
        fallbacks.put("associationEndName", "referencePropertyName");

        // Reference fallbacks
        fallbacks.put("enumerationReference", "enumerationName");
        fallbacks.put("classifierReference", "classifierName");
        fallbacks.put("interfaceReference", "interfaceName");
        fallbacks.put("classReference", "className");
        fallbacks.put("projectionReference", "projectionName");
        fallbacks.put("dataTypePropertyReference", "propertyName");
        fallbacks.put("associationEndReference", "associationEndName");
        fallbacks.put("parameterizedPropertyReference", "parameterizedPropertyName");
        fallbacks.put("propertyReference", "propertyName");
        fallbacks.put("parameterReference", "parameterName");

        // Literal fallbacks
        fallbacks.put("literal", "foreground");
        fallbacks.put("stringLiteral", "literal");
        fallbacks.put("integerLiteral", "literal");
        fallbacks.put("booleanLiteral", "literal");
        fallbacks.put("characterLiteral", "literal");
        fallbacks.put("floatingPointLiteral", "literal");
        fallbacks.put("literalThis", "literal");
        fallbacks.put("literalNative", "literal");

        // Punctuation fallbacks
        fallbacks.put("punctuation", "foreground");
        fallbacks.put("comma", "punctuation");
        fallbacks.put("dot", "punctuation");
        fallbacks.put("semi", "punctuation");
        fallbacks.put("operator", "punctuation");
        fallbacks.put("colon", "punctuation");
        fallbacks.put("slash", "punctuation");
        fallbacks.put("question", "punctuation");
        fallbacks.put("ampersand", "punctuation");
        fallbacks.put("pairedPunctuation", "punctuation");
        fallbacks.put("dotdot", "punctuation");
        fallbacks.put("parentheses", "pairedPunctuation");
        fallbacks.put("curlyBraces", "pairedPunctuation");
        fallbacks.put("squareBrackets", "pairedPunctuation");

        // Operator fallbacks
        fallbacks.put("wordOperator", "operator");
        fallbacks.put("operatorEquals", "operator");
        fallbacks.put("operatorNotEquals", "operator");
        fallbacks.put("operatorLessThan", "operator");
        fallbacks.put("operatorGreaterThan", "operator");
        fallbacks.put("operatorLessThanOrEqual", "operator");
        fallbacks.put("operatorGreaterThanOrEqual", "operator");
        fallbacks.put("operatorAnd", "operator");
        fallbacks.put("operatorOr", "operator");
        fallbacks.put("operatorIn", "operator");
        fallbacks.put("operatorString", "operator");

        // Special case: dotDot fallbacks to dot
        fallbacks.put("dotDot", "dot");

        return fallbacks.toImmutable();
    }

    private void applyRule(String ruleName, Ansi ansi) {
        ColorSchemeRule rule = this.getColorSchemeRule(ruleName);

        StyleSettings style = rule.style();

        if (style.foreground() != null) {
            AnsiColorUtils.applyColor(ansi, style.foreground(), true, false);
        }

        if (style.background() != null) {
            AnsiColorUtils.applyColor(ansi, style.background(), false, false);
        }

        if (style.bold() != null && style.bold()) {
            ansi.a(Attribute.INTENSITY_BOLD);
        }

        if (style.faint() != null && style.faint()) {
            ansi.a(Attribute.INTENSITY_FAINT);
        }

        if (style.italic() != null && style.italic()) {
            ansi.a(Attribute.ITALIC);
        }

        if (style.underline() != null && style.underline()) {
            ansi.a(Attribute.UNDERLINE);
        }

        if (style.blink() != null && style.blink()) {
            ansi.a(Attribute.BLINK_SLOW);
        }

        if (style.reverse() != null && style.reverse()) {
            ansi.a(Attribute.NEGATIVE_ON);
        }

        if (style.strikethrough() != null && style.strikethrough()) {
            ansi.a(Attribute.STRIKETHROUGH_ON);
        }
    }

    @Nonnull
    private ColorSchemeRule getColorSchemeRule(String ruleName) {
        String eachRuleName = ruleName;
        MutableSet<String> visitedRules = SetAdapter.adapt(new LinkedHashSet<>());

        while (true) {
            if (visitedRules.contains(eachRuleName)) {
                throw new IllegalStateException("Circular rule reference detected: " + visitedRules);
            }
            visitedRules.add(eachRuleName);
            ColorSchemeRule result = this.ruleMap.get(eachRuleName);
            if (result != null) {
                return result;
            }
            eachRuleName = FALLBACK_RULES.get(eachRuleName);
            if (eachRuleName == null) {
                String detailMessage = "No rule found for: " + visitedRules + " in " + this.definition.name();
                throw new IllegalArgumentException(detailMessage);
            }
        }
    }

    @Override
    public void foreground(Ansi ansi) {
        this.applyRule("foreground", ansi);
    }

    @Override
    public void background(Ansi ansi) {
        this.applyRule("background", ansi);
    }

    @Override
    public void errorAnnotation(Ansi ansi) {
        this.applyRule("errorAnnotation", ansi);
    }

    @Override
    public void warningAnnotation(Ansi ansi) {
        this.applyRule("warningAnnotation", ansi);
    }

    @Override
    public void errorCaret(Ansi ansi) {
        this.applyRule("errorCaret", ansi);
    }

    @Override
    public void warningCaret(Ansi ansi) {
        this.applyRule("warningCaret", ansi);
    }

    @Override
    public void causeCaret(Ansi ansi) {
        this.applyRule("causeCaret", ansi);
    }

    @Override
    public void annotationCode(Ansi ansi) {
        this.applyRule("annotationCode", ansi);
    }

    @Override
    public void metadataLabel(Ansi ansi) {
        this.applyRule("metadataLabel", ansi);
    }

    @Override
    public void blockComment(Ansi ansi) {
        this.applyRule("blockComment", ansi);
    }

    @Override
    public void lineComment(Ansi ansi) {
        this.applyRule("lineComment", ansi);
    }

    @Override
    public void keyword(Ansi ansi) {
        this.applyRule("keyword", ansi);
    }

    @Override
    public void packageKeyword(Ansi ansi) {
        this.applyRule("packageKeyword", ansi);
    }

    @Override
    public void primitiveType(Ansi ansi) {
        this.applyRule("primitiveType", ansi);
    }

    @Override
    public void verb(Ansi ansi) {
        this.applyRule("verb", ansi);
    }

    @Override
    public void modifier(Ansi ansi) {
        this.applyRule("modifier", ansi);
    }

    @Override
    public void classifierModifier(Ansi ansi) {
        this.applyRule("classifierModifier", ansi);
    }

    @Override
    public void dataTypePropertyModifier(Ansi ansi) {
        this.applyRule("dataTypePropertyModifier", ansi);
    }

    @Override
    public void associationEndModifier(Ansi ansi) {
        this.applyRule("associationEndModifier", ansi);
    }

    @Override
    public void parameterizedPropertyModifier(Ansi ansi) {
        this.applyRule("parameterizedPropertyModifier", ansi);
    }

    @Override
    public void parameterModifier(Ansi ansi) {
        this.applyRule("parameterModifier", ansi);
    }

    @Override
    public void validationModifier(Ansi ansi) {
        this.applyRule("validationModifier", ansi);
    }

    @Override
    public void serviceCategoryModifier(Ansi ansi) {
        this.applyRule("serviceCategoryModifier", ansi);
    }

    @Override
    public void identifier(Ansi ansi) {
        this.applyRule("identifier", ansi);
    }

    @Override
    public void packageName(Ansi ansi) {
        this.applyRule("packageName", ansi);
    }

    @Override
    public void topLevelElementName(Ansi ansi) {
        this.applyRule("topLevelElementName", ansi);
    }

    @Override
    public void enumerationName(Ansi ansi) {
        this.applyRule("enumerationName", ansi);
    }

    @Override
    public void classifierName(Ansi ansi) {
        this.applyRule("classifierName", ansi);
    }

    @Override
    public void className(Ansi ansi) {
        this.applyRule("className", ansi);
    }

    @Override
    public void interfaceName(Ansi ansi) {
        this.applyRule("interfaceName", ansi);
    }

    @Override
    public void associationName(Ansi ansi) {
        this.applyRule("associationName", ansi);
    }

    @Override
    public void projectionName(Ansi ansi) {
        this.applyRule("projectionName", ansi);
    }

    @Override
    public void serviceName(Ansi ansi) {
        this.applyRule("serviceName", ansi);
    }

    @Override
    public void enumerationLiteralName(Ansi ansi) {
        this.applyRule("enumerationLiteralName", ansi);
    }

    @Override
    public void parameterName(Ansi ansi) {
        this.applyRule("parameterName", ansi);
    }

    @Override
    public void propertyName(Ansi ansi) {
        this.applyRule("propertyName", ansi);
    }

    @Override
    public void dataTypePropertyName(Ansi ansi) {
        this.applyRule("dataTypePropertyName", ansi);
    }

    @Override
    public void primitivePropertyName(Ansi ansi) {
        this.applyRule("primitivePropertyName", ansi);
    }

    @Override
    public void enumerationPropertyName(Ansi ansi) {
        this.applyRule("enumerationPropertyName", ansi);
    }

    @Override
    public void referencePropertyName(Ansi ansi) {
        this.applyRule("referencePropertyName", ansi);
    }

    @Override
    public void parameterizedPropertyName(Ansi ansi) {
        this.applyRule("parameterizedPropertyName", ansi);
    }

    @Override
    public void associationEndName(Ansi ansi) {
        this.applyRule("associationEndName", ansi);
    }

    @Override
    public void enumerationReference(Ansi ansi) {
        this.applyRule("enumerationReference", ansi);
    }

    @Override
    public void classifierReference(Ansi ansi) {
        this.applyRule("classifierReference", ansi);
    }

    @Override
    public void interfaceReference(Ansi ansi) {
        this.applyRule("interfaceReference", ansi);
    }

    @Override
    public void classReference(Ansi ansi) {
        this.applyRule("classReference", ansi);
    }

    @Override
    public void projectionReference(Ansi ansi) {
        this.applyRule("projectionReference", ansi);
    }

    @Override
    public void dataTypePropertyReference(Ansi ansi) {
        this.applyRule("dataTypePropertyReference", ansi);
    }

    @Override
    public void associationEndReference(Ansi ansi) {
        this.applyRule("associationEndReference", ansi);
    }

    @Override
    public void parameterizedPropertyReference(Ansi ansi) {
        this.applyRule("parameterizedPropertyReference", ansi);
    }

    @Override
    public void propertyReference(Ansi ansi) {
        this.applyRule("propertyReference", ansi);
    }

    @Override
    public void parameterReference(Ansi ansi) {
        this.applyRule("parameterReference", ansi);
    }

    @Override
    public void literal(Ansi ansi) {
        this.applyRule("literal", ansi);
    }

    @Override
    public void stringLiteral(Ansi ansi) {
        this.applyRule("stringLiteral", ansi);
    }

    @Override
    public void integerLiteral(Ansi ansi) {
        this.applyRule("integerLiteral", ansi);
    }

    @Override
    public void booleanLiteral(Ansi ansi) {
        this.applyRule("booleanLiteral", ansi);
    }

    @Override
    public void characterLiteral(Ansi ansi) {
        this.applyRule("characterLiteral", ansi);
    }

    @Override
    public void floatingPointLiteral(Ansi ansi) {
        this.applyRule("floatingPointLiteral", ansi);
    }

    @Override
    public void literalThis(Ansi ansi) {
        this.applyRule("literalThis", ansi);
    }

    @Override
    public void literalNative(Ansi ansi) {
        this.applyRule("literalNative", ansi);
    }

    @Override
    public void punctuation(Ansi ansi) {
        this.applyRule("punctuation", ansi);
    }

    @Override
    public void comma(Ansi ansi) {
        this.applyRule("comma", ansi);
    }

    @Override
    public void dot(Ansi ansi) {
        this.applyRule("dot", ansi);
    }

    @Override
    public void dotDot(Ansi ansi) {
        this.applyRule("dotDot", ansi);
    }

    @Override
    public void semi(Ansi ansi) {
        this.applyRule("semi", ansi);
    }

    @Override
    public void operator(Ansi ansi) {
        this.applyRule("operator", ansi);
    }

    @Override
    public void urlConstant(Ansi ansi) {
        this.applyRule("urlConstant", ansi);
    }

    @Override
    public void invisibleToken(Ansi ansi) {
        this.applyRule("invisibleToken", ansi);
    }

    @Override
    public void whitespace(Ansi ansi) {
        this.applyRule("whitespace", ansi);
    }

    @Override
    public void newline(Ansi ansi) {
        this.applyRule("newline", ansi);
    }

    @Override
    public void endOfFile(Ansi ansi) {
        this.applyRule("endOfFile", ansi);
    }

    @Override
    @Nonnull
    public StyleSettings getStyleSettings(TokenCategory tokenCategory) {
        if (tokenCategory == null) {
            ColorSchemeRule rule = this.getColorSchemeRule("background");
            return rule.style();
        }

        String ruleName = this.getRuleNameForTokenCategory(tokenCategory);
        ColorSchemeRule rule = this.getColorSchemeRule(ruleName);
        return rule.style();
    }

    private String getRuleNameForTokenCategory(TokenCategory tokenCategory) {
        // Handle exceptions first
        if (tokenCategory == TokenCategory.COMMENT) {
            throw new AssertionError("Comment categories will always be more specific.");
        }
        if (tokenCategory == TokenCategory.INVISIBLE_TOKEN) {
            throw new AssertionError("Invisible token categories will always be more specific.");
        }

        // Special cases for multi-token mappings and exceptions to camelCase naming pattern
        return switch (tokenCategory) {
            // Multi-token mappings
            case PARENTHESES, PARENTHESIS_LEFT, PARENTHESIS_RIGHT -> "parentheses";
            case CURLY_BRACES, CURLY_LEFT, CURLY_RIGHT -> "curlyBraces";
            case SQUARE_BRACKETS, SQUARE_BRACKET_LEFT, SQUARE_BRACKET_RIGHT -> "squareBrackets";
            case INTEGER_LITERAL, ASTERISK_LITERAL -> "integerLiteral";
            // Special cases with non-standard naming
            case SEMICOLON -> "semi";
            case END_OF_FILE -> "endOfFile";
            // Default case: Use automatic conversion
            default -> UPPER_UNDERSCORE_TO_LOWER_CAMEL.convert(tokenCategory.name());
        };
    }
}
