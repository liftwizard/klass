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

package cool.klass.model.meta.domain.api.projection;

public class BaseProjectionListener implements ProjectionListener {

	@Override
	public void enterProjection(Projection projection) {}

	@Override
	public void exitProjection(Projection projection) {}

	@Override
	public void enterProjectionReferenceProperty(ProjectionReferenceProperty projectionReferenceProperty) {}

	@Override
	public void exitProjectionReferenceProperty(ProjectionReferenceProperty projectionReferenceProperty) {}

	@Override
	public void enterProjectionProjectionReference(ProjectionProjectionReference projectionProjectionReference) {}

	@Override
	public void exitProjectionProjectionReference(ProjectionProjectionReference projectionProjectionReference) {}

	@Override
	public void enterProjectionDataTypeProperty(ProjectionDataTypeProperty projectionDataTypeProperty) {}

	@Override
	public void exitProjectionDataTypeProperty(ProjectionDataTypeProperty projectionDataTypeProperty) {}
}
