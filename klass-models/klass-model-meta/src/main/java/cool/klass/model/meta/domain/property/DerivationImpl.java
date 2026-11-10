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

package cool.klass.model.meta.domain.property;

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.Derivation;

public final class DerivationImpl
	implements Derivation
{
	@Nonnull
	private final DataTypeProperty owningProperty;

	@Nonnull
	private final String associationEndName;

	public DerivationImpl(@Nonnull DataTypeProperty owningProperty, @Nonnull String associationEndName)
	{
		this.owningProperty = Objects.requireNonNull(owningProperty);
		this.associationEndName = Objects.requireNonNull(associationEndName);
	}

	@Nonnull
	@Override
	public DataTypeProperty getOwningProperty()
	{
		return this.owningProperty;
	}

	// Association ends are built after data type properties, so the association end is looked up on demand.
	@Nonnull
	@Override
	public AssociationEnd getAssociationEnd()
	{
		Klass owningClass = (Klass) this.owningProperty.getOwningClassifier();
		return owningClass.getAssociationEndByName(this.associationEndName);
	}

	@Override
	public String toString()
	{
		return "count(this." + this.associationEndName + ")";
	}
}
