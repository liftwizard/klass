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

package cool.klass.generator.reladomo.interfacefile;

import com.gs.fw.common.mithra.generator.metamodel.AttributeInterfaceType;
import cool.klass.model.meta.domain.api.visitor.PrimitiveTypeVisitor;

class AttributeInterfaceTypeVisitor implements PrimitiveTypeVisitor {

	private final AttributeInterfaceType attributeType;

	AttributeInterfaceTypeVisitor(AttributeInterfaceType attributeType) {
		this.attributeType = attributeType;
	}

	@Override
	public void visitString() {
		this.attributeType.setJavaType("String");
	}

	@Override
	public void visitInteger() {
		this.attributeType.setJavaType("int");
	}

	@Override
	public void visitLong() {
		this.attributeType.setJavaType("long");
	}

	@Override
	public void visitDouble() {
		this.attributeType.setJavaType("double");
	}

	@Override
	public void visitFloat() {
		this.attributeType.setJavaType("float");
	}

	@Override
	public void visitBoolean() {
		this.attributeType.setJavaType("boolean");
	}

	@Override
	public void visitInstant() {
		this.attributeType.setJavaType("Timestamp");
	}

	@Override
	public void visitLocalDate() {
		this.attributeType.setJavaType("Date");
	}

	@Override
	public void visitTemporalInstant() {
		throw new AssertionError();
	}

	@Override
	public void visitTemporalRange() {
		throw new AssertionError();
	}
}
