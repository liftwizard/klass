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

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.syntax.highlighter.ansi.functional.AnsiColorStyle;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.dto.StyleSettings;

/**
 * Utility class for converting between style representations.
 * Consolidates style conversion logic used across ANSI syntax highlighting classes.
 */
public final class StyleConversionUtils {

    private StyleConversionUtils() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Converts StyleSettings to AnsiColorStyle with null-safe Boolean handling.
     *
     * @param styleSettings The style settings to convert
     * @return The converted AnsiColorStyle
     */
    @Nonnull
    public static AnsiColorStyle convertToAnsiColorStyle(@Nonnull StyleSettings styleSettings) {
        return new AnsiColorStyle(
            styleSettings.foreground(),
            styleSettings.background(),
            Boolean.TRUE.equals(styleSettings.bold()),
            Boolean.TRUE.equals(styleSettings.italic()),
            Boolean.TRUE.equals(styleSettings.underline()),
            Boolean.TRUE.equals(styleSettings.blink()),
            Boolean.TRUE.equals(styleSettings.reverse()),
            Boolean.TRUE.equals(styleSettings.strikethrough()),
            Boolean.TRUE.equals(styleSettings.faint())
        );
    }
}
