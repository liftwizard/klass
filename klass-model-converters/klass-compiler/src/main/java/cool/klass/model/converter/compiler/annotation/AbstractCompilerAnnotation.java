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
 * The data of a compiler annotation: where it points, which source it shows, and the macro that caused it.
 * Rendering it as text is the job of {@link CompilerAnnotationRenderer}.
 */
public abstract class AbstractCompilerAnnotation
{
	@Nonnull
	private final SnippetSource snippetSource;

	@Nonnull
	private final Optional<CauseCompilerAnnotation> macroCause;

	// TODO: Change type of offendingContexts to also be SourceContexts
	@Nonnull
	private final ImmutableList<ParserRuleContext> offendingContexts;

	@Nonnull
	private final ImmutableList<IAntlrElement> sourceContexts;

	@Nonnull
	private final AnnotationSeverity severity;

	protected AbstractCompilerAnnotation(
		@Nonnull SnippetSource snippetSource,
		@Nonnull Optional<CauseCompilerAnnotation> macroCause,
		@Nonnull ImmutableList<ParserRuleContext> offendingContexts,
		@Nonnull ImmutableList<IAntlrElement> sourceContexts,
		@Nonnull AnnotationSeverity severity
	)
	{
		this.snippetSource = Objects.requireNonNull(snippetSource);
		this.macroCause = Objects.requireNonNull(macroCause);
		this.offendingContexts = Objects.requireNonNull(offendingContexts);
		this.sourceContexts = Objects.requireNonNull(sourceContexts).select(IAntlrElement::isContext);
		this.severity = Objects.requireNonNull(severity);

		if (offendingContexts.isEmpty())
		{
			throw new AssertionError();
		}
		if (offendingContexts.anySatisfy((offendingContext) -> offendingContext.getStart() == null))
		{
			throw new AssertionError();
		}
	}

	@Nonnull
	protected CompilationUnit getCompilationUnit()
	{
		return this.snippetSource.annotatedCompilationUnit();
	}

	@Nonnull
	SnippetSource getSnippetSource()
	{
		return this.snippetSource;
	}

	@Nonnull
	Optional<CauseCompilerAnnotation> getMacroCause()
	{
		return this.macroCause;
	}

	@Nonnull
	ImmutableList<ParserRuleContext> getOffendingContexts()
	{
		return this.offendingContexts;
	}

	@Nonnull
	ImmutableList<IAntlrElement> getSourceContexts()
	{
		return this.sourceContexts;
	}

	public boolean isError()
	{
		return this.severity == AnnotationSeverity.ERROR;
	}

	public boolean isWarning()
	{
		return this.severity == AnnotationSeverity.WARNING;
	}

	@Nonnull
	public AnnotationSeverity getSeverity()
	{
		return this.severity;
	}

	@Nonnull
	protected String getFilenameWithoutDirectory()
	{
		String sourceName = this.getCompilationUnit().getSourceName();
		return this.macroCause
			.map((ignore) -> sourceName)
			.orElseGet(() -> sourceName.substring(sourceName.lastIndexOf('/') + 1));
	}

	protected int getLine()
	{
		return this.getOffendingToken().getLine();
	}

	protected int getCharPositionInLine()
	{
		return this.getOffendingToken().getCharPositionInLine();
	}

	private Token getOffendingToken()
	{
		return this.offendingContexts.getFirst().getStart();
	}

	public ImmutableList<Integer> getLines()
	{
		MutableList<Integer> result = Lists.mutable.empty();
		this.getLines(result);
		return result.toImmutable();
	}

	protected void getLines(MutableList<Integer> lines)
	{
		lines.add(this.getLine());
		this.macroCause.ifPresent((cause) -> cause.getLines(lines));
	}
}
