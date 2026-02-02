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

package cool.klass.generator.react.prop.type;

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.projection.Projection;
import cool.klass.model.meta.domain.api.projection.ProjectionDataTypeProperty;
import cool.klass.model.meta.domain.api.projection.ProjectionElement;
import cool.klass.model.meta.domain.api.projection.ProjectionListener;
import cool.klass.model.meta.domain.api.projection.ProjectionProjectionReference;
import cool.klass.model.meta.domain.api.projection.ProjectionReferenceProperty;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.stack.MutableStack;

public class GatherProjectionSelfReferencesVisitor implements ProjectionListener {

	private final MutableStack<String> context = Stacks.mutable.empty();
	private final MutableList<String> results = Lists.mutable.empty();
	private final Projection originalProjection;

	public GatherProjectionSelfReferencesVisitor(Projection originalProjection) {
		this.originalProjection = Objects.requireNonNull(originalProjection);
	}

	public ImmutableList<String> getResults() {
		return this.results.toImmutable();
	}

	@Override
	public void enterProjection(Projection projection) {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".enterProjection() not implemented yet"
		);
	}

	@Override
	public void exitProjection(Projection projection) {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".exitProjection() not implemented yet"
		);
	}

	@Override
	public void enterProjectionReferenceProperty(@Nonnull ProjectionReferenceProperty projectionReferenceProperty) {
		this.context.push(projectionReferenceProperty.getName());
		projectionReferenceProperty.getChildren().forEachWith(ProjectionElement::visit, this);
	}

	@Override
	public void exitProjectionReferenceProperty(ProjectionReferenceProperty projectionReferenceProperty) {
		this.context.pop();
	}

	@Override
	public void enterProjectionProjectionReference(
		@Nonnull ProjectionProjectionReference projectionProjectionReference
	) {
		this.context.push(projectionProjectionReference.getName());
		if (projectionProjectionReference.getProjection() != this.originalProjection) {
			return;
		}

		ReferenceProperty referenceProperty = projectionProjectionReference.getProperty();
		Multiplicity multiplicity = referenceProperty.getMultiplicity();
		String isRequiredSuffix = multiplicity.isRequired() || multiplicity.isToMany() ? ".isRequired" : "";
		String toOnePropType = this.originalProjection.getName() + isRequiredSuffix;
		String propType = multiplicity.isToMany()
			? "PropTypes.arrayOf(" + toOnePropType + ").isRequired"
			: toOnePropType;

		String result = String.format(
			"%s.%s = %s;%n",
			this.originalProjection.getName(),
			this.context.makeString("."),
			propType
		);

		this.results.add(result);
	}

	@Override
	public void exitProjectionProjectionReference(ProjectionProjectionReference projectionProjectionReference) {
		this.context.pop();
	}

	@Override
	public void enterProjectionDataTypeProperty(ProjectionDataTypeProperty projectionDataTypeProperty) {
		// Deliberately empty
	}

	@Override
	public void exitProjectionDataTypeProperty(ProjectionDataTypeProperty projectionDataTypeProperty) {
		// Deliberately empty
	}
}
