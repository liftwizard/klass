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

import java.awt.Color;

import org.fusesource.jansi.Ansi;

public sealed interface AnsiInstruction {
    void apply(Ansi ansi);

    // Base reset instruction
    record Reset() implements AnsiInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.a(Ansi.Attribute.RESET);
        }
    }

    // Foreground color instructions
    sealed interface ForegroundInstruction extends AnsiInstruction {}

    record ResetForeground() implements ForegroundInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.fg(Ansi.Color.DEFAULT);
        }
    }

    record NamedForegroundColor(Ansi.Color color) implements ForegroundInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.fg(color);
        }
    }

    record RgbForegroundColor(int r, int g, int b) implements ForegroundInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.fgRgb(r, g, b);
        }
    }

    record IndexedForegroundColor(int index) implements ForegroundInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.fg(index);
        }
    }

    // Background color instructions
    sealed interface BackgroundInstruction extends AnsiInstruction {}

    record ResetBackground() implements BackgroundInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.bg(Ansi.Color.DEFAULT);
        }
    }

    record NamedBackgroundColor(Ansi.Color color) implements BackgroundInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.bg(color);
        }
    }

    record RgbBackgroundColor(int r, int g, int b) implements BackgroundInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.bgRgb(r, g, b);
        }
    }

    record IndexedBackgroundColor(int index) implements BackgroundInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.bg(index);
        }
    }

    // Font style instructions
    sealed interface FontStyleInstruction extends AnsiInstruction {}

    record BoldOn() implements FontStyleInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.a(Ansi.Attribute.INTENSITY_BOLD);
        }
    }

    record BoldOff() implements FontStyleInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.a(Ansi.Attribute.INTENSITY_BOLD_OFF);
        }
    }

    record ItalicOn() implements FontStyleInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.a(Ansi.Attribute.ITALIC);
        }
    }

    record ItalicOff() implements FontStyleInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.a(Ansi.Attribute.ITALIC_OFF);
        }
    }

    record UnderlineOn() implements FontStyleInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.a(Ansi.Attribute.UNDERLINE);
        }
    }

    record UnderlineOff() implements FontStyleInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.a(Ansi.Attribute.UNDERLINE_OFF);
        }
    }

    record BlinkOn() implements FontStyleInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.a(Ansi.Attribute.BLINK_SLOW);
        }
    }

    record BlinkOff() implements FontStyleInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.a(Ansi.Attribute.BLINK_OFF);
        }
    }

    record ReverseOn() implements FontStyleInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.a(Ansi.Attribute.NEGATIVE_ON);
        }
    }

    record ReverseOff() implements FontStyleInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.a(Ansi.Attribute.NEGATIVE_OFF);
        }
    }

    record StrikethroughOn() implements FontStyleInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.a(Ansi.Attribute.STRIKETHROUGH_ON);
        }
    }

    record StrikethroughOff() implements FontStyleInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.a(Ansi.Attribute.STRIKETHROUGH_OFF);
        }
    }

    record FaintOn() implements FontStyleInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.a(Ansi.Attribute.INTENSITY_FAINT);
        }
    }

    record FaintOff() implements FontStyleInstruction {
        @Override
        public void apply(Ansi ansi) {
            ansi.a(Ansi.Attribute.INTENSITY_BOLD_OFF);
        }
    }

    // Factory methods to create instructions from different types of colors
    static ForegroundInstruction createForegroundColor(Object colorValue) {
        if (colorValue == null) {
            return RESET_FOREGROUND;
        }

        if (colorValue instanceof String stringColor) {
            if (stringColor.startsWith("#")) {
                // Handle hex color
                Color decodedColor = Color.decode(stringColor);
                return new RgbForegroundColor(decodedColor.getRed(), decodedColor.getGreen(), decodedColor.getBlue());
            } else {
                // Handle named color
                try {
                    Ansi.Color namedColor = Ansi.Color.valueOf(stringColor);
                    return new NamedForegroundColor(namedColor);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid color name: '" + stringColor + "'");
                }
            }
        } else if (colorValue instanceof Number numberColor) {
            // Handle indexed color
            return new IndexedForegroundColor(numberColor.intValue());
        } else {
            throw new IllegalArgumentException("Unsupported color type: " + colorValue.getClass().getName());
        }
    }

    static BackgroundInstruction createBackgroundColor(Object colorValue) {
        if (colorValue == null) {
            return RESET_BACKGROUND;
        }

        if (colorValue instanceof String stringColor) {
            if (stringColor.startsWith("#")) {
                // Handle hex color
                Color decodedColor = Color.decode(stringColor);
                return new RgbBackgroundColor(decodedColor.getRed(), decodedColor.getGreen(), decodedColor.getBlue());
            } else {
                // Handle named color
                try {
                    Ansi.Color namedColor = Ansi.Color.valueOf(stringColor);
                    return new NamedBackgroundColor(namedColor);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid color name: '" + stringColor + "'");
                }
            }
        } else if (colorValue instanceof Number numberColor) {
            // Handle indexed color
            return new IndexedBackgroundColor(numberColor.intValue());
        } else {
            throw new IllegalArgumentException("Unsupported color type: " + colorValue.getClass().getName());
        }
    }

    // Static instances for common instructions
    AnsiInstruction RESET = new Reset();
    ForegroundInstruction RESET_FOREGROUND = new ResetForeground();
    BackgroundInstruction RESET_BACKGROUND = new ResetBackground();

    FontStyleInstruction BOLD_ON = new BoldOn();
    FontStyleInstruction BOLD_OFF = new BoldOff();

    FontStyleInstruction ITALIC_ON = new ItalicOn();
    FontStyleInstruction ITALIC_OFF = new ItalicOff();

    FontStyleInstruction UNDERLINE_ON = new UnderlineOn();
    FontStyleInstruction UNDERLINE_OFF = new UnderlineOff();

    FontStyleInstruction BLINK_ON = new BlinkOn();
    FontStyleInstruction BLINK_OFF = new BlinkOff();

    FontStyleInstruction REVERSE_ON = new ReverseOn();
    FontStyleInstruction REVERSE_OFF = new ReverseOff();

    FontStyleInstruction STRIKETHROUGH_ON = new StrikethroughOn();
    FontStyleInstruction STRIKETHROUGH_OFF = new StrikethroughOff();

    FontStyleInstruction FAINT_ON = new FaintOn();
    FontStyleInstruction FAINT_OFF = new FaintOff();
}
