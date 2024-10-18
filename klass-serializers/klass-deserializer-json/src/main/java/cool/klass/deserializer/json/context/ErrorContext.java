/*
 * Copyright 2024 Craig Motlin
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

package cool.klass.deserializer.json.context;

import javax.annotation.Nonnull;

import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.stack.MutableStack;

public class ErrorContext
{
    @Nonnull
    private final MutableStack<String> contextStack;

    public ErrorContext()
    {
        this(Stacks.mutable.empty());
    }

    public ErrorContext(@Nonnull MutableStack<String> contextStack)
    {
        this.contextStack = contextStack;
    }

    public void push(String contextString)
    {
        this.contextStack.push(contextString);
    }

    public void pop()
    {
        this.contextStack.pop();
    }

    @Override
    public String toString()
    {
        return this.contextStack
                .toList()
                .asReversed()
                .makeString(".");
    }
}
