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
import org.fusesource.jansi.Ansi.Attribute;

public final class AnsiStreamWriter {

    private AnsiStreamWriter() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static Ansi write(ImmutableList<StyledToken> tokens, Object defaultForeground, Object defaultBackground) {
        Ansi ansi = Ansi.ansi();

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

        AnsiColorStyle currentStyle = themeDefaults;

        AnsiStreamWriter.applyInitialThemeStyles(ansi, defaultForeground, defaultBackground);

        for (StyledToken token : tokens) {
            AnsiColorStyle mergedStyle = AnsiStreamWriter.mergeWithDefaults(token.style(), themeDefaults);

            StyleTransition transition = StyleTransition.fromStyles(currentStyle, mergedStyle);

            AnsiStreamWriter.applyTransition(ansi, transition);

            ansi.a(token.text());

            currentStyle = mergedStyle;
        }

        // Don't add automatic reset - let the caller handle resets when appropriate
        // ansi.a(Attribute.RESET);

        return ansi;
    }

    private static void applyInitialThemeStyles(Ansi ansi, Object defaultForeground, Object defaultBackground) {
        ansi.a(Attribute.RESET);

        if (defaultForeground != null) {
            AnsiInstruction.createForegroundColor(defaultForeground).apply(ansi);
        }

        if (defaultBackground != null) {
            AnsiInstruction.createBackgroundColor(defaultBackground).apply(ansi);
        }
    }

    private static AnsiColorStyle mergeWithDefaults(AnsiColorStyle tokenStyle, AnsiColorStyle themeDefaults) {
        return new AnsiColorStyle(
            tokenStyle.foreground() == null ? themeDefaults.foreground() : tokenStyle.foreground(),
            tokenStyle.background() == null ? themeDefaults.background() : tokenStyle.background(),
            tokenStyle.bold(),
            tokenStyle.italic(),
            tokenStyle.underline(),
            tokenStyle.blink(),
            tokenStyle.reverse(),
            tokenStyle.strikethrough(),
            tokenStyle.faint()
        );
    }

    private static void applyTransition(Ansi ansi, StyleTransition transition) {
        for (AnsiInstruction instruction : transition.instructions()) {
            instruction.apply(ansi);
        }
    }

    public record StyledToken(AnsiColorStyle style, String text) {}
}
