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

import java.util.Objects;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;

public record StyleTransition(ImmutableList<AnsiInstruction> instructions) {
    public static StyleTransition fromStyles(AnsiColorStyle from, AnsiColorStyle to) {
        if (from.equals(to)) {
            return new StyleTransition(Lists.immutable.empty());
        }

        MutableList<AnsiInstruction> directInstructions = Lists.mutable.empty();
        calculateDirectTransformInstructions(directInstructions, from, to);

        MutableList<AnsiInstruction> resetInstructions = Lists.mutable.empty();
        resetInstructions.add(AnsiInstruction.RESET);
        addAllActiveStyles(resetInstructions, to);

        if (resetInstructions.size() <= directInstructions.size()) {
            return new StyleTransition(resetInstructions.toImmutable());
        }

        return new StyleTransition(directInstructions.toImmutable());
    }

    private static void calculateDirectTransformInstructions(
        MutableList<AnsiInstruction> instructions,
        AnsiColorStyle from,
        AnsiColorStyle to
    ) {
        if (!Objects.equals(from.foreground(), to.foreground())) {
            if (to.foreground() == null) {
                instructions.add(AnsiInstruction.RESET_FOREGROUND);
            } else {
                instructions.add(AnsiInstruction.createForegroundColor(to.foreground()));
            }
        }

        if (!Objects.equals(from.background(), to.background())) {
            if (to.background() == null) {
                instructions.add(AnsiInstruction.RESET_BACKGROUND);
            } else {
                instructions.add(AnsiInstruction.createBackgroundColor(to.background()));
            }
        }

        if (from.bold() != to.bold()) {
            instructions.add(to.bold() ? AnsiInstruction.BOLD_ON : AnsiInstruction.BOLD_OFF);
        }

        if (from.italic() != to.italic()) {
            instructions.add(to.italic() ? AnsiInstruction.ITALIC_ON : AnsiInstruction.ITALIC_OFF);
        }

        if (from.underline() != to.underline()) {
            instructions.add(to.underline() ? AnsiInstruction.UNDERLINE_ON : AnsiInstruction.UNDERLINE_OFF);
        }

        if (from.blink() != to.blink()) {
            instructions.add(to.blink() ? AnsiInstruction.BLINK_ON : AnsiInstruction.BLINK_OFF);
        }

        if (from.reverse() != to.reverse()) {
            instructions.add(to.reverse() ? AnsiInstruction.REVERSE_ON : AnsiInstruction.REVERSE_OFF);
        }

        if (from.strikethrough() != to.strikethrough()) {
            instructions.add(to.strikethrough() ? AnsiInstruction.STRIKETHROUGH_ON : AnsiInstruction.STRIKETHROUGH_OFF);
        }

        if (from.faint() != to.faint()) {
            instructions.add(to.faint() ? AnsiInstruction.FAINT_ON : AnsiInstruction.FAINT_OFF);
        }
    }

    private static void addAllActiveStyles(MutableList<AnsiInstruction> instructions, AnsiColorStyle style) {
        if (style.foreground() != null) {
            instructions.add(AnsiInstruction.createForegroundColor(style.foreground()));
        }

        if (style.background() != null) {
            instructions.add(AnsiInstruction.createBackgroundColor(style.background()));
        }

        if (style.bold()) {
            instructions.add(AnsiInstruction.BOLD_ON);
        }

        if (style.italic()) {
            instructions.add(AnsiInstruction.ITALIC_ON);
        }

        if (style.underline()) {
            instructions.add(AnsiInstruction.UNDERLINE_ON);
        }

        if (style.blink()) {
            instructions.add(AnsiInstruction.BLINK_ON);
        }

        if (style.reverse()) {
            instructions.add(AnsiInstruction.REVERSE_ON);
        }

        if (style.strikethrough()) {
            instructions.add(AnsiInstruction.STRIKETHROUGH_ON);
        }

        if (style.faint()) {
            instructions.add(AnsiInstruction.FAINT_ON);
        }
    }
}
