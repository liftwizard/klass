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

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.state.IAntlrElement;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;

public class RootCompilerAnnotation
	extends AbstractCompilerAnnotation
	implements Comparable<RootCompilerAnnotation>
{
	private static final Comparator<RootCompilerAnnotation> COMPILER_ANNOTATION_COMPARATOR = Comparator.comparing(
		RootCompilerAnnotation::getSeverity
	)
		.thenComparingInt((each) -> each.getCompilationUnit().getOrdinal())
		.thenComparingInt(RootCompilerAnnotation::getLine)
		.thenComparingInt(RootCompilerAnnotation::getCharPositionInLine);

	@Nonnull
	private final String annotationCode;

	@Nonnull
	private final String message;

	public RootCompilerAnnotation(
		@Nonnull SnippetSource snippetSource,
		@Nonnull Optional<CauseCompilerAnnotation> macroCause,
		@Nonnull ImmutableList<ParserRuleContext> offendingContexts,
		@Nonnull ImmutableList<IAntlrElement> sourceContexts,
		@Nonnull String annotationCode,
		@Nonnull String message,
		@Nonnull AnnotationSeverity severity
	)
	{
		super(snippetSource, macroCause, offendingContexts, sourceContexts, severity);
		this.annotationCode = Objects.requireNonNull(annotationCode);
		this.message = Objects.requireNonNull(message);
	}

	@Nonnull
	public String getAnnotationCode()
	{
		return this.annotationCode;
	}

	@Nonnull
	public String getMessage()
	{
		return this.message;
	}

	@Override
	public int compareTo(@Nonnull RootCompilerAnnotation other)
	{
		return COMPILER_ANNOTATION_COMPARATOR.compare(this, other);
	}

	/**
	 * {@return a one-line summary for debugging; use {@link CompilerAnnotationRenderer} for the full message}
	 */
	@Nonnull
	@Override
	public String toString()
	{
		return "%s %s: %s (%s:%d:%d)".formatted(
			this.annotationCode,
			this.getSeverity(),
			this.message,
			this.getCompilationUnit().getFilenameWithoutDirectory(),
			this.getLine(),
			this.getCharPositionInLine() + 1
		);
	}
}
