/*
 * Copyright 2026 Craig Motlin
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

package cool.klass.generator.react.prop.type;

import java.util.Objects;

import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.visitor.DataTypePropertyVisitor;

public class PropTypeSourceCodeDataTypePropertyVisitor implements DataTypePropertyVisitor {

	private String result;

	public String getResult() {
		return Objects.requireNonNull(this.result);
	}

	@Override
	public void visitEnumerationProperty(EnumerationProperty enumerationProperty) {
		this.result = "string";
	}

	@Override
	public void visitString(PrimitiveProperty primitiveProperty) {
		this.result = "string";
	}

	@Override
	public void visitInteger(PrimitiveProperty primitiveProperty) {
		this.result = "number";
	}

	@Override
	public void visitLong(PrimitiveProperty primitiveProperty) {
		this.result = "number";
	}

	@Override
	public void visitDouble(PrimitiveProperty primitiveProperty) {
		this.result = "number";
	}

	@Override
	public void visitFloat(PrimitiveProperty primitiveProperty) {
		this.result = "number";
	}

	@Override
	public void visitBoolean(PrimitiveProperty primitiveProperty) {
		this.result = "bool";
	}

	@Override
	public void visitInstant(PrimitiveProperty primitiveProperty) {
		this.result = "string";
	}

	@Override
	public void visitLocalDate(PrimitiveProperty primitiveProperty) {
		this.result = "string";
	}

	@Override
	public void visitTemporalInstant(PrimitiveProperty primitiveProperty) {
		this.result = "string";
	}

	@Override
	public void visitTemporalRange(PrimitiveProperty primitiveProperty) {
		this.result = "string";
	}
}
