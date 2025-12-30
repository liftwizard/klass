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

package cool.klass.generator.relational.schema;

import java.util.Objects;

import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.validation.NumericPropertyValidation;
import cool.klass.model.meta.domain.api.visitor.DataTypePropertyVisitor;

public class RelationalSchemaGeneratorDataTypePropertyVisitor implements DataTypePropertyVisitor {

	private String dataTypeSourceCode;

	public String getDataTypeSourceCode() {
		Objects.requireNonNull(this.dataTypeSourceCode);
		return this.dataTypeSourceCode;
	}

	private void handleDataTypeProperty(DataTypeProperty dataTypeProperty) {
		int maxLength = dataTypeProperty
			.getMaxLengthPropertyValidation()
			.map(NumericPropertyValidation::getNumber)
			.orElse(255);
		this.dataTypeSourceCode = "varchar(" + maxLength + ')';
	}

	@Override
	public void visitEnumerationProperty(EnumerationProperty enumerationProperty) {
		this.handleDataTypeProperty(enumerationProperty);
	}

	@Override
	public void visitString(PrimitiveProperty primitiveProperty) {
		this.handleDataTypeProperty(primitiveProperty);
	}

	@Override
	public void visitInteger(PrimitiveProperty primitiveProperty) {
		this.dataTypeSourceCode = "int";
	}

	@Override
	public void visitLong(PrimitiveProperty primitiveProperty) {
		this.dataTypeSourceCode = "bigint";
	}

	@Override
	public void visitDouble(PrimitiveProperty primitiveProperty) {
		this.dataTypeSourceCode = "float8";
	}

	@Override
	public void visitFloat(PrimitiveProperty primitiveProperty) {
		this.dataTypeSourceCode = "float4";
	}

	@Override
	public void visitBoolean(PrimitiveProperty primitiveProperty) {
		this.dataTypeSourceCode = "boolean";
	}

	@Override
	public void visitInstant(PrimitiveProperty primitiveProperty) {
		this.dataTypeSourceCode = "timestamp";
	}

	@Override
	public void visitLocalDate(PrimitiveProperty primitiveProperty) {
		this.dataTypeSourceCode = "date";
	}

	@Override
	public void visitTemporalInstant(PrimitiveProperty primitiveProperty) {
		this.dataTypeSourceCode = "timestamp";
	}

	@Override
	public void visitTemporalRange(PrimitiveProperty primitiveProperty) {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".visitTemporalRange() not implemented yet"
		);
	}
}
