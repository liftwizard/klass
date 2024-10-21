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

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.stack.MutableStack;

public class ContextStack
{
    @Nonnull
    private final MutableStack<ContextNode> delegate = Stacks.mutable.empty();

    @Nonnull
    private final MutableList<String> errors;
    @Nullable
    private final MutableList<String> warnings;

    public ContextStack(@Nonnull MutableList<String> errors, @Nullable MutableList<String> warnings)
    {
        this.errors = Objects.requireNonNull(errors);
        this.warnings = warnings;
    }

    public void push(@Nonnull ContextNode contextNode)
    {
        Objects.requireNonNull(contextNode);
        this.delegate.push(contextNode);
    }

    public void pop()
    {
        this.delegate.pop();
    }

    public void runWithContext(@Nonnull ContextNode contextNode, @Nonnull Runnable runnable)
    {
        Objects.requireNonNull(contextNode);
        Objects.requireNonNull(runnable);

        this.delegate.push(contextNode);

        try
        {
            runnable.run();
        }
        finally
        {
            this.delegate.pop();
        }
    }

    public void addError(String message)
    {
        String error = String.format("Error at %s. %s", this, message);
        this.errors.add(error);
    }

    public void addWarning(String message)
    {
        String warning = String.format("Warning at %s. %s", this, message);
        this.warnings.add(warning);
    }

    @Override
    public String toString()
    {
        return this.delegate
                .toList()
                .asReversed()
                .makeString(".");
    }
}
