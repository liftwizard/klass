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

package cool.klass.deserializer.json.test.association.owned.natural.fromone.toone;

import java.io.IOException;

import javax.annotation.Nonnull;

import cool.klass.deserializer.json.OperationMode;
import cool.klass.deserializer.json.test.AbstractValidatorTest;
import cool.klass.model.meta.domain.api.Klass;
import org.junit.jupiter.api.Test;

class OwnedNaturalOneToOneTest extends AbstractValidatorTest {

	@Test
	void validate_good() throws IOException {
		this.validate("validate_good");
	}

	@Test
	void validate_backwards_association_end() throws IOException {
		this.validate("validate_backwards_association_end");
	}

	@Test
	void validate_extra_properties() throws IOException {
		this.validate("validate_extra_properties");
	}

	@Test
	void validate_expected_actual_missing() throws IOException {
		this.validate("validate_expected_actual_missing");
	}

	@Test
	void validate_expected_actual_array() throws IOException {
		this.validate("validate_expected_actual_array");
	}

	@Test
	void validate_expected_actual_object() throws IOException {
		this.validate("validate_expected_actual_object");
	}

	@Test
	void validate_expected_actual_null() throws IOException {
		this.validate("validate_expected_actual_null");
	}

	@Test
	void validate_expected_actual_boolean() throws IOException {
		this.validate("validate_expected_actual_boolean");
	}

	@Nonnull
	@Override
	protected Klass getKlass() {
		return this.domainModel.getClassByName("OwnedNaturalOneToOneSource");
	}

	@Nonnull
	@Override
	protected OperationMode getMode() {
		return OperationMode.CREATE;
	}
}
