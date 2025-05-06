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

package cool.klass.model.converter.compiler.syntax.highlighter.ansi.functional;

import org.eclipse.collections.api.list.ImmutableList;
import org.fusesource.jansi.Ansi;

/**
 * A functional writer for ANSI styled text. Takes styled tokens and creates
 * efficient ANSI color instructions for display.
 */
public final class AnsiStreamWriter {

    private AnsiStreamWriter() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Record representing a token with its style and text content.
     */
    public record StyledToken(AnsiColorStyle style, String text) {}

    /**
     * Writes a sequence of styled tokens to an ANSI stream with optimal color transitions.
     */
    public static Ansi write(ImmutableList<StyledToken> tokens, Object defaultForeground, Object defaultBackground) {
        Ansi ansi = Ansi.ansi();

        // Create theme defaults (only for foreground and background colors)
        AnsiColorStyle themeDefaults = new AnsiColorStyle(
            defaultForeground,
            defaultBackground,
            false,
            false,
            false,
            false,
            false,
            false,
            false
        );

        // Initialize with theme defaults
        AnsiColorStyle currentStyle = themeDefaults;

        // Apply initial theme styles
        applyInitialThemeStyles(ansi, defaultForeground, defaultBackground);

        for (StyledToken token : tokens) {
            // Merge token style with theme defaults for foreground/background only
            AnsiColorStyle mergedStyle = mergeWithDefaults(token.style(), themeDefaults);

            // Calculate the optimal transition from current style to merged style
            StyleTransition transition = StyleTransition.fromStyles(currentStyle, mergedStyle);

            // Apply the transition instructions
            applyTransition(ansi, transition);

            // Write the text content
            ansi.a(token.text());

            // Update current style for the next token
            currentStyle = mergedStyle;
        }

        // Reset at the end
        ansi.a(Ansi.Attribute.RESET);

        return ansi;
    }

    /**
     * Applies the initial theme styles to the ANSI object.
     */
    private static void applyInitialThemeStyles(Ansi ansi, Object defaultForeground, Object defaultBackground) {
        // Reset first to clear any existing styles
        ansi.a(Ansi.Attribute.RESET);

        if (defaultForeground != null) {
            AnsiInstruction.createForegroundColor(defaultForeground).apply(ansi);
        }

        if (defaultBackground != null) {
            AnsiInstruction.createBackgroundColor(defaultBackground).apply(ansi);
        }
    }

    /**
     * Merges a token style with theme defaults, using defaults for foreground/background only.
     */
    private static AnsiColorStyle mergeWithDefaults(AnsiColorStyle tokenStyle, AnsiColorStyle themeDefaults) {
        return new AnsiColorStyle(
            tokenStyle.foreground() != null ? tokenStyle.foreground() : themeDefaults.foreground(),
            tokenStyle.background() != null ? tokenStyle.background() : themeDefaults.background(),
            tokenStyle.bold(),
            tokenStyle.italic(),
            tokenStyle.underline(),
            tokenStyle.blink(),
            tokenStyle.reverse(),
            tokenStyle.strikethrough(),
            tokenStyle.faint()
        );
    }

    /**
     * Applies a style transition to the ANSI stream.
     */
    private static void applyTransition(Ansi ansi, StyleTransition transition) {
        for (AnsiInstruction instruction : transition.instructions()) {
            instruction.apply(ansi);
        }
    }
}
