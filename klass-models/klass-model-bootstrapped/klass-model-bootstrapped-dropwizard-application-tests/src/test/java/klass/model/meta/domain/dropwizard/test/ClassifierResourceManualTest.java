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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
class ClassifierResourceManualTest extends AbstractResourceTestCase {

	@Test
	@Order(1)
	void getAllMeta() {
		this.assertUrlReturns("getAllMeta", "/meta/classifier");
	}

	@Test
	@Order(2)
	void getByName() {
		this.assertUrlReturns("getByName", "/meta/classifier/Klass");
	}

	@Test
	@Order(3)
	void getByNameInterface() {
		this.assertUrlReturns("getByNameInterface", "/meta/classifier/NamedElement");
	}

	@Test
	@Order(4)
	@Disabled("TODO: DELETE for Classifier 'NamedElement' returns 500 due to dependent entities")
	void deleteByName() {
		this.assertUrlDeletes("deleteByName", "/meta/classifier/NamedElement");
	}

	@Test
	@Order(5)
	@Disabled("TODO: DELETE for Classifier 'NamedElement' returns 500 due to dependent entities")
	void getByNameAfterDelete() {
		this.assertUrlReturnsGone("getByNameAfterDelete", "/meta/classifier/NamedElement");
	}
}
