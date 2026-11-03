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

import cool.klass.deserializer.json.test.AbstractValidatorTest;
import org.junit.jupiter.api.Test;

public abstract class AbstractPrimitiveValidatorTest extends AbstractValidatorTest {

	@Test
	void validateAllCases() throws IOException {
		this.validate("validate_good");
		this.validate("validate_extra_properties");
		this.validate("validate_expected_primitive_actual_missing");
		this.validate("validate_expected_primitive_actual_array");
		this.validate("validate_expected_primitive_actual_object");
		this.validate("validate_expected_primitive_actual_null");
		this.validate("validate_expected_primitive_actual_string");
	}
}
