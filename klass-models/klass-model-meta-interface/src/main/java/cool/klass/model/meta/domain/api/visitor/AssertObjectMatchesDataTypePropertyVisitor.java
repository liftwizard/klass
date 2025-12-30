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

package cool.klass.model.meta.domain.api.visitor;

import java.util.Objects;

import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;

public class AssertObjectMatchesDataTypePropertyVisitor implements DataTypePropertyVisitor {

	private final Object object;

	public AssertObjectMatchesDataTypePropertyVisitor(Object object) {
		this.object = Objects.requireNonNull(object);
	}

	@Override
	public void visitEnumerationProperty(EnumerationProperty enumerationProperty) {
		if (this.object instanceof EnumerationLiteral) {
			return;
		}
		this.throwError("Enumeration Literal");
	}

	@Override
	public void visitString(PrimitiveProperty primitiveProperty) {
		if (this.object instanceof String) {
			return;
		}
		this.throwError("String");
	}

	@Override
	public void visitInteger(PrimitiveProperty primitiveProperty) {
		if (this.object instanceof Integer) {
			return;
		}
		this.throwError("Integer");
	}

	@Override
	public void visitLong(PrimitiveProperty primitiveProperty) {
		if (this.object instanceof Long) {
			return;
		}
		this.throwError("Long");
	}

	@Override
	public void visitDouble(PrimitiveProperty primitiveProperty) {
		if (this.object instanceof Double) {
			return;
		}
		this.throwError("Double");
	}

	@Override
	public void visitFloat(PrimitiveProperty primitiveProperty) {
		if (this.object instanceof Float) {
			return;
		}
		this.throwError("Float");
	}

	@Override
	public void visitBoolean(PrimitiveProperty primitiveProperty) {
		if (this.object instanceof Boolean) {
			return;
		}

		this.throwError("Boolean");
	}

	@Override
	public void visitInstant(PrimitiveProperty primitiveProperty) {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".visitInstant() not implemented yet"
		);
	}

	@Override
	public void visitLocalDate(PrimitiveProperty primitiveProperty) {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".visitLocalDate() not implemented yet"
		);
	}

	@Override
	public void visitTemporalInstant(PrimitiveProperty primitiveProperty) {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".visitTemporalInstant() not implemented yet"
		);
	}

	@Override
	public void visitTemporalRange(PrimitiveProperty primitiveProperty) {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".visitTemporalRange() not implemented yet"
		);
	}

	private void throwError(String expected) {
		String error = String.format(
			"Expected %s but got object of type %s: %s",
			expected,
			this.object.getClass().getSimpleName(),
			this.object
		);
		throw new IllegalArgumentException(error);
	}
}
