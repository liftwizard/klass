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

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.MutableSet;

public final class ProjectionWalker {

	private ProjectionWalker() {
		throw new AssertionError("Suppress default constructor for noninstantiability");
	}

	public static void walk(@Nonnull ProjectionElement projectionElement, @Nonnull ProjectionListener listener) {
		ProjectionWalker.recursiveWalk(projectionElement, listener, Sets.mutable.empty());
	}

	private static void recursiveWalk(
		@Nonnull ProjectionElement projectionElement,
		@Nonnull ProjectionListener listener,
		@Nonnull MutableSet<ProjectionElement> visited
	) {
		projectionElement.enter(listener);
		projectionElement
			.getChildren()
			.forEach((eachChild) -> {
				boolean notYetVisited = visited.add(eachChild);
				if (notYetVisited) {
					ProjectionWalker.recursiveWalk(eachChild, listener, visited);
				}
			});
		projectionElement.exit(listener);
	}
}
