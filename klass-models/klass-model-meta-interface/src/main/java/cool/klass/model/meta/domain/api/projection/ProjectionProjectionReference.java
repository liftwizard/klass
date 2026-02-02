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
import org.eclipse.collections.api.list.ImmutableList;

public interface ProjectionProjectionReference extends ProjectionWithReferenceProperty {
	Projection getProjection();

	@Nonnull
	@Override
	default Classifier getClassifier() {
		return this.getProjection().getClassifier();
	}

	@Override
	default ImmutableList<? extends ProjectionChild> getChildren() {
		return this.getProjection().getChildren();
	}

	@Override
	default void visit(@Nonnull ProjectionVisitor visitor) {
		visitor.visitProjectionProjectionReference(this);
	}

	@Override
	default void enter(@Nonnull ProjectionListener listener) {
		listener.enterProjectionProjectionReference(this);
	}

	@Override
	default void exit(@Nonnull ProjectionListener listener) {
		listener.exitProjectionProjectionReference(this);
	}
}
