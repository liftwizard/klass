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

package klass.model.meta.domain;

import com.gs.fw.common.mithra.MithraBusinessException;

public class RootProjection extends RootProjectionAbstract {

	public RootProjection() {
		// You must not modify this constructor. Mithra calls this internally.
		// You can call this constructor. You can also add new constructors.
	}

	// RootProjection extends ProjectionElement (joined table inheritance).
	// The generated cascadeDeleteImpl does not cascade through the ProjectionElement tree
	// (children, subclass rows). We handle it manually.
	@Override
	protected void cascadeDeleteImpl() throws MithraBusinessException {
		ProjectionElement projectionElement = this.getProjectionElementSuperClass();
		if (projectionElement != null) {
			projectionElement.getChildren().cascadeDeleteAll();
		}
		this.delete();
		if (projectionElement != null) {
			projectionElement.delete();
		}
	}
}
