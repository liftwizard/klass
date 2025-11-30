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

package cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.dto.ColorSchemeDefinition;
import io.dropwizard.jersey.validation.Validators;
import io.liftwizard.serialization.jackson.config.ObjectMapperConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads color schemes from JSON files by name.
 */
public final class ColorSchemeProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColorSchemeProvider.class);
    private static final String COLOR_SCHEME_PATH = "klass/color-scheme/";
    private static final Validator VALIDATOR = Validators.newValidator();

    private ColorSchemeProvider() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * Get a color scheme by name, loading from JSONC if available. Looks in the classpath. If the {@code schemeName} is "dark" then the classpath is searched for a file named "klass/color-scheme/dark.jsonc".
     *
     * @param schemeName The name of the color scheme to load, like "dark", "light", or "dark-rgb".
     */
    public static AnsiColorScheme getByName(String schemeName) {
        Objects.requireNonNull(
            schemeName,
            () ->
                "colorScheme is required but was not configured. "
                + "Add <colorScheme>dark</colorScheme> to the plugin configuration in your pom.xml. "
                + "Available schemes: dark, light, dark-rgb, light-rgb, dark-cube"
        );

        String jsonPath = COLOR_SCHEME_PATH + schemeName.toLowerCase(Locale.ROOT) + ".jsonc";
        return getByClasspath(jsonPath);
    }

    /**
     * Checks if a color scheme exists by name without loading it.
     *
     * @param schemeName The name of the color scheme to check, like "dark", "light", or "dark-rgb".
     * @return true if the color scheme file exists, false otherwise
     */
    public static boolean existsByName(String schemeName) {
        if (schemeName == null) {
            return false;
        }

        String jsonPath = COLOR_SCHEME_PATH + schemeName.toLowerCase(Locale.ROOT) + ".jsonc";
        try (InputStream inputStream = ColorSchemeProvider.class.getClassLoader().getResourceAsStream(jsonPath)) {
            return inputStream != null;
        } catch (IOException e) {
            LOGGER.warn("Error checking color scheme existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Attempt to load a color scheme from a JSON file.
     */
    @Nonnull
    private static AnsiColorScheme getByClasspath(String path) {
        try (InputStream inputStream = ColorSchemeProvider.class.getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("No color scheme found for path " + path);
            }

            ObjectMapper objectMapper = ObjectMapperConfig.configure(new ObjectMapper());
            var colorSchemeDefinition = objectMapper.readValue(inputStream, ColorSchemeDefinition.class);

            Set<ConstraintViolation<ColorSchemeDefinition>> violations = VALIDATOR.validate(colorSchemeDefinition);
            if (violations.isEmpty()) {
                return new JsonAnsiColorScheme(colorSchemeDefinition);
            }

            StringBuilder errorMessage = new StringBuilder("Color scheme validation errors:");
            for (ConstraintViolation<ColorSchemeDefinition> violation : violations) {
                errorMessage
                    .append("\n  - ")
                    .append(violation.getPropertyPath())
                    .append(": ")
                    .append(violation.getMessage());
            }
            throw new IllegalArgumentException(errorMessage.toString() + " for path " + path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load color scheme from " + path, e);
        }
    }
}
