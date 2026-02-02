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

package cool.klass.generator.grahql.reladomo.finder;

import cool.klass.model.meta.domain.api.visitor.PrimitiveTypeVisitor;

public class PrimitiveTypeSourceCodeVisitor implements PrimitiveTypeVisitor {

	private String sourceCode;

	public String getSourceCode() {
		return this.sourceCode;
	}

	@Override
	public void visitString() {
		this.sourceCode = "String";
	}

	@Override
	public void visitInteger() {
		this.sourceCode = "Integer";
	}

	@Override
	public void visitLong() {
		this.sourceCode = "Long";
	}

	@Override
	public void visitDouble() {
		this.sourceCode = "Double";
	}

	@Override
	public void visitFloat() {
		this.sourceCode = "Float";
	}

	@Override
	public void visitBoolean() {
		this.sourceCode = "Boolean";
	}

	@Override
	public void visitInstant() {
		this.sourceCode = "Timestamp";
	}

	@Override
	public void visitLocalDate() {
		this.sourceCode = "Date";
	}

	@Override
	public void visitTemporalInstant() {
		this.sourceCode = "Timestamp";
	}

	@Override
	public void visitTemporalRange() {
		this.sourceCode = "AsOf";
	}
}
