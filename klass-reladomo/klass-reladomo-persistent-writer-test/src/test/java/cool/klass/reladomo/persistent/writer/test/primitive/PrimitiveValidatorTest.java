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

package cool.klass.reladomo.persistent.writer.test.primitive;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public interface PrimitiveValidatorTest {
	@Test
	void validate_good() throws IOException;

	@Test
	void validate_mutation_context() throws IOException;

	@Test
	void validate_extra_properties() throws IOException;

	@Test
	void validate_expected_primitive_actual_missing() throws IOException;

	@Test
	void validate_expected_primitive_actual_array() throws IOException;

	@Test
	void validate_expected_primitive_actual_object() throws IOException;

	@Test
	void validate_expected_primitive_actual_null() throws IOException;

	@Test
	void validate_expected_primitive_actual_string() throws IOException;

	@Test
	void validate_version_expected_primitive_actual_object() throws IOException;
}
