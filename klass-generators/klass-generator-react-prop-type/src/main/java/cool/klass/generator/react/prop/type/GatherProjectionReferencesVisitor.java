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

package cool.klass.generator.react.prop.type;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.projection.Projection;
import cool.klass.model.meta.domain.api.projection.ProjectionDataTypeProperty;
import cool.klass.model.meta.domain.api.projection.ProjectionElement;
import cool.klass.model.meta.domain.api.projection.ProjectionProjectionReference;
import cool.klass.model.meta.domain.api.projection.ProjectionReferenceProperty;
import cool.klass.model.meta.domain.api.projection.ProjectionVisitor;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;

public class GatherProjectionReferencesVisitor implements ProjectionVisitor {

	private final Projection originalProjection;
	private final MutableSet<Projection> referencedProjections = Sets.mutable.empty();

	public GatherProjectionReferencesVisitor(Projection projection) {
		this.originalProjection = projection;
	}

	public ImmutableSet<Projection> getReferencedProjections() {
		return this.referencedProjections.toImmutable();
	}

	@Override
	public void visitProjection(@Nonnull Projection projection) {
		if (projection == this.originalProjection) {
			return;
		}

		boolean added = this.referencedProjections.add(projection);
		if (added) {
			projection.getChildren().forEachWith(ProjectionElement::visit, this);
		}
	}

	@Override
	public void visitProjectionReferenceProperty(@Nonnull ProjectionReferenceProperty projectionReferenceProperty) {
		projectionReferenceProperty.getChildren().forEachWith(ProjectionElement::visit, this);
	}

	@Override
	public void visitProjectionProjectionReference(
		@Nonnull ProjectionProjectionReference projectionProjectionReference
	) {
		projectionProjectionReference.getProjection().visit(this);
	}

	@Override
	public void visitProjectionDataTypeProperty(ProjectionDataTypeProperty projectionDataTypeProperty) {
		// Deliberately empty
	}
}
