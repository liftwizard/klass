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

import java.util.Optional;

import cool.klass.model.meta.domain.api.NamedElement;
import org.eclipse.collections.api.list.ImmutableList;

public interface ProjectionElement extends NamedElement {
	Optional<ProjectionParent> getParent();

	ImmutableList<? extends ProjectionChild> getChildren();

	default int getDepth() {
		return 1 + this.getParent().map(ProjectionElement::getDepth).orElse(0);
	}

	void enter(ProjectionListener listener);

	void exit(ProjectionListener listener);

	void visit(ProjectionVisitor visitor);

	default void visit(ProjectionListener listener) {
		try {
			this.enter(listener);
		} finally {
			this.exit(listener);
		}
	}
}
