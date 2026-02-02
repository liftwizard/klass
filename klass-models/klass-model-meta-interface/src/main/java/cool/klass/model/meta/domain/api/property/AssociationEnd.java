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

package cool.klass.model.meta.domain.api.property;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Association;
import cool.klass.model.meta.domain.api.Klass;

public interface AssociationEnd extends ReferenceProperty {
	@Nonnull
	@Override
	Klass getType();

	@Nonnull
	@Override
	Klass getOwningClassifier();

	@Override
	default void visit(@Nonnull PropertyVisitor visitor) {
		visitor.visitAssociationEnd(this);
	}

	@Nonnull
	default AssociationEnd getOpposite() {
		Association association = this.getOwningAssociation();

		if (this == association.getSourceAssociationEnd()) {
			return association.getTargetAssociationEnd();
		}

		if (this == association.getTargetAssociationEnd()) {
			return association.getSourceAssociationEnd();
		}

		throw new AssertionError();
	}

	@Nonnull
	Association getOwningAssociation();

	default boolean hasRealKeys() {
		return this.getType()
			.getKeyProperties()
			.anySatisfy(
				(keyProperty) ->
					!keyProperty.isForeignKeyWithOpposite() && !keyProperty.isForeignKeyMatchingKeyOnPath(this)
			);
	}

	default boolean isVersioned() {
		return this.getOpposite().isVersion();
	}
}
