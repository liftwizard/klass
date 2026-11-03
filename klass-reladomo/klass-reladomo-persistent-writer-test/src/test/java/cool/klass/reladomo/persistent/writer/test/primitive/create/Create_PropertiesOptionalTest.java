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

package cool.klass.reladomo.persistent.writer.test.primitive.create;

import java.io.IOException;

import javax.annotation.Nonnull;

import cool.klass.deserializer.json.OperationMode;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.junit.jupiter.api.Test;

class Create_PropertiesOptionalTest extends AbstractCreateValidatorTest {

	@Test
	void validateAllCases() throws IOException {
		this.validate("validate_good");
		this.validate("validate_mutation_context");
		this.validate("validate_extra_properties");
		this.validate("validate_expected_primitive_actual_missing");
		this.validate("validate_expected_primitive_actual_array");
		this.validate("validate_expected_primitive_actual_object");
		this.validate("validate_expected_primitive_actual_null");
		this.validate("validate_expected_primitive_actual_string");
		this.validate("validate_version_expected_primitive_actual_object");
	}

	@Nonnull
	@Override
	protected Klass getKlass() {
		return this.domainModel.getClassByName("PropertiesOptional");
	}

	@Nonnull
	@Override
	protected OperationMode getMode() {
		return OperationMode.CREATE;
	}

	@Override
	protected ImmutableMap<DataTypeProperty, Object> getPropertyDataFromUrl() {
		DataTypeProperty dataTypeProperty = this.getKlass().getDataTypePropertyByName("propertiesOptionalId");
		return Maps.immutable.with(dataTypeProperty, 1L);
	}
}
