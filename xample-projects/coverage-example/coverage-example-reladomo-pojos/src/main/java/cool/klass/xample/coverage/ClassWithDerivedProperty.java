/*
 * Copyright 2025 Craig Motlin
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

package cool.klass.xample.coverage;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class ClassWithDerivedProperty extends ClassWithDerivedPropertyAbstract {

	public ClassWithDerivedProperty() {
		// You must not modify this constructor. Mithra calls this internally.
		// You can call this constructor. You can also add new constructors.
	}

	public String getDerivedRequiredString() {
		return "derivedRequiredString";
	}

	public Integer getDerivedRequiredInteger() {
		return 1;
	}

	public Long getDerivedRequiredLong() {
		return 1L;
	}

	public Double getDerivedRequiredDouble() {
		return 1.0;
	}

	public Float getDerivedRequiredFloat() {
		return 1.0f;
	}

	public Boolean isDerivedRequiredBoolean() {
		return true;
	}

	public Instant getDerivedRequiredInstant() {
		return Instant.now();
	}

	public LocalDate getDerivedRequiredLocalDate() {
		return LocalDate.now(ZoneId.of("UTC"));
	}

	public String getDerivedOptionalString() {
		return "derivedOptionalString";
	}

	public Integer getDerivedOptionalInteger() {
		return 1;
	}

	public Long getDerivedOptionalLong() {
		return 1L;
	}

	public Double getDerivedOptionalDouble() {
		return 1.0;
	}

	public Float getDerivedOptionalFloat() {
		return 1.0f;
	}

	public Boolean isDerivedOptionalBoolean() {
		return true;
	}

	public Instant getDerivedOptionalInstant() {
		return Instant.now();
	}

	public LocalDate getDerivedOptionalLocalDate() {
		return LocalDate.now(ZoneId.of("UTC"));
	}
}
