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

package cool.klass.deserializer.json.test.primitive;

import java.io.IOException;

import javax.annotation.Nonnull;

import cool.klass.deserializer.json.OperationMode;
import cool.klass.model.meta.domain.api.Klass;
import org.junit.jupiter.api.Test;

class PropertiesRequiredTest extends AbstractPrimitiveValidatorTest {

	@Override
	@Test
	public void validate_good() throws IOException {
		this.validate("validate_good");
	}

	@Test
	@Override
	public void validate_extra_properties() throws IOException {
		this.validate("validate_extra_properties");
	}

	@Override
	@Test
	public void validate_expected_primitive_actual_missing() throws IOException {
		this.validate("validate_expected_primitive_actual_missing");
	}

	@Override
	@Test
	public void validate_expected_primitive_actual_array() throws IOException {
		this.validate("validate_expected_primitive_actual_array");
	}

	@Override
	@Test
	public void validate_expected_primitive_actual_object() throws IOException {
		this.validate("validate_expected_primitive_actual_object");
	}

	@Override
	@Test
	public void validate_expected_primitive_actual_null() throws IOException {
		this.validate("validate_expected_primitive_actual_null");
	}

	@Override
	@Test
	public void validate_expected_primitive_actual_string() throws IOException {
		this.validate("validate_expected_primitive_actual_string");
	}

	@Nonnull
	@Override
	protected Klass getKlass() {
		return this.domainModel.getClassByName("PropertiesRequired");
	}

	@Nonnull
	@Override
	protected OperationMode getMode() {
		return OperationMode.CREATE;
	}
}
