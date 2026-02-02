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

package cool.klass.dropwizard.configuration.sample.data;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

public class SampleDataFactory {

	private boolean enabled;

	private @Valid @NotNull Instant dataInstant = Instant.parse("1999-12-31T23:59:00Z");
	private @Valid @NotNull List<String> skippedPackages = Arrays.asList("klass.model.meta.domain");

	public boolean isEnabled() {
		return this.enabled;
	}

	@JsonProperty
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@JsonProperty
	public Instant getDataInstant() {
		return this.dataInstant;
	}

	@JsonProperty
	public void setDataInstant(Instant dataInstant) {
		this.dataInstant = dataInstant;
	}

	@JsonProperty
	public ImmutableList<String> getSkippedPackages() {
		return Lists.immutable.withAll(this.skippedPackages);
	}

	@JsonProperty
	public void setSkippedPackages(List<String> skippedPackages) {
		this.skippedPackages = skippedPackages;
	}
}
