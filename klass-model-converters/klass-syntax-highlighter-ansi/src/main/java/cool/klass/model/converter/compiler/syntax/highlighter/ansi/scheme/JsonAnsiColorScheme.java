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

import java.awt.Color;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.dto.ColorSchemeDefinition;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.dto.ColorSchemeRule;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.dto.StyleSettings;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonAnsiColorScheme implements AnsiColorScheme {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonAnsiColorScheme.class);

    private static final ImmutableMap<String, String> FALLBACK_RULES = getFallbacks();
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?");

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

        validRules.add("background");
        validRules.add("foreground");

        return validRules.toImmutable();
    }

    private static ImmutableMap<String, String> getFallbacks() {
        MutableMap<String, String> fallbacks = Maps.mutable.empty();

        // Comment fallbacks
        fallbacks.put("lineComment", "blockComment");

        // Keyword fallbacks
        fallbacks.put("packageKeyword", "keyword");
        fallbacks.put("primitiveType", "keyword");
        fallbacks.put("verb", "keyword");
        fallbacks.put("modifier", "keyword");

        // Modifier fallbacks
        fallbacks.put("classifierModifier", "modifier");
        fallbacks.put("dataTypePropertyModifier", "modifier");
        fallbacks.put("associationEndModifier", "modifier");
        fallbacks.put("parameterizedPropertyModifier", "modifier");
        fallbacks.put("parameterModifier", "modifier");
        fallbacks.put("validationModifier", "modifier");
        fallbacks.put("serviceCategoryModifier", "modifier");

        // Identifier fallbacks
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
        fallbacks.put("stringLiteral", "literal");
        fallbacks.put("integerLiteral", "literal");
        fallbacks.put("booleanLiteral", "literal");
        fallbacks.put("characterLiteral", "literal");
        fallbacks.put("floatingPointLiteral", "literal");
        fallbacks.put("literalThis", "literal");
        fallbacks.put("literalNative", "literal");

        // Punctuation fallbacks
        fallbacks.put("comma", "punctuation");
        fallbacks.put("dot", "punctuation");
        fallbacks.put("semi", "punctuation");
        fallbacks.put("operator", "punctuation");

        // Special case: dotDot fallbacks to dot
        fallbacks.put("dotDot", "dot");

        return fallbacks.toImmutable();
    }

    private void applyRule(String ruleName, Ansi ansi) {
        ColorSchemeRule rule = this.getColorSchemeRule(ruleName);

        StyleSettings style = rule.style();

        if (style.foreground() != null) {
            this.applyColor(ansi, style.foreground(), true);
        }

        if (style.background() != null) {
            this.applyColor(ansi, style.background(), false);
        }
    }

    /**
     * Apply a color to the Ansi instance.
     *
     * @param ansi         The Ansi instance to modify
     * @param colorValue   The color value (can be a string enum name, an RGB hex string, or an integer)
     * @param isForeground True for foreground color, false for background
     */
    private void applyColor(Ansi ansi, Object colorValue, boolean isForeground) {
        if (colorValue instanceof String) {
            applyColorString(ansi, (String) colorValue, isForeground);
        } else if (colorValue instanceof Number) {
            applyColorNumber(ansi, (Number) colorValue, isForeground);
        } else if (colorValue == null) {
            LOGGER.warn("Null color value provided");
        } else {
            LOGGER.warn("Unsupported color value type: {}", colorValue.getClass().getName());
        }
    }

    private static void applyColorString(Ansi ansi, String colorValue, boolean isForeground) {
        // Handle hex RGB colors (e.g., "#111111", "#F4A7B9")
        if (colorValue.startsWith("#") && HEX_COLOR_PATTERN.matcher(colorValue).matches()) {
            applyColorRGB(ansi, isForeground, colorValue);
        } else {
            applyColorNamed(ansi, isForeground, colorValue);
        }
    }

    private static void applyColorRGB(Ansi ansi, boolean isForeground, String colorStr) {
        Color decodedColor = Color.decode(colorStr);
        int r = decodedColor.getRed();
        int g = decodedColor.getGreen();
        int b = decodedColor.getBlue();

        if (isForeground) {
            ansi.fgRgb(r, g, b);
        } else {
            ansi.bgRgb(r, g, b);
        }
    }

    // Handle named colors (e.g., "RED", "BLUE")
    private static void applyColorNamed(Ansi ansi, boolean isForeground, String colorStr) {
        try {
            Ansi.Color namedColor = Ansi.Color.valueOf(colorStr);

            if (isForeground) {
                ansi.fg(namedColor);
            } else {
                ansi.bg(namedColor);
            }
        } catch (IllegalArgumentException e) {
            String detailMessage =
                "Invalid color name: '" +
                colorStr +
                "'. Must be a valid ANSI color enum, integer, or RGB hex format (#RRGGBB). Valid enum values are: " +
                ArrayAdapter.adapt(Ansi.Color.values()).makeString();
            throw new IllegalArgumentException(detailMessage);
        }
    }

    /**
     * Handle numeric color codes (e.g., 256-color terminal codes)
     */
    private static void applyColorNumber(Ansi ansi, Number colorValue, boolean isForeground) {
        int colorCode = colorValue.intValue();

        if (isForeground) {
            ansi.fg(colorCode);
        } else {
            ansi.bg(colorCode);
        }
    }

    @Nonnull
    private ColorSchemeRule getColorSchemeRule(String ruleName) {
        String eachRuleName = ruleName;
        MutableSet<String> visitedRules = Sets.mutable.empty();

        while (true) {
            if (visitedRules.contains(eachRuleName)) {
                throw new IllegalStateException("Circular rule reference detected: " + visitedRules);
            }
            visitedRules.add(eachRuleName);
            if (eachRuleName == null) {
                throw new IllegalArgumentException("No rule found for: " + ruleName + " in " + this.definition.name());
            }
            ColorSchemeRule result = this.ruleMap.get(eachRuleName);
            if (result != null) {
                return result;
            }
            eachRuleName = FALLBACK_RULES.get(eachRuleName);
        }
    }

    @Override
    public void background(Ansi ansi) {
        this.applyRule("background", ansi);
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
}
