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

import java.awt.Color;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.dto.StyleSettings;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.fusesource.jansi.Ansi;

public record StyleState(
    @Nullable Object foreground,
    @Nullable Object background,
    boolean bold,
    boolean italic,
    boolean underline,
    boolean blink,
    boolean reverse,
    boolean strikethrough,
    boolean faint
) {
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?");

    public static final StyleState EMPTY = new StyleState(null, null, false, false, false, false, false, false, false);

    public static StyleState fromStyleSettings(StyleSettings styleSettings) {
        return new StyleState(
            styleSettings.foreground(),
            styleSettings.background(),
            styleSettings.bold() != null && styleSettings.bold(),
            styleSettings.italic() != null && styleSettings.italic(),
            styleSettings.underline() != null && styleSettings.underline(),
            styleSettings.blink() != null && styleSettings.blink(),
            styleSettings.reverse() != null && styleSettings.reverse(),
            styleSettings.strikethrough() != null && styleSettings.strikethrough(),
            styleSettings.faint() != null && styleSettings.faint()
        );
    }

    public boolean applyForegroundChanges(StyleState target, Ansi ansi) {
        if (Objects.equals(this.foreground, target.foreground)) {
            return false;
        }

        if (target.foreground == null) {
            ansi.a(Ansi.Attribute.RESET);
            return true;
        }

        applyColor(ansi, target.foreground, true);
        return true;
    }

    public boolean applyBackgroundChanges(StyleState target, Ansi ansi) {
        if (Objects.equals(this.background, target.background)) {
            return false;
        }

        if (target.background == null) {
            ansi.a(Ansi.Attribute.RESET);
            return true;
        }

        applyColor(ansi, target.background, false);
        return true;
    }

    public boolean applyDecorationChanges(StyleState target, Ansi ansi) {
        boolean changed = false;

        if (this.bold != target.bold) {
            ansi.a(target.bold ? Ansi.Attribute.INTENSITY_BOLD : Ansi.Attribute.INTENSITY_BOLD_OFF);
            changed = true;
        }

        if (this.italic != target.italic) {
            ansi.a(target.italic ? Ansi.Attribute.ITALIC : Ansi.Attribute.ITALIC_OFF);
            changed = true;
        }

        if (this.underline != target.underline) {
            ansi.a(target.underline ? Ansi.Attribute.UNDERLINE : Ansi.Attribute.UNDERLINE_OFF);
            changed = true;
        }

        if (this.blink != target.blink) {
            ansi.a(target.blink ? Ansi.Attribute.BLINK_SLOW : Ansi.Attribute.BLINK_OFF);
            changed = true;
        }

        if (this.reverse != target.reverse) {
            ansi.a(target.reverse ? Ansi.Attribute.NEGATIVE_ON : Ansi.Attribute.NEGATIVE_OFF);
            changed = true;
        }

        if (this.strikethrough != target.strikethrough) {
            ansi.a(target.strikethrough ? Ansi.Attribute.STRIKETHROUGH_ON : Ansi.Attribute.STRIKETHROUGH_OFF);
            changed = true;
        }

        if (this.faint != target.faint) {
            ansi.a(target.faint ? Ansi.Attribute.INTENSITY_FAINT : Ansi.Attribute.INTENSITY_BOLD_OFF);
            changed = true;
        }

        return changed;
    }

    private static void applyColor(Ansi ansi, Object colorValue, boolean isForeground) {
        if (colorValue instanceof String colorString) {
            if (colorString.startsWith("#")) {
                applyHexColor(ansi, colorString, isForeground);
            } else {
                applyNamedColor(ansi, colorString, isForeground);
            }
        } else if (colorValue instanceof Number numberValue) {
            int colorCode = numberValue.intValue();
            if (colorCode < 0 || colorCode > 255) {
                throw new AssertionError("Invalid color code: " + colorCode + ". Must be between 0 and 255.");
            }

            if (isForeground) {
                ansi.fg(colorCode);
            } else {
                ansi.bg(colorCode);
            }
        } else {
            throw new AssertionError(
                "Unsupported color value type: " + (colorValue == null ? "null" : colorValue.getClass().getName())
            );
        }
    }

    private static void applyHexColor(Ansi ansi, String hexColor, boolean isForeground) {
        if (!HEX_COLOR_PATTERN.matcher(hexColor).matches()) {
            throw new AssertionError(
                "Invalid hex color format: '" + hexColor + "'. Must be in format #RRGGBB or #RRGGBBAA."
            );
        }

        try {
            Color decodedColor = Color.decode(hexColor);
            int r = decodedColor.getRed();
            int g = decodedColor.getGreen();
            int b = decodedColor.getBlue();

            if (isForeground) {
                ansi.fgRgb(r, g, b);
            } else {
                ansi.bgRgb(r, g, b);
            }
        } catch (NumberFormatException e) {
            throw new AssertionError("Invalid hex color value: '" + hexColor + "'. Error: " + e.getMessage());
        }
    }

    private static void applyNamedColor(Ansi ansi, String colorName, boolean isForeground) {
        try {
            Ansi.Color namedColor = Ansi.Color.valueOf(colorName);
            if (isForeground) {
                ansi.fg(namedColor);
            } else {
                ansi.bg(namedColor);
            }
        } catch (IllegalArgumentException e) {
            String detailMessage =
                "Invalid color name: '" +
                colorName +
                "'. Must be one of: " +
                ArrayAdapter.adapt(Ansi.Color.values()).makeString();
            throw new AssertionError(detailMessage);
        }
    }
}
