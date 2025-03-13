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

import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.dto.ColorSchemeDefinition;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.dto.ColorSchemeRule;
import io.dropwizard.jersey.validation.Validators;
import io.liftwizard.serialization.jackson.config.ObjectMapperConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorSchemeValidationTest
{
    private final Validator validator = Validators.newValidator();

    @Test
    void validScheme()
    {
        String json = """
                {
                  "name": "test",
                  "description": "Valid test color scheme",
                  "rules": [
                    {
                      "name": "background",
                      "style": {
                        "background": "BLACK"
                      }
                    },
                    {
                      "name": "keyword",
                      "style": {
                        "foreground": "MAGENTA"
                      }
                    }
                  ]
                }
                """;

        ColorSchemeDefinition definition = this.parseAndValidate(json);

        List<String> actualViolationMessages = this.validator.validate(definition)
                .stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList();

        assertTrue(actualViolationMessages.isEmpty(), "Should have no violations for valid scheme");
    }

    @Test
    void missingName()
    {
        String json = """
                {
                  "name": "",
                  "description": "Invalid test color scheme",
                  "rules": [
                    {
                      "name": "background",
                      "style": {
                        "background": "BLACK"
                      }
                    }
                  ]
                }
                """;

        ColorSchemeDefinition definition = this.parseAndValidate(json);

        List<String> actualViolationMessages = this.validator.validate(definition)
                .stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList();

        List<String> expectedViolationMessages = List.of("Color scheme name is required");

        assertEquals(
                expectedViolationMessages,
                actualViolationMessages,
                "Should have correct violation message for missing name");
    }

    @Test
    void missingDescription()
    {
        String json = """
                {
                  "name": "test",
                  "description": null,
                  "rules": [
                    {
                      "name": "background",
                      "style": {
                        "background": "BLACK"
                      }
                    }
                  ]
                }
                """;

        ColorSchemeDefinition definition = this.parseAndValidate(json);

        List<String> actualViolationMessages = this.validator.validate(definition)
                .stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList();

        List<String> expectedViolationMessages = List.of("Color scheme description is required");

        assertEquals(
                expectedViolationMessages,
                actualViolationMessages,
                "Should have correct violation message for null description");
    }

    @Test
    void emptyRules()
    {
        String json = """
                {
                  "name": "test",
                  "description": "Test scheme",
                  "rules": []
                }
                """;

        ColorSchemeDefinition definition = this.parseAndValidate(json);

        List<String> actualViolationMessages = this.validator.validate(definition)
                .stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList();

        List<String> expectedViolationMessages = List.of(
                "Color scheme must define a 'background' rule",
                "Color scheme must define at least one rule");

        assertEquals(
                expectedViolationMessages,
                actualViolationMessages,
                "Should have correct violation message for empty rules");
    }

    @Test
    void noBackgroundRule()
    {
        String json = """
                {
                  "name": "test",
                  "description": "Test scheme",
                  "rules": [
                    {
                      "name": "keyword",
                      "style": {
                        "foreground": "MAGENTA"
                      }
                    }
                  ]
                }
                """;

        ColorSchemeDefinition definition = this.parseAndValidate(json);

        List<String> actualViolationMessages = this.validator.validate(definition)
                .stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList();

        List<String> expectedViolationMessages = List.of(
                "Color scheme must define a 'background' rule");

        assertEquals(
                expectedViolationMessages,
                actualViolationMessages,
                "Should have correct violation message for missing background rule");
    }

    @Test
    void invalidColorName()
    {
        String json = """
                {
                  "name": "test",
                  "description": "Test scheme",
                  "rules": [
                    {
                      "name": "background",
                      "style": {
                        "background": "BLACK"
                      }
                    },
                    {
                      "name": "keyword",
                      "style": {
                        "foreground": "INVALID_COLOR"
                      }
                    }
                  ]
                }
                """;

        ColorSchemeDefinition definition = this.parseAndValidate(json);

        List<String> actualViolationMessages = this.validator.validate(definition)
                .stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList();

        List<String> expectedViolationMessages = List.of("Invalid foreground color value");

        assertEquals(
                expectedViolationMessages,
                actualViolationMessages,
                "Should have correct violation message for invalid color name");

        // Also check the detailed error in the logs
        Set<ConstraintViolation<ColorSchemeRule>> ruleViolations = this.validator.validate(definition.rules().getFirst());

        assertTrue(ruleViolations.isEmpty(), "Rule itself should be valid since name is present");

        // Style settings should have the validation error
        List<String> actualStyleViolationMessages = this.validator.validate(definition.rules().get(1).style())
                .stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList();

        List<String> expectedStyleViolationMessages = List.of("Invalid foreground color value");

        assertEquals(
                expectedStyleViolationMessages,
                actualStyleViolationMessages,
                "Should have correct style validation messages");
    }

    @Test
    void invalidHexColor()
    {
        String json = """
                {
                  "name": "test",
                  "description": "Test scheme",
                  "rules": [
                    {
                      "name": "background",
                      "style": {
                        "background": "BLACK"
                      }
                    },
                    {
                      "name": "keyword",
                      "style": {
                        "foreground": "#XYZ"
                      }
                    }
                  ]
                }
                """;

        ColorSchemeDefinition definition = this.parseAndValidate(json);

        List<String> actualViolationMessages = this.validator.validate(definition)
                .stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList();

        List<String> expectedViolationMessages = List.of("Invalid foreground color value");

        assertEquals(
                expectedViolationMessages,
                actualViolationMessages,
                "Should have correct violation message for invalid hex color");
    }

    @Test
    void invalidNumericColor()
    {
        String json = """
                {
                  "name": "test",
                  "description": "Test scheme",
                  "rules": [
                    {
                      "name": "background",
                      "style": {
                        "background": "BLACK"
                      }
                    },
                    {
                      "name": "keyword",
                      "style": {
                        "foreground": 300
                      }
                    }
                  ]
                }
                """;

        ColorSchemeDefinition definition = this.parseAndValidate(json);

        List<String> actualViolationMessages = this.validator.validate(definition)
                .stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList();

        List<String> expectedViolationMessages = List.of("Invalid foreground color value");

        assertEquals(
                expectedViolationMessages,
                actualViolationMessages,
                "Should have correct violation message for out-of-range numeric color");
    }

    @Test
    void invalidRuleName()
    {
        String json = """
                {
                  "name": "test",
                  "description": "Test scheme with invalid rule name",
                  "rules": [
                    {
                      "name": "background",
                      "style": {
                        "background": "BLACK"
                      }
                    },
                    {
                      "name": "nonexistent_syntax_node",
                      "style": {
                        "foreground": "RED"
                      }
                    }
                  ]
                }
                """;

        ColorSchemeDefinition definition = this.parseAndValidate(json);

        List<String> actualViolationMessages = this.validator.validate(definition)
                .stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList();

        List<String> expectedViolationMessages = List.of(
                "Unknown rule name in color scheme");

        assertEquals(
                expectedViolationMessages,
                actualViolationMessages,
                "Should report unknown rule names");
    }

    @Test
    void multipleInvalidRuleNames()
    {
        String json = """
                {
                  "name": "test",
                  "description": "Test scheme with multiple invalid rule names",
                  "rules": [
                    {
                      "name": "background",
                      "style": {
                        "background": "BLACK"
                      }
                    },
                    {
                      "name": "invalid_name_1",
                      "style": {
                        "foreground": "RED"
                      }
                    },
                    {
                      "name": "invalid_name_2",
                      "style": {
                        "foreground": "GREEN"
                      }
                    }
                  ]
                }
                """;

        ColorSchemeDefinition definition = this.parseAndValidate(json);

        List<String> actualViolationMessages = this.validator.validate(definition)
                .stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList();

        List<String> expectedViolationMessages = List.of(
                "Unknown rule name in color scheme");

        assertEquals(
                expectedViolationMessages,
                actualViolationMessages,
                "Should report unknown rule name");
    }

    @Test
    void invalidJson()
    {
        String json = """
                {
                  "name": "",
                  "description": null,
                  "rules": [
                    {
                      "t": "keyword",
                      "style": {
                        "foreground": "INVALID_COLOR"
                      }
                    }
                  ]
                }
                """;

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> this.parseAndValidate(json),
                "Should throw RuntimeException for invalid JSON");

        assertEquals("Failed to parse JSON", exception.getMessage());

        Throwable cause = exception.getCause();
        assertTrue(cause instanceof UnrecognizedPropertyException);
        assertTrue(cause.getMessage().contains("Unrecognized field \"t\""));
    }

    private ColorSchemeDefinition parseAndValidate(String json)
    {
        try
        {
            ObjectMapper objectMapper = ObjectMapperConfig.configure(new ObjectMapper());
            return objectMapper.readValue(json, ColorSchemeDefinition.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }
}
