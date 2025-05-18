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

import java.awt.Color;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for applying colors to ANSI output.
 * Consolidates common color application logic used across ANSI syntax highlighting classes.
 */
public final class AnsiColorUtils {

    public static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?");

    private static final Logger LOGGER = LoggerFactory.getLogger(AnsiColorUtils.class);

    private AnsiColorUtils() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Applies a color value to an Ansi object.
     * Supports String (named colors or hex), Number (color codes), or null values.
     *
     * @param ansi         The Ansi object to apply color to
     * @param colorValue   The color value (String, Number, or null)
     * @param isForeground True for foreground color, false for background
     * @param throwOnError True to throw exceptions, false to log warnings
     */
    public static void applyColor(
        @Nonnull Ansi ansi,
        @Nullable Object colorValue,
        boolean isForeground,
        boolean throwOnError
    ) {
        if (colorValue instanceof String) {
            applyColorString(ansi, (String) colorValue, isForeground, throwOnError);
        } else if (colorValue instanceof Number) {
            applyColorNumber(ansi, (Number) colorValue, isForeground);
        } else if (colorValue == null) {
            if (!throwOnError) {
                LOGGER.warn("Null color value provided");
            }
            // Silently ignore null colors when throwOnError is true
        } else {
            String message = "Unsupported color value type: " + colorValue.getClass().getName();
            if (throwOnError) {
                throw new IllegalArgumentException(message);
            } else {
                LOGGER.warn(message);
            }
        }
    }

    /**
     * Applies a color value to an Ansi object, throwing exceptions on errors.
     */
    public static void applyColor(@Nonnull Ansi ansi, @Nullable Object colorValue, boolean isForeground) {
        applyColor(ansi, colorValue, isForeground, true);
    }

    private static void applyColorString(Ansi ansi, String colorValue, boolean isForeground, boolean throwOnError) {
        // Handle hex RGB colors (e.g., "#111111", "#F4A7B9")
        if (colorValue.startsWith("#") && HEX_COLOR_PATTERN.matcher(colorValue).matches()) {
            applyColorRGB(ansi, isForeground, colorValue);
        } else {
            applyColorNamed(ansi, isForeground, colorValue, throwOnError);
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

    private static void applyColorNamed(Ansi ansi, boolean isForeground, String colorStr, boolean throwOnError) {
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
                "'. Must be a valid ANSI color enum, integer, or RGB hex format (#RRGGBB).";

            if (throwOnError) {
                // Simple error message for FunctionalSyntaxHighlighter
                throw new IllegalArgumentException(detailMessage);
            } else {
                // Detailed error message for JsonAnsiColorScheme
                detailMessage += " Valid enum values are: " + ArrayAdapter.adapt(Ansi.Color.values()).makeString();
                throw new IllegalArgumentException(detailMessage);
            }
        }
    }

    private static void applyColorNumber(Ansi ansi, Number colorValue, boolean isForeground) {
        int colorCode = colorValue.intValue();

        if (isForeground) {
            ansi.fg(colorCode);
        } else {
            ansi.bg(colorCode);
        }
    }

    /**
     * Checks if a color value is valid.
     *
     * @param colorValue The color value to check
     * @return true if the color value is valid
     */
    public static boolean isValidColorValue(@Nullable Object colorValue) {
        if (colorValue == null) {
            return true;
        }

        if (colorValue instanceof Number) {
            int colorCode = ((Number) colorValue).intValue();
            return colorCode >= 0 && colorCode <= 255;
        }

        if (colorValue instanceof String) {
            String colorStr = (String) colorValue;

            // Check hex format
            if (colorStr.startsWith("#")) {
                return HEX_COLOR_PATTERN.matcher(colorStr).matches();
            }

            // Check named color
            try {
                Ansi.Color.valueOf(colorStr);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        return false;
    }
}
