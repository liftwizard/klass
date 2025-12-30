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

import com.fasterxml.jackson.databind.JsonNode;
import cool.klass.model.meta.domain.api.visitor.PrimitiveTypeVisitor;

public class JsonPrimitiveTypeValueVisitor implements PrimitiveTypeVisitor {

	private final JsonNode jsonDataTypeValue;

	private Object result;

	public JsonPrimitiveTypeValueVisitor(JsonNode jsonDataTypeValue) {
		this.jsonDataTypeValue = Objects.requireNonNull(jsonDataTypeValue);
	}

	public Object getResult() {
		return this.result;
	}

	@Override
	public void visitString() {
		if (!this.jsonDataTypeValue.isTextual()) {
			throw new AssertionError();
		}

		this.result = this.jsonDataTypeValue.textValue();
	}

	@Override
	public void visitInteger() {
		if (!this.jsonDataTypeValue.isIntegralNumber() || !this.jsonDataTypeValue.canConvertToInt()) {
			throw new AssertionError();
		}

		this.result = this.jsonDataTypeValue.intValue();
	}

	@Override
	public void visitLong() {
		if (!this.jsonDataTypeValue.isIntegralNumber() || !this.jsonDataTypeValue.canConvertToLong()) {
			throw new AssertionError();
		}

		this.result = this.jsonDataTypeValue.longValue();
	}

	@Override
	public void visitDouble() {
		if (
			!this.jsonDataTypeValue.isDouble()
			&& !this.jsonDataTypeValue.isFloat()
			&& !this.jsonDataTypeValue.isInt()
			&& !this.jsonDataTypeValue.isLong()
		) {
			throw new AssertionError();
		}

		this.result = this.jsonDataTypeValue.doubleValue();
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
			throw new AssertionError();
		}

		this.result = this.jsonDataTypeValue.floatValue();
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
			throw new AssertionError();
		}

		this.result = this.jsonDataTypeValue.booleanValue();
	}

	@Override
	public void visitInstant() {
		this.visitTemporal();
	}

	@Override
	public void visitLocalDate() {
		if (!this.jsonDataTypeValue.isTextual()) {
			throw new AssertionError();
		}

		String text = this.jsonDataTypeValue.textValue();
		if (text.equals("now")) {
			throw new RuntimeException("TODO: Support 'now' as a value for dates.");
		}
		if (text.equals("infinity")) {
			throw new RuntimeException("TODO: Support 'infinity' as a value for dates.");
		}

		this.result = LocalDate.parse(text);
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
		if (!this.jsonDataTypeValue.isTextual()) {
			throw new AssertionError();
		}

		String text = this.jsonDataTypeValue.textValue();
		if (text.equals("now")) {
			throw new RuntimeException("TODO: Support 'now' as a value for dates.");
		}
		if (text.equals("infinity")) {
			throw new RuntimeException("TODO: Support 'infinity' as a value for dates.");
		}

		try {
			this.result = Instant.parse(text);
		} catch (DateTimeParseException e) {
			throw new AssertionError(e);
		}
	}
}
