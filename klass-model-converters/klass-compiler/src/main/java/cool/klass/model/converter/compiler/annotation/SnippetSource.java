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

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.SetIterable;

/**
 * The compilation units a compiler annotation renders its source snippet from, chosen explicitly where the kind of error is known.
 */
public sealed interface SnippetSource permits SingleFileSource, GraftedSnippetSource
{
	/**
	 * {@return the compilation unit the annotation itself points into}
	 */
	@Nonnull
	CompilationUnit annotatedCompilationUnit();

	/**
	 * {@return the compilation unit whose token stream produced the token}
	 * @throws AssertionError if the token comes from none of this source's compilation units
	 */
	@Nonnull
	CompilationUnit getCompilationUnit(@Nonnull Token token);

	/**
	 * {@return the rendered lines containing the context tokens, in display order}
	 */
	@Nonnull
	ImmutableList<TokenLine> getTokenLines(@Nonnull SetIterable<Token> contextTokens);

	@Nonnull
	default CommonTokenStream getTokenStream(@Nonnull Token token)
	{
		return (CommonTokenStream) this.getCompilationUnit(token).getTokenStream();
	}
}
