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

import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;

public interface ClassLens<T> {
	@Nonnull
	Klass getKlass();

	@Nonnull
	PropertyLens<T, ?> getLensByProperty(@Nonnull Property property);

	@Nonnull
	DataTypeLens<T, ?> getLensByProperty(@Nonnull DataTypeProperty property);

	@Nonnull
	PrimitiveLens<T, ?> getLensByProperty(@Nonnull PrimitiveProperty property);

	@Nonnull
	EnumerationLens<T> getLensByProperty(@Nonnull EnumerationProperty property);

	@Nonnull
	ReferenceLens<T, ?> getLensByProperty(@Nonnull ReferenceProperty property);

	@Nonnull
	AssociationLens<T, ?> getLensByProperty(@Nonnull AssociationEnd property);
}
