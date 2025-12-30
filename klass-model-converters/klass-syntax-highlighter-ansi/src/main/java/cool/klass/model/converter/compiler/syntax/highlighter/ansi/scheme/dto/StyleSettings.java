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

import java.awt.Color;
import java.util.Arrays;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record StyleSettings(Object foreground, Object background) {
	private static final Logger LOGGER = LoggerFactory.getLogger(StyleSettings.class);
	private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?");

	@ValidationMethod(message = "Invalid foreground color value")
	@JsonIgnore
	public boolean isValidForegroundColor() {
		return this.foreground == null || this.validateColorValue(this.foreground, "foreground");
	}

	@ValidationMethod(message = "Invalid background color value")
	@JsonIgnore
	public boolean isValidBackgroundColor() {
		return this.background == null || this.validateColorValue(this.background, "background");
	}

	private boolean validateColorValue(Object colorValue, String propertyName) {
		if (colorValue instanceof String stringValue) {
			return this.validateStringColor(stringValue, propertyName);
		}

		if (colorValue instanceof Number numberValue) {
			int value = numberValue.intValue();
			if (value < 0 || value > 255) {
				LOGGER.warn("Invalid {} color code: {}. Must be between 0 and 255.", propertyName, value);
				return false;
			}
			return true;
		}

		String colorType = colorValue == null ? "null" : colorValue.getClass().getName();
		LOGGER.warn("Unsupported {} color value type: {}", propertyName, colorType);
		return false;
	}

	private boolean validateStringColor(String colorValue, String propertyName) {
		if (colorValue.startsWith("#")) {
			if (!HEX_COLOR_PATTERN.matcher(colorValue).matches()) {
				LOGGER.warn(
					"Invalid {} hex color format: '{}'. Must be in format #RRGGBB or #RRGGBBAA.",
					propertyName,
					colorValue
				);
				return false;
			}

			try {
				Color.decode(colorValue);
				return true;
			} catch (NumberFormatException e) {
				LOGGER.warn("Invalid {} hex color value: '{}'. Error: {}", propertyName, colorValue, e.getMessage());
				return false;
			}
		}

		try {
			Ansi.Color.valueOf(colorValue);
			return true;
		} catch (IllegalArgumentException e) {
			LOGGER.warn(
				"Invalid {} color name: '{}'. Valid names are: {}",
				propertyName,
				colorValue,
				Arrays.toString(Ansi.Color.values())
			);
			return false;
		}
	}
}
