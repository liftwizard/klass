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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ColorSchemeValidationTest {

    private final Validator validator = Validators.newValidator();

    @Test
    void validScheme() {
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

        assertThat(actualViolationMessages).as("There should be no validation violations for a valid scheme").isEmpty();
    }

    @Test
    void missingName() {
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

        assertThat(actualViolationMessages)
            .as("Should have correct violation message for missing name")
            .containsExactly("Color scheme name is required");
    }

    @Test
    void missingDescription() {
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

        assertThat(actualViolationMessages)
            .as("Should have correct violation message for null description")
            .containsExactly("Color scheme description is required");
    }

    @Test
    void emptyRules() {
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

        assertThat(actualViolationMessages)
            .as("Should have correct violation messages for empty rules")
            .containsExactlyInAnyOrder(
                "Color scheme must define a 'background' rule",
                "Color scheme must define at least one rule"
            );
    }

    @Test
    void noBackgroundRule() {
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

        assertThat(actualViolationMessages)
            .as("Should have correct violation message for missing background rule")
            .containsExactly("Color scheme must define a 'background' rule");
    }

    @Test
    void invalidColorName() {
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

        assertThat(actualViolationMessages)
            .as("Should have correct violation message for invalid color name")
            .containsExactly("Invalid foreground color value");

        // Also check the detailed error in the logs
        Set<ConstraintViolation<ColorSchemeRule>> ruleViolations = this.validator.validate(
            definition.rules().getFirst()
        );

        assertThat(ruleViolations).as("Rule itself should be valid since name is present").isEmpty();

        // Style settings should have the validation error
        List<String> actualStyleViolationMessages = this.validator.validate(definition.rules().get(1).style())
            .stream()
            .map(ConstraintViolation::getMessage)
            .sorted()
            .toList();

        assertThat(actualStyleViolationMessages)
            .as("Should have correct style validation messages")
            .containsExactly("Invalid foreground color value");
    }

    @Test
    void invalidHexColor() {
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

        assertThat(actualViolationMessages)
            .as("Should have correct violation message for invalid hex color")
            .containsExactly("Invalid foreground color value");
    }

    @Test
    void invalidNumericColor() {
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

        assertThat(actualViolationMessages)
            .as("Should have correct violation message for out-of-range numeric color")
            .containsExactly("Invalid foreground color value");
    }

    @Test
    void invalidRuleName() {
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

        assertThat(actualViolationMessages)
            .as("Should report unknown rule names")
            .containsExactly("Unknown rule name in color scheme");
    }

    @Test
    void multipleInvalidRuleNames() {
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

        assertThat(actualViolationMessages)
            .as("Should report unknown rule name")
            .containsExactly("Unknown rule name in color scheme");
    }

    @Test
    void invalidJson() {
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

        assertThatThrownBy(() -> this.parseAndValidate(json))
            .as("Should throw RuntimeException for invalid JSON")
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to parse JSON")
            .hasCauseInstanceOf(UnrecognizedPropertyException.class)
            .getCause()
            .hasMessageContaining("Unrecognized field \"t\"");
    }

    private ColorSchemeDefinition parseAndValidate(String json) {
        try {
            ObjectMapper objectMapper = ObjectMapperConfig.configure(new ObjectMapper());
            return objectMapper.readValue(json, ColorSchemeDefinition.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }
}
