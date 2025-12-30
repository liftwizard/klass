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

package cool.klass.model.reladomo.projection;

import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Type;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;

public interface ProjectionElementReladomoNode {
	String getName();

	Classifier getOwningClassifier();

	Type getType();

	RootReladomoNode getRootReladomoNode();

	MutableMap<String, ProjectionElementReladomoNode> getChildren();

	ProjectionElementReladomoNode computeChild(String name, ProjectionElementReladomoNode node);

	void setProjection(RootReladomoNode rootReladomoNode);

	default String getShortString() {
		return '.' + this.getName() + "()";
	}

	default String getNodeString() {
		String suffix = this.getRootReladomoNode() == null
			? ""
			: " <- " + this.getRootReladomoNode().getProjection().getName();
		return this.getOwningClassifier().getName() + this.getShortString() + ": " + this.getType().getName() + suffix;
	}

	default boolean isLeaf() {
		return this.getChildren().allSatisfy(ProjectionDataTypePropertyReladomoNode.class::isInstance);
	}

	default ImmutableList<String> getDeepFetchStrings() {
		if (this.isLeaf()) {
			return Lists.immutable.empty();
		}
		MutableList<String> result = Lists.mutable.empty();
		MutableStack<String> stack = Stacks.mutable.empty();
		this.getDeepFetchStrings(result, stack);
		return result.toImmutable();
	}

	private void getDeepFetchStrings(MutableList<String> result, MutableStack<String> stack) {
		if (this instanceof ProjectionDataTypePropertyReladomoNode) {
			if (stack.size() > 1) {
				String string = stack.toList().asReversed().makeString("");
				result.add(string);
			}
			return;
		}

		stack.push(this.getShortString());
		if (this.isLeaf() || this.getRootReladomoNode() != null) {
			String string = stack.toList().asReversed().makeString("");
			result.add(string);
		} else {
			for (ProjectionElementReladomoNode child : this.getChildren()) {
				child.getDeepFetchStrings(result, stack);
			}
		}
		stack.pop();
	}

	default boolean hasPolymorphicChildren() {
		return this.getChildren().anySatisfy(SubClassReladomoNode.class::isInstance);
	}

	default String toString(String indent) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(indent);
		stringBuilder.append(this.getNodeString());
		stringBuilder.append('\n');

		String childIndent = indent + "  ";
		this.toStringChildren(stringBuilder, childIndent);

		return stringBuilder.toString();
	}

	default void toStringChildren(StringBuilder result, String childIndent) {
		if (this.getRootReladomoNode() != null) {
			return;
		}
		this.getChildren()
			.valuesView()
			.collectWith(ProjectionElementReladomoNode::toString, childIndent)
			.each(result::append);
	}
}
