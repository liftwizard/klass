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

package cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.util.AnsiColorUtils;
import io.dropwizard.validation.ValidationMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record StyleSettings(
    Object foreground,
    Object background,
    Boolean bold,
    Boolean italic,
    Boolean underline,
    Boolean blink,
    Boolean reverse,
    Boolean strikethrough,
    Boolean faint
) {
    private static final Logger LOGGER = LoggerFactory.getLogger(StyleSettings.class);

    @ValidationMethod(message = "Invalid foreground color value")
    @JsonIgnore
    public boolean isValidForegroundColor() {
        boolean isValid = AnsiColorUtils.isValidColorValue(this.foreground);
        if (!isValid && this.foreground != null) {
            LOGGER.warn("Invalid foreground color value: {}", this.foreground);
        }
        return isValid;
    }

    @ValidationMethod(message = "Invalid background color value")
    @JsonIgnore
    public boolean isValidBackgroundColor() {
        boolean isValid = AnsiColorUtils.isValidColorValue(this.background);
        if (!isValid && this.background != null) {
            LOGGER.warn("Invalid background color value: {}", this.background);
        }
        return isValid;
    }
}
