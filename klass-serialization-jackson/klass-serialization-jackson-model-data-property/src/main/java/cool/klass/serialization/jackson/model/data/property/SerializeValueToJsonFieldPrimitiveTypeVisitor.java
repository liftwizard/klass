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

package cool.klass.serialization.jackson.model.data.property;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import cool.klass.model.meta.domain.api.visitor.PrimitiveTypeVisitor;

public class SerializeValueToJsonFieldPrimitiveTypeVisitor implements PrimitiveTypeVisitor {

	private final JsonGenerator jsonGenerator;
	private final String propertyName;
	private final Object value;

	public SerializeValueToJsonFieldPrimitiveTypeVisitor(
		JsonGenerator jsonGenerator,
		String primitivePropertyName,
		Object value
	) {
		this.jsonGenerator = Objects.requireNonNull(jsonGenerator);
		this.propertyName = Objects.requireNonNull(primitivePropertyName);
		this.value = Objects.requireNonNull(value);
	}

	@Override
	public void visitString() throws IOException {
		this.jsonGenerator.writeStringField(this.propertyName, (String) this.value);
	}

	@Override
	public void visitInteger() throws IOException {
		this.jsonGenerator.writeNumberField(this.propertyName, (Integer) this.value);
	}

	@Override
	public void visitLong() throws IOException {
		this.jsonGenerator.writeNumberField(this.propertyName, (Long) this.value);
	}

	@Override
	public void visitDouble() throws IOException {
		this.jsonGenerator.writeNumberField(this.propertyName, (Double) this.value);
	}

	@Override
	public void visitFloat() throws IOException {
		this.jsonGenerator.writeNumberField(this.propertyName, (Float) this.value);
	}

	@Override
	public void visitBoolean() throws IOException {
		this.jsonGenerator.writeBooleanField(this.propertyName, (Boolean) this.value);
	}

	@Override
	public void visitInstant() throws IOException {
		this.jsonGenerator.writeStringField(this.propertyName, this.value.toString());
	}

	@Override
	public void visitLocalDate() throws IOException {
		this.jsonGenerator.writeStringField(this.propertyName, this.value.toString());
	}

	@Override
	public void visitTemporalInstant() throws IOException {
		this.jsonGenerator.writeStringField(this.propertyName, this.value.toString());
	}

	@Override
	public void visitTemporalRange() {
		throw new IllegalStateException();
	}
}
