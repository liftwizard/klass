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

package cool.klass.model.reladomo.tree;

import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Type;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.stack.MutableStack;

public interface ReladomoTreeNode {
	void visit(ReladomoTreeNodeVisitor visitor);

	default void walk(ReladomoTreeNodeListener listener) {
		this.visit(new ReladomoTreeNodeWalkerVisitor(listener));
	}

	default void toManyAwareWalk(ReladomoTreeNodeToManyAwareListener listener) {
		this.visit(new ReladomoTreeNodeToManyAwareWalkerVisitor(listener));
	}

	String getName();

	Classifier getOwningClassifier();

	Type getType();

	MapIterable<String, ReladomoTreeNode> getChildren();

	ReladomoTreeNode computeChild(String childName, ReladomoTreeNode childNode);

	default String getShortString() {
		return '.' + this.getName();
	}

	default String getNodeString(String indent) {
		return (
			indent
			+ this.getOwningClassifier().getName()
			+ this.getShortString()
			+ ": "
			+ this.getType().getName()
			+ "\n"
		);
	}

	default boolean isLeaf() {
		return this.getChildren().isEmpty();
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
		stack.push(this.getShortString());
		if (this.isLeaf()) {
			String string = stack.toList().asReversed().makeString("");
			result.add(string);
		} else {
			for (ReladomoTreeNode child : this.getChildren()) {
				child.getDeepFetchStrings(result, stack);
			}
		}
		stack.pop();
	}

	String toString(String indent);
}
