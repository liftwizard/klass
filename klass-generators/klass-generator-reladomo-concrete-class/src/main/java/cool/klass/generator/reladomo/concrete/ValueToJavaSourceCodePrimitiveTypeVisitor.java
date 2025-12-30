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

package cool.klass.generator.reladomo.concrete;

import java.time.Instant;
import java.time.LocalDate;

import cool.klass.model.meta.domain.api.visitor.PrimitiveTypeVisitor;
import org.apache.commons.text.StringEscapeUtils;

@SuppressWarnings("RedundantCast")
public class ValueToJavaSourceCodePrimitiveTypeVisitor implements PrimitiveTypeVisitor {

	private final Object value;

	private String result;

	public ValueToJavaSourceCodePrimitiveTypeVisitor(Object value) {
		this.value = value;
	}

	public String getResult() {
		return this.result;
	}

	@Override
	public void visitString() {
		this.result = "\"" + StringEscapeUtils.escapeJava((String) this.value) + "\"";
	}

	@Override
	public void visitInteger() {
		this.result = Integer.toString((Integer) this.value);
	}

	@Override
	public void visitLong() {
		this.result = (Long) this.value + "L";
	}

	@Override
	public void visitDouble() {
		this.result = Double.toString((Double) this.value);
	}

	@Override
	public void visitFloat() {
		this.result = (Float) this.value + "f";
	}

	@Override
	public void visitBoolean() {
		this.result = Boolean.toString((Boolean) this.value);
	}

	@Override
	public void visitInstant() {
		this.result = "Instant.parse(\"" + ((Instant) this.value) + "\")";
	}

	@Override
	public void visitLocalDate() {
		this.result = "LocalDate.parse(\"" + ((LocalDate) this.value) + "\")";
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
