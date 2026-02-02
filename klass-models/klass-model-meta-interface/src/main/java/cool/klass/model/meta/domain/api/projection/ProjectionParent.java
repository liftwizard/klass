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
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

public interface ProjectionParent extends ProjectionElement {
	@Nonnull
	Classifier getClassifier();

	default void visitChildren(ProjectionListener projectionListener) {
		for (ProjectionElement projectionElement : this.getChildren()) {
			projectionElement.visit(projectionListener);
		}
	}

	default ImmutableList<ProjectionReferenceProperty> getReferencePropertyChildren() {
		return this.getChildren().selectInstancesOf(ProjectionReferenceProperty.class);
	}

	default ImmutableList<ReferenceProperty> getReferenceProperties() {
		return this.getReferencePropertyChildren().collect(ProjectionReferenceProperty::getProperty);
	}

	default ImmutableList<AssociationEnd> getAssociationEndsOutsideProjection() {
		ImmutableList<ReferenceProperty> referencePropertiesInProjection = this.getReferenceProperties();

		ImmutableList<AssociationEnd> optionalReturnPath = Lists.immutable
			.with(this)
			.selectInstancesOf(ProjectionReferenceProperty.class)
			.collect(ProjectionReferenceProperty::getProperty)
			.selectInstancesOf(AssociationEnd.class)
			.collect(AssociationEnd::getOpposite);

		if (this.getClassifier() instanceof Klass) {
			return ((Klass) this.getClassifier()).getAssociationEnds()
				.reject(referencePropertiesInProjection::contains)
				.reject(optionalReturnPath::contains);
		}

		throw new AssertionError(this.getClassifier());
	}

	default boolean hasPolymorphicChildren() {
		return this.getChildren()
			.asLazy()
			.selectInstancesOf(ProjectionChild.class)
			.anySatisfy(ProjectionChild::isPolymorphic);
	}
}
