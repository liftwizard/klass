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

package cool.klass.reladomo.tree.serializer;

import com.gs.fw.common.mithra.finder.AbstractRelatedFinder;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.stack.mutable.ArrayStack;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StackStateTest
{
	@Test
	void comparesElementsByIdentity()
	{
		MutableStack<Object> contextStack = new ArrayStack<>();
		MutableStack<AbstractRelatedFinder> finderStack = new ArrayStack<>();
		MutableStack<Object> persistentInstanceStack = new ArrayStack<>();
		MutableStack<Object> resultNodeStack = new ArrayStack<>();

		var persistentInstance = new UncomparableElement();
		MutableList<Object> resultNode = Lists.mutable.with("result");
		persistentInstanceStack.push(persistentInstance);
		resultNodeStack.push(resultNode);

		StackState before = snapshot(contextStack, finderStack, persistentInstanceStack, resultNodeStack);
		StackState balanced = snapshot(contextStack, finderStack, persistentInstanceStack, resultNodeStack);
		assertThat(before.equals(balanced)).isTrue();
		assertThat(before.hashCode()).isEqualTo(balanced.hashCode());

		resultNodeStack.pop();
		resultNodeStack.push(Lists.mutable.with("result"));
		StackState replaced = snapshot(contextStack, finderStack, persistentInstanceStack, resultNodeStack);
		assertThat(before.equals(replaced)).isFalse();

		resultNodeStack.push(resultNode);
		StackState unbalanced = snapshot(contextStack, finderStack, persistentInstanceStack, resultNodeStack);
		assertThat(before.equals(unbalanced)).isFalse();
	}

	private static StackState snapshot(
		MutableStack<Object> contextStack,
		MutableStack<AbstractRelatedFinder> finderStack,
		MutableStack<Object> persistentInstanceStack,
		MutableStack<Object> resultNodeStack
	)
	{
		return new StackState(
			contextStack.toImmutable(),
			finderStack.toImmutable(),
			persistentInstanceStack.toImmutable(),
			resultNodeStack.toImmutable()
		);
	}

	/**
	 * Stands in for a lazily resolved Reladomo list, whose equals and hashCode run queries and walk every element.
	 */
	private static final class UncomparableElement
	{
		@Override
		public boolean equals(Object other)
		{
			throw new AssertionError("equals must not be called");
		}

		@Override
		public int hashCode()
		{
			throw new AssertionError("hashCode must not be called");
		}
	}
}
