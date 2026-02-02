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

package cool.klass.model.meta.domain.api.projection;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;

public interface ProjectionWithReferenceProperty extends ProjectionParent, ProjectionChild {
	@Override
	@Nonnull
	ReferenceProperty getProperty();

	@Nonnull
	@Override
	default Classifier getClassifier() {
		return this.getProperty().getType();
	}

	default boolean isLeaf() {
		return this.getChildren().asLazy().selectInstancesOf(ProjectionWithReferenceProperty.class).isEmpty();
	}
}
