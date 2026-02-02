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

package cool.klass.data.store.reladomo;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.gs.fw.common.mithra.attribute.Attribute;
import com.gs.fw.common.mithra.attribute.BooleanAttribute;
import com.gs.fw.common.mithra.attribute.DoubleAttribute;
import com.gs.fw.common.mithra.attribute.FloatAttribute;
import com.gs.fw.common.mithra.attribute.IntegerAttribute;
import com.gs.fw.common.mithra.attribute.LongAttribute;
import com.gs.fw.common.mithra.attribute.StringAttribute;
import com.gs.fw.common.mithra.finder.Operation;
import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.visitor.DataTypePropertyVisitor;

public class OperationVisitor implements DataTypePropertyVisitor {

	private final Attribute attribute;
	private final Object key;
	private Operation result;

	public OperationVisitor(@Nonnull Attribute attribute, @Nonnull Object key) {
		this.attribute = Objects.requireNonNull(attribute);
		this.key = Objects.requireNonNull(key);
	}

	public Operation getResult() {
		return this.result;
	}

	@Override
	public void visitEnumerationProperty(EnumerationProperty enumerationProperty) {
		var enumerationLiteral = (EnumerationLiteral) this.key;
		String prettyName = enumerationLiteral.getPrettyName();
		this.result = ((StringAttribute<?>) this.attribute).eq(prettyName);
	}

	@Override
	public void visitString(PrimitiveProperty primitiveProperty) {
		this.result = ((StringAttribute<?>) this.attribute).eq((String) this.key);
	}

	@Override
	public void visitInteger(PrimitiveProperty primitiveProperty) {
		this.result = ((IntegerAttribute<?>) this.attribute).eq((Integer) this.key);
	}

	@Override
	public void visitLong(PrimitiveProperty primitiveProperty) {
		this.result = ((LongAttribute<?>) this.attribute).eq((Long) this.key);
	}

	@Override
	public void visitDouble(PrimitiveProperty primitiveProperty) {
		this.result = ((DoubleAttribute<?>) this.attribute).eq((Double) this.key);
	}

	@Override
	public void visitFloat(PrimitiveProperty primitiveProperty) {
		this.result = ((FloatAttribute<?>) this.attribute).eq((Float) this.key);
	}

	@Override
	public void visitBoolean(PrimitiveProperty primitiveProperty) {
		this.result = ((BooleanAttribute<?>) this.attribute).eq((Boolean) this.key);
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
}
