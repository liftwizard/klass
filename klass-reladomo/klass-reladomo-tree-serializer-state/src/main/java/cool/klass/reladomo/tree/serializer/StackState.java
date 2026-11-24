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
import org.eclipse.collections.api.stack.ImmutableStack;
import org.eclipse.collections.api.stack.StackIterable;

/**
 * A snapshot of a tree serializer listener's stacks, used to assert that every enter was matched by an exit.
 *
 * <p>Equality compares stack elements by identity. A balanced walk leaves the very same objects on each stack, and the elements include persistent instances and lazily resolved Reladomo lists whose own equals and hashCode run queries and walk every element.
 */
public record StackState(
	ImmutableStack<Object> contextStack,
	ImmutableStack<AbstractRelatedFinder> finderStack,
	ImmutableStack<Object> persistentInstanceStack,
	ImmutableStack<Object> resultNodeStack
)
{
	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof StackState that))
		{
			return false;
		}
		return (
			sameElements(this.contextStack, that.contextStack) &&
			sameElements(this.finderStack, that.finderStack) &&
			sameElements(this.persistentInstanceStack, that.persistentInstanceStack) &&
			sameElements(this.resultNodeStack, that.resultNodeStack)
		);
	}

	@Override
	public int hashCode()
	{
		int result = identityHashCode(this.contextStack);
		result = 31 * result + identityHashCode(this.finderStack);
		result = 31 * result + identityHashCode(this.persistentInstanceStack);
		result = 31 * result + identityHashCode(this.resultNodeStack);
		return result;
	}

	private static <T> boolean sameElements(StackIterable<T> stack, StackIterable<T> otherStack)
	{
		// Eclipse Collections stacks throw UnsupportedOperationException from corresponds, so compare as lists.
		return stack.toList().corresponds(otherStack.toList(), (element, otherElement) -> element == otherElement);
	}

	private static int identityHashCode(StackIterable<?> stack)
	{
		return stack.injectInto(1, (int hash, Object element) -> 31 * hash + System.identityHashCode(element));
	}
}
