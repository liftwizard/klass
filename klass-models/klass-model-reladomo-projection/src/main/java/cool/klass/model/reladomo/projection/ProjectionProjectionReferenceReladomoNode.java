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

package cool.klass.model.reladomo.projection;

import java.util.Objects;

import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.projection.ProjectionProjectionReference;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;

public class ProjectionProjectionReferenceReladomoNode extends ProjectionWithReferencePropertyReladomoNode {

	private final ProjectionProjectionReference projectionProjectionReference;

	public ProjectionProjectionReferenceReladomoNode(
		String name,
		ProjectionProjectionReference projectionProjectionReference
	) {
		super(name);
		this.projectionProjectionReference = Objects.requireNonNull(projectionProjectionReference);
	}

	@Override
	public Classifier getOwningClassifier() {
		return this.projectionProjectionReference.getProperty().getOwningClassifier();
	}

	@Override
	public Classifier getType() {
		return this.projectionProjectionReference.getProperty().getType();
	}

	@Override
	public ReferenceProperty getReferenceProperty() {
		return this.projectionProjectionReference.getProperty();
	}

	@Override
	public String getNodeString() {
		return super.getNodeString() + " -> " + this.projectionProjectionReference.getProjection().getName();
	}
}
