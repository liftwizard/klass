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

package cool.klass.model.lens;

import javax.annotation.Nonnull;

/**
 * Lens for Long properties with unboxed primitive methods.
 *
 * @param <T> the domain object type
 */
public interface LongLens<T> extends PrimitiveLens<T, Long> {
	/**
	 * Gets the unboxed long value from the domain object.
	 *
	 * @param domainObject the domain object to read from
	 * @return the primitive long value
	 */
	long getLong(@Nonnull T domainObject);

	/**
	 * Sets the unboxed long value on the domain object.
	 *
	 * @param domainObject the domain object to write to
	 * @param value        the primitive long value to set
	 */
	void setLong(@Nonnull T domainObject, long value);
}
