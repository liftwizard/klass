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

package cool.klass.deserializer.json.type;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import cool.klass.deserializer.json.context.ContextStack;
import cool.klass.model.meta.domain.api.modifier.Modifier;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.visitor.PrimitiveTypeVisitor;

public class JsonTypeCheckingPrimitiveTypeVisitor implements PrimitiveTypeVisitor {

    @Nonnull
    private final ContextStack contextStack;

    @Nonnull
    private final PrimitiveProperty primitiveProperty;

    @Nonnull
    private final JsonNode jsonDataTypeValue;

    public JsonTypeCheckingPrimitiveTypeVisitor(
        ContextStack contextStack,
        PrimitiveProperty primitiveProperty,
        JsonNode jsonDataTypeValue
    ) {
        this.contextStack = Objects.requireNonNull(contextStack);

        this.primitiveProperty = Objects.requireNonNull(primitiveProperty);
        this.jsonDataTypeValue = Objects.requireNonNull(jsonDataTypeValue);
    }

    private void emitTypeError() {
        String error = String.format(
            "Expected property with type '%s.%s: %s%s' but got '%s' with type '%s'.",
            this.primitiveProperty.getOwningClassifier().getName(),
            this.primitiveProperty.getName(),
            this.primitiveProperty.getType().getPrettyName(),
            this.primitiveProperty.isOptional() ? "?" : "",
            this.jsonDataTypeValue,
            this.jsonDataTypeValue.getNodeType().toString().toLowerCase(Locale.ROOT)
        );
        this.contextStack.addError(error);
    }

    // TODO: Test nullable primitives

    @Override
    public void visitString() {
        if (!this.jsonDataTypeValue.isTextual()) {
            this.emitTypeError();
        }
    }

    @Override
    public void visitInteger() {
        if (!this.jsonDataTypeValue.isIntegralNumber() || !this.jsonDataTypeValue.canConvertToInt()) {
            this.emitTypeError();
        }
    }

    @Override
    public void visitLong() {
        if (!this.jsonDataTypeValue.isIntegralNumber() || !this.jsonDataTypeValue.canConvertToLong()) {
            this.emitTypeError();
        }
    }

    @Override
    public void visitDouble() {
        if (
            !this.jsonDataTypeValue.isDouble() &&
            !this.jsonDataTypeValue.isFloat() &&
            !this.jsonDataTypeValue.isInt() &&
            !this.jsonDataTypeValue.isLong()
        ) {
            this.emitTypeError();
        }
    }

    @Override
    public void visitFloat() {
        if (
            (!this.jsonDataTypeValue.isDouble() &&
                !this.jsonDataTypeValue.isFloat() &&
                !this.jsonDataTypeValue.isInt() &&
                !this.jsonDataTypeValue.isLong()) ||
            !this.hasValidFloatString()
        ) {
            this.emitTypeError();
        }
    }

    private boolean hasValidFloatString() {
        double doubleValue = this.jsonDataTypeValue.doubleValue();
        float floatValue = this.jsonDataTypeValue.floatValue();
        String doubleString = Double.toString(doubleValue);
        String floatString = Float.toString(floatValue);
        return doubleString.equals(floatString);
    }

    @Override
    public void visitBoolean() {
        if (!this.jsonDataTypeValue.isBoolean()) {
            this.emitTypeError();
        }
    }

    @Override
    public void visitInstant() {
        this.visitTemporal();
    }

    @Override
    public void visitLocalDate() {
        if (!this.jsonDataTypeValue.isTextual()) {
            this.emitTypeError();
            return;
        }

        String text = this.jsonDataTypeValue.textValue();
        if (text.equals("now") || text.equals("infinity")) {
            return;
        }

        try {
            LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            String error = String.format(
                "Expected property with type '%s' but got '%s' which could not be parsed by LocalDate.parse() which expects a String like '1999-12-31.",
                this.primitiveProperty,
                this.jsonDataTypeValue
            );
            this.contextStack.addError(error);
        }
    }

    @Override
    public void visitTemporalInstant() {
        this.visitTemporal();
    }

    @Override
    public void visitTemporalRange() {
        this.visitTemporal();
    }

    private void visitTemporal() {
        if (
            this.jsonDataTypeValue.isNull() &&
            this.primitiveProperty.isTemporalInstant() &&
            this.primitiveProperty.getModifiers().anySatisfy(Modifier::isTo)
        ) {
            // TODO: Other validations might make this one unreachable
            return;
        }

        if (!this.jsonDataTypeValue.isTextual()) {
            this.emitTypeError();
            return;
        }

        String text = this.jsonDataTypeValue.textValue();
        if (text.equals("now") || text.equals("infinity")) {
            return;
        }

        try {
            Instant.parse(text);
        } catch (DateTimeParseException e) {
            String error = String.format(
                "Expected property with type '%s' but got '%s' which could not be parsed by java.time.format.DateTimeFormatter.ISO_INSTANT which expects a String like '1999-12-31T23:59:59Z'",
                this.primitiveProperty,
                this.jsonDataTypeValue
            );
            this.contextStack.addError(error);
        }
    }
}
