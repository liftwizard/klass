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

package cool.klass.generator.service;

import java.util.Objects;

import cool.klass.model.meta.domain.api.visitor.PrimitiveTypeVisitor;

public class PrimitiveSetVisitor implements PrimitiveTypeVisitor {

	private final StringBuilder stringBuilder;
	private final String parameterName;

	public PrimitiveSetVisitor(StringBuilder stringBuilder, String parameterName) {
		this.stringBuilder = Objects.requireNonNull(stringBuilder);
		this.parameterName = Objects.requireNonNull(parameterName);
	}

	@Override
	public void visitString() {
		this.stringBuilder.append(this.parameterName);
	}

	@Override
	public void visitInteger() {
		this.stringBuilder.append("SetAdapter.adapt(");
		this.stringBuilder.append(this.parameterName);
		this.stringBuilder.append(").collectInt(x -> x, IntSets.mutable.empty())");
	}

	@Override
	public void visitLong() {
		this.stringBuilder.append("SetAdapter.adapt(");
		this.stringBuilder.append(this.parameterName);
		this.stringBuilder.append(").collectLong(x -> x, LongSets.mutable.empty())");
	}

	@Override
	public void visitDouble() {
		this.stringBuilder.append("SetAdapter.adapt(");
		this.stringBuilder.append(this.parameterName);
		this.stringBuilder.append(").collectDouble(x -> x, DoubleSets.mutable.empty())");
	}

	@Override
	public void visitFloat() {
		this.stringBuilder.append("SetAdapter.adapt(");
		this.stringBuilder.append(this.parameterName);
		this.stringBuilder.append(").collectFloat(x -> x, FloatSets.mutable.empty())");
	}

	@Override
	public void visitBoolean() {
		this.stringBuilder.append("SetAdapter.adapt(");
		this.stringBuilder.append(this.parameterName);
		this.stringBuilder.append(").collectBoolean(x -> x, BooleanSets.mutable.empty())");
	}

	@Override
	public void visitInstant() {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".visitInstant() not implemented yet"
		);
	}

	@Override
	public void visitLocalDate() {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".visitLocalDate() not implemented yet"
		);
	}

	@Override
	public void visitTemporalInstant() {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".visitTemporalInstant() not implemented yet"
		);
	}

	@Override
	public void visitTemporalRange() {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".visitTemporalRange() not implemented yet"
		);
	}
}
