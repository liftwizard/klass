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

package cool.klass.reladomo.csv.test.extension;

import java.util.List;

import com.gs.fw.common.mithra.attribute.Attribute;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class CsvTestDataParserTest {

	@RegisterExtension
	private final LogMarkerTestExtension logMarkerTestExtension = new LogMarkerTestExtension();

	@Test
	void extractClassNameFromFilename() {
		CsvTestDataParser parser = new CsvTestDataParser("test-data/cool.klass.xample.coverage.User.csv");
		assertThat(parser.getClassName()).isEqualTo("cool.klass.xample.coverage.User");
		assertThat(parser.getDataObjects()).hasSize(2);
	}

	@Test
	void parsesAttributesFromHeaders() {
		CsvTestDataParser parser = new CsvTestDataParser("test-data/cool.klass.xample.coverage.User.csv");
		List<Attribute<?, ?>> attributes = parser.getAttributes();

		assertThat(attributes).hasSize(6);
		assertThat(attributes.get(0).getAttributeName()).isEqualTo("systemFrom");
		assertThat(attributes.get(1).getAttributeName()).isEqualTo("systemTo");
		assertThat(attributes.get(2).getAttributeName()).isEqualTo("userId");
		assertThat(attributes.get(3).getAttributeName()).isEqualTo("firstName");
		assertThat(attributes.get(4).getAttributeName()).isEqualTo("lastName");
		assertThat(attributes.get(5).getAttributeName()).isEqualTo("email");
	}
}
