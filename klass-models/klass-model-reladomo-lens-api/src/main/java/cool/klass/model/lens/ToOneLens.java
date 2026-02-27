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

/**
 * Lens for to-one association ends (multiplicity 0..1 or 1).
 * The get() and set() methods work with a single object.
 *
 * @param <T> the domain object type
 * @param <V> the associated domain object type
 */
public interface ToOneLens<T, V> extends AssociationLens<T, V> {}
