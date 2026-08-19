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

package cool.klass.reladomo.utc.infinity.timestamp;

import java.time.Instant;

import com.gs.fw.common.mithra.util.DefaultInfinityTimestamp;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UtcInfinityTimestampTest {

	@Test
	void separatesReladomoInfinityFromItsExternalInstant() {
		assertThat(UtcInfinityTimestamp.getDefaultInfinity()).isEqualTo(DefaultInfinityTimestamp.getDefaultInfinity());
		assertThat(UtcInfinityTimestamp.getDefaultInfinityInstant()).isEqualTo(Instant.parse("9999-12-01T23:59:00Z"));
	}
}
