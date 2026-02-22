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

import org.eclipse.collections.api.list.ImmutableList;

/**
 * Lens for to-many association ends (multiplicity 0..* or 1..*).
 * The get() method returns an ImmutableList of the associated objects.
 *
 * <p>Two type parameters are used to preserve both:
 * <ul>
 *     <li>E = element type (e.g., Order)</li>
 *     <li>The full ImmutableList&lt;E&gt; as V for compatibility with the parent interface</li>
 * </ul>
 *
 * @param <T> the domain object type
 * @param <E> the element type in the collection
 */
public interface ToManyLens<T, E> extends AssociationLens<T, ImmutableList<E>> {}
