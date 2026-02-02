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

package cool.klass.reladomo.tree.serializer;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import cool.klass.model.meta.domain.api.Enumeration;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.visitor.DataTypePropertyVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Move this to a shared module
public class ReflectionSetterDataTypePropertyVisitor implements DataTypePropertyVisitor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionSetterDataTypePropertyVisitor.class);

	private static final Converter<String, String> LOWER_TO_UPPER_CAMEL = CaseFormat.LOWER_CAMEL.converterTo(
		CaseFormat.UPPER_CAMEL
	);

	private final ReflectionCache reflectionCache;
	private final Object object;
	private final Object primitiveValue;

	public ReflectionSetterDataTypePropertyVisitor(
		ReflectionCache reflectionCache,
		Object object,
		Object primitiveValue
	) {
		this.reflectionCache = Objects.requireNonNull(reflectionCache);
		this.object = Objects.requireNonNull(object);
		this.primitiveValue = Objects.requireNonNull(primitiveValue);
	}

	@Override
	public void visitEnumerationProperty(EnumerationProperty enumerationProperty) {
		String propertyName = enumerationProperty.getName();
		String methodName = "set" + LOWER_TO_UPPER_CAMEL.convert(propertyName);
		Enumeration type = enumerationProperty.getType();
		String dtoFQCN = type.getPackageName() + ".dto." + type.getName() + "DTO";
		Class<?> objectClass = this.object.getClass();

		try {
			Class aClass = this.reflectionCache.classForName(dtoFQCN);
			Method method = this.reflectionCache.getMethod(objectClass, methodName, aClass);
			Object enumConstant = Enum.valueOf(aClass, this.primitiveValue.toString());
			method.invoke(this.object, enumConstant);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void visitString(PrimitiveProperty primitiveProperty) {
		this.visitPrimitiveProperty(primitiveProperty, String.class);
	}

	@Override
	public void visitInteger(PrimitiveProperty primitiveProperty) {
		this.visitPrimitiveProperty(primitiveProperty, Integer.class);
	}

	@Override
	public void visitLong(PrimitiveProperty primitiveProperty) {
		this.visitPrimitiveProperty(primitiveProperty, Long.class);
	}

	@Override
	public void visitDouble(PrimitiveProperty primitiveProperty) {
		this.visitPrimitiveProperty(primitiveProperty, Double.class);
	}

	@Override
	public void visitFloat(PrimitiveProperty primitiveProperty) {
		this.visitPrimitiveProperty(primitiveProperty, Float.class);
	}

	@Override
	public void visitBoolean(PrimitiveProperty primitiveProperty) {
		this.visitPrimitiveProperty(primitiveProperty, Boolean.class);
	}

	@Override
	public void visitInstant(PrimitiveProperty primitiveProperty) {
		this.visitPrimitiveProperty(primitiveProperty, Instant.class);
	}

	@Override
	public void visitLocalDate(PrimitiveProperty primitiveProperty) {
		this.visitPrimitiveProperty(primitiveProperty, LocalDate.class);
	}

	@Override
	public void visitTemporalInstant(PrimitiveProperty primitiveProperty) {
		this.visitPrimitiveProperty(primitiveProperty, Instant.class);
	}

	@Override
	public void visitTemporalRange(PrimitiveProperty primitiveProperty) {
		LOGGER.info("{}.visitTemporalRange() not implemented yet", this.getClass().getSimpleName());
	}

	private void visitPrimitiveProperty(PrimitiveProperty primitiveProperty, Class<?>... parameterTypes) {
		Class<?> objectClass = this.object.getClass();
		String propertyName = primitiveProperty.getName();
		String methodName = "set" + LOWER_TO_UPPER_CAMEL.convert(propertyName);

		Method method = this.reflectionCache.getMethod(objectClass, methodName, parameterTypes);

		try {
			method.invoke(this.object, this.primitiveValue);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
