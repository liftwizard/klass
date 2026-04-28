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

package cool.klass.model.lens;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;

// Klass enumerations are NOT Java enums. They are stored as String (prettyName)
// in the database, but the public API uses EnumerationLiteral for type safety.
public interface EnumerationLens<T> extends DataTypeLens<T, EnumerationLiteral> {
	@Nonnull
	EnumerationProperty getEnumerationProperty();

	@Override
	@Nonnull
	default EnumerationProperty getProperty() {
		return this.getEnumerationProperty();
	}
}
