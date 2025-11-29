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

package cool.klass.deserializer.json;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.visitor.PrimitiveTypeVisitor;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.stack.MutableStack;

public class AssertValuesMatchPrimitiveTypeVisitor implements PrimitiveTypeVisitor {

    @Nonnull
    private final PrimitiveProperty primitiveProperty;

    @Nonnull
    private final JsonNode jsonDataTypeValue;

    private final Object persistentValue;

    @Nonnull
    private final String propertyKind;

    @Nonnull
    private final MutableStack<String> contextStack;

    @Nonnull
    private final String severity;

    @Nonnull
    private final MutableList<String> annotations;

    public AssertValuesMatchPrimitiveTypeVisitor(
        @Nonnull PrimitiveProperty primitiveProperty,
        @Nonnull JsonNode jsonDataTypeValue,
        Object persistentValue,
        @Nonnull String propertyKind,
        @Nonnull MutableStack<String> contextStack,
        String severity,
        @Nonnull MutableList<String> annotations
    ) {
        this.primitiveProperty = Objects.requireNonNull(primitiveProperty);
        this.jsonDataTypeValue = Objects.requireNonNull(jsonDataTypeValue);
        this.persistentValue = persistentValue;
        this.propertyKind = Objects.requireNonNull(propertyKind);
        this.contextStack = Objects.requireNonNull(contextStack);
        this.severity = Objects.requireNonNull(severity);
        this.annotations = Objects.requireNonNull(annotations);
    }

    @Override
    public void visitString() {
        if (!this.jsonDataTypeValue.isTextual()) {
            return;
        }

        String incomingValue = this.jsonDataTypeValue.textValue();
        this.assertValuesMatch(incomingValue);
    }

    @Override
    public void visitInteger() {
        if (!this.jsonDataTypeValue.isIntegralNumber() || !this.jsonDataTypeValue.canConvertToInt()) {
            return;
        }

        int incomingValue = this.jsonDataTypeValue.intValue();
        this.assertValuesMatch(incomingValue);
    }

    @Override
    public void visitLong() {
        if (!this.jsonDataTypeValue.isIntegralNumber() || !this.jsonDataTypeValue.canConvertToLong()) {
            return;
        }

        long incomingValue = this.jsonDataTypeValue.longValue();
        this.assertValuesMatch(incomingValue);
    }

    @Override
    public void visitDouble() {
        if (
            !this.jsonDataTypeValue.isDouble()
            && !this.jsonDataTypeValue.isFloat()
            && !this.jsonDataTypeValue.isInt()
            && !this.jsonDataTypeValue.isLong()
        ) {
            return;
        }

        double incomingValue = this.jsonDataTypeValue.doubleValue();
        this.assertValuesMatch(incomingValue);
    }

    @Override
    public void visitFloat() {
        if (
            (!this.jsonDataTypeValue.isDouble()
                && !this.jsonDataTypeValue.isFloat()
                && !this.jsonDataTypeValue.isInt()
                && !this.jsonDataTypeValue.isLong())
            || !this.hasValidFloatString()
        ) {
            return;
        }

        float incomingValue = this.jsonDataTypeValue.floatValue();
        this.assertValuesMatch(incomingValue);
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
            return;
        }

        boolean incomingValue = this.jsonDataTypeValue.booleanValue();
        this.assertValuesMatch(incomingValue);
    }

    @Override
    public void visitInstant() {
        this.visitTemporal();
    }

    @Override
    public void visitLocalDate() {
        if (!this.jsonDataTypeValue.isTextual()) {
            return;
        }

        String text = this.jsonDataTypeValue.textValue();
        if (text.equals("now")) {
            throw new RuntimeException("TODO: Support now as a value for dates.");
        }
        if (text.equals("infinity")) {
            throw new RuntimeException("TODO: Support infinity as a value for dates.");
        }

        LocalDate incomingValue = LocalDate.parse(text);
        this.assertValuesMatch(incomingValue);
    }

    @Override
    public void visitTemporalInstant() {
        this.visitTemporal();
    }

    @Override
    public void visitTemporalRange() {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitTemporalRange() not implemented yet"
        );
    }

    private void visitTemporal() {
        if (!this.jsonDataTypeValue.isTextual()) {
            return;
        }

        String text = this.jsonDataTypeValue.textValue();
        if (text.equals("now")) {
            throw new RuntimeException("TODO: Support now as a value for dates.");
        }
        if (text.equals("infinity")) {
            throw new RuntimeException("TODO: Support infinity as a value for dates.");
        }

        try {
            Instant incomingValue = Instant.parse(text);
            this.assertValuesMatch(incomingValue);
        } catch (DateTimeParseException e) {
            // Deliberately empty
        }
    }

    private void assertValuesMatch(Object incomingValue) {
        if (Objects.equals(this.persistentValue, incomingValue)) {
            return;
        }

        String annotation = String.format(
            "%s at %s. Mismatched value for %s property '%s.%s: %s%s'. Expected absent value or '%s' but value was '%s'.",
            this.severity,
            this.getContextString(),
            this.propertyKind,
            this.primitiveProperty.getOwningClassifier().getName(),
            this.primitiveProperty.getName(),
            this.primitiveProperty.getType(),
            this.primitiveProperty.isOptional() ? "?" : "",
            this.persistentValue,
            incomingValue
        );
        this.annotations.add(annotation);
    }

    private String getContextString() {
        return this.contextStack.toList().asReversed().makeString(".");
    }
}
