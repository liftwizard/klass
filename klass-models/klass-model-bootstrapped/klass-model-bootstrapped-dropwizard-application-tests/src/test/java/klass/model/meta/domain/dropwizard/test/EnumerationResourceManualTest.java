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

package klass.model.meta.domain.dropwizard.test;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
class EnumerationResourceManualTest extends AbstractResourceTestCase {

	@Test
	@Order(1)
	void getAllMeta() {
		this.assertUrlReturns("getAllMeta", "/meta/enumeration");
	}

	@Test
	@Order(2)
	void getByName() {
		this.assertUrlReturns("getByName", "/meta/enumeration/PrimitiveType");
	}

	@Test
	@Order(3)
	void getByNameVerb() {
		this.assertUrlReturns("getByNameVerb", "/meta/enumeration/Verb");
	}

	@Test
	@Order(4)
	void getByNameMultiplicity() {
		this.assertUrlReturns("getByNameMultiplicity", "/meta/enumeration/Multiplicity");
	}

	@Test
	@Order(5)
	void deleteByName() {
		this.assertUrlDeletes("deleteByName", "/meta/enumeration/Verb");
	}

	@Test
	@Order(6)
	void getByNameAfterDelete() {
		this.assertUrlReturnsGone("getByNameAfterDelete", "/meta/enumeration/Verb");
	}
}
