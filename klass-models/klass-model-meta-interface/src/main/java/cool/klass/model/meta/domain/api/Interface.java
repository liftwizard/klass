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

package cool.klass.model.meta.domain.api;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.property.Property;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

public interface Interface extends Classifier {
	@Override
	default void visit(TopLevelElementVisitor visitor) {
		visitor.visitInterface(this);
	}

	@Override
	default boolean isAbstract() {
		return true;
	}

	// TODO: Replace with an implementation that preserves order
	@Override
	@Nonnull
	default ImmutableList<Property> getProperties() {
		return Lists.immutable.<Property>empty().newWithAll(this.getDataTypeProperties());
	}

	@Override
	default ImmutableList<Property> getDeclaredProperties() {
		return Lists.immutable.<Property>empty().newWithAll(this.getDeclaredDataTypeProperties());
	}

	@Override
	default boolean isUniquelyOwned() {
		return false;
	}
}
