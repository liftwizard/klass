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

import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.AssociationEndSignature;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.ParameterizedProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.PropertyVisitor;
import cool.klass.model.meta.domain.api.visitor.PrimitiveTypeVisitor;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.stack.MutableStack;

public class AssertValuesMatchPropertyVisitor implements PropertyVisitor {

	@Nonnull
	private final JsonNode jsonDataTypeValue;

	private final Object value;

	@Nonnull
	private final String propertyKind;

	@Nonnull
	private final MutableStack<String> contextStack;

	@Nonnull
	private final String severity;

	@Nonnull
	private final MutableList<String> annotations;

	public AssertValuesMatchPropertyVisitor(
		@Nonnull JsonNode jsonDataTypeValue,
		Object value,
		@Nonnull String propertyKind,
		@Nonnull MutableStack<String> contextStack,
		@Nonnull String severity,
		@Nonnull MutableList<String> annotations
	) {
		this.jsonDataTypeValue = Objects.requireNonNull(jsonDataTypeValue);
		this.value = value;
		this.propertyKind = Objects.requireNonNull(propertyKind);
		this.contextStack = Objects.requireNonNull(contextStack);
		this.severity = Objects.requireNonNull(severity);
		this.annotations = Objects.requireNonNull(annotations);
	}

	@Override
	public void visitPrimitiveProperty(@Nonnull PrimitiveProperty primitiveProperty) {
		PrimitiveTypeVisitor visitor = new AssertValuesMatchPrimitiveTypeVisitor(
			primitiveProperty,
			this.jsonDataTypeValue,
			this.value,
			this.propertyKind,
			this.contextStack,
			this.severity,
			this.annotations
		);
		primitiveProperty.getType().visit(visitor);
	}

	@Override
	public void visitEnumerationProperty(@Nonnull EnumerationProperty enumerationProperty) {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".visitEnumerationProperty() not implemented yet"
		);
	}

	@Override
	public void visitAssociationEndSignature(AssociationEndSignature associationEndSignature) {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".visitAssociationEndSignature() not implemented yet"
		);
	}

	@Override
	public void visitAssociationEnd(@Nonnull AssociationEnd associationEnd) {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".visitAssociationEnd() not implemented yet"
		);
	}

	@Override
	public void visitParameterizedProperty(@Nonnull ParameterizedProperty parameterizedProperty) {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".visitParameterizedProperty() not implemented yet"
		);
	}
}
