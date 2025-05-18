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

package cool.klass.model.converter.compiler.syntax.highlighter.ansi.style;

import javax.annotation.Nonnull;

import org.fusesource.jansi.Ansi;

public final class StyleTransition {

    private StyleTransition() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    @Nonnull
    public static StyleState transition(
        @Nonnull StyleState currentState,
        @Nonnull StyleState targetState,
        @Nonnull Ansi ansi
    ) {
        int diffCount = countDifferences(currentState, targetState);

        if (diffCount >= 3) {
            applyFullReset(targetState, ansi);
            return targetState;
        }

        boolean needReapply = false;

        needReapply = currentState.applyForegroundChanges(targetState, ansi) || needReapply;

        if (needReapply) {
            applyAllAttributes(targetState, ansi);
            return targetState;
        }

        needReapply = currentState.applyBackgroundChanges(targetState, ansi) || needReapply;

        if (needReapply) {
            applyAllAttributes(targetState, ansi);
            return targetState;
        }

        currentState.applyDecorationChanges(targetState, ansi);

        return targetState;
    }

    private static void applyFullReset(@Nonnull StyleState targetState, @Nonnull Ansi ansi) {
        ansi.a(Ansi.Attribute.RESET);
        applyAllAttributes(targetState, ansi);
    }

    private static void applyAllAttributes(@Nonnull StyleState state, @Nonnull Ansi ansi) {
        if (state.foreground() != null) {
            state.applyForegroundChanges(state, ansi);
        }

        if (state.background() != null) {
            state.applyBackgroundChanges(state, ansi);
        }

        if (state.bold()) {
            ansi.a(Ansi.Attribute.INTENSITY_BOLD);
        }

        if (state.faint()) {
            ansi.a(Ansi.Attribute.INTENSITY_FAINT);
        }

        if (state.italic()) {
            ansi.a(Ansi.Attribute.ITALIC);
        }

        if (state.underline()) {
            ansi.a(Ansi.Attribute.UNDERLINE);
        }

        if (state.blink()) {
            ansi.a(Ansi.Attribute.BLINK_SLOW);
        }

        if (state.reverse()) {
            ansi.a(Ansi.Attribute.NEGATIVE_ON);
        }

        if (state.strikethrough()) {
            ansi.a(Ansi.Attribute.STRIKETHROUGH_ON);
        }
    }

    private static int countDifferences(@Nonnull StyleState current, @Nonnull StyleState target) {
        int count = 0;

        if (!areSameColor(current.foreground(), target.foreground())) {
            count++;
        }

        if (!areSameColor(current.background(), target.background())) {
            count++;
        }

        if (current.bold() != target.bold()) {
            count++;
        }
        if (current.italic() != target.italic()) {
            count++;
        }
        if (current.underline() != target.underline()) {
            count++;
        }
        if (current.blink() != target.blink()) {
            count++;
        }
        if (current.reverse() != target.reverse()) {
            count++;
        }
        if (current.strikethrough() != target.strikethrough()) {
            count++;
        }
        if (current.faint() != target.faint()) {
            count++;
        }

        return count;
    }

    private static boolean areSameColor(Object color1, Object color2) {
        if (color1 == null && color2 == null) {
            return true;
        }
        if (color1 == null || color2 == null) {
            return false;
        }
        return color1.equals(color2);
    }
}
