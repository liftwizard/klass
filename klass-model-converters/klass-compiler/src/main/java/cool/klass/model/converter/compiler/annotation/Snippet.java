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

package cool.klass.model.converter.compiler.annotation;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;

/**
 * The source an annotation points at: the contexts to underline, the surrounding elements to show, and the compilation units they come from.
 * A snippet in macro-generated code derives its cause, the snippet of the macro element that generated it, from its compilation unit.
 */
record Snippet(
	@Nonnull SnippetSource snippetSource,
	// TODO: Change type of offendingContexts to also be SourceContexts
	@Nonnull ImmutableList<ParserRuleContext> offendingContexts,
	@Nonnull ImmutableList<IAntlrElement> sourceContexts
)
{
	Snippet
	{
		Objects.requireNonNull(snippetSource);
		Objects.requireNonNull(offendingContexts);
		sourceContexts = Objects.requireNonNull(sourceContexts).select(IAntlrElement::isContext);

		if (offendingContexts.isEmpty())
		{
			throw new AssertionError();
		}
		if (offendingContexts.anySatisfy((offendingContext) -> offendingContext.getStart() == null))
		{
			throw new AssertionError();
		}
	}

	/**
	 * {@return the snippet that points at the macro element which generated this snippet's compilation unit}
	 */
	@Nonnull
	Optional<Snippet> getMacroCause()
	{
		return this.getCompilationUnit()
			.getMacroElement()
			.map((macroElement) ->
				new Snippet(
					new SingleFileSource(macroElement.getCompilationUnit().get()),
					Lists.immutable.with(macroElement.getElementContext()),
					macroElement.getSurroundingElements()
				)
			);
	}

	@Nonnull
	CompilationUnit getCompilationUnit()
	{
		return this.snippetSource.annotatedCompilationUnit();
	}

	int getLine()
	{
		return this.getOffendingToken().getLine();
	}

	int getCharPositionInLine()
	{
		return this.getOffendingToken().getCharPositionInLine();
	}

	private Token getOffendingToken()
	{
		return this.offendingContexts.getFirst().getStart();
	}

	/**
	 * {@return this snippet's line followed by the lines of its macro causes, innermost first}
	 */
	@Nonnull
	ImmutableList<Integer> getLines()
	{
		MutableList<Integer> result = Lists.mutable.empty();
		for (Optional<Snippet> each = Optional.of(this); each.isPresent(); each = each.get().getMacroCause())
		{
			result.add(each.get().getLine());
		}
		return result.toImmutable();
	}
}
