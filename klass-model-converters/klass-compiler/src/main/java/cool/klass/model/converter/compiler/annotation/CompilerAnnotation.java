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

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * A compiler error or warning: its code, message, and severity, and the snippet of source it points at.
 * Rendering it as text is the job of {@link CompilerAnnotationRenderer}.
 */
public class CompilerAnnotation
	implements Comparable<CompilerAnnotation>
{
	private static final Comparator<CompilerAnnotation> COMPILER_ANNOTATION_COMPARATOR = Comparator.comparing(
		CompilerAnnotation::getSeverity
	)
		.thenComparingInt((each) -> each.getCompilationUnit().getOrdinal())
		.thenComparingInt(CompilerAnnotation::getLine)
		.thenComparingInt(CompilerAnnotation::getCharPositionInLine);

	@Nonnull
	private final String annotationCode;

	@Nonnull
	private final String message;

	@Nonnull
	private final AnnotationSeverity severity;

	@Nonnull
	private final Snippet snippet;

	CompilerAnnotation(
		@Nonnull String annotationCode,
		@Nonnull String message,
		@Nonnull AnnotationSeverity severity,
		@Nonnull Snippet snippet
	)
	{
		this.annotationCode = Objects.requireNonNull(annotationCode);
		this.message = Objects.requireNonNull(message);
		this.severity = Objects.requireNonNull(severity);
		this.snippet = Objects.requireNonNull(snippet);
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

	@Nonnull
	public AnnotationSeverity getSeverity()
	{
		return this.severity;
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
	Snippet getSnippet()
	{
		return this.snippet;
	}

	@Nonnull
	protected CompilationUnit getCompilationUnit()
	{
		return this.snippet.getCompilationUnit();
	}

	protected int getLine()
	{
		return this.snippet.getLine();
	}

	protected int getCharPositionInLine()
	{
		return this.snippet.getCharPositionInLine();
	}

	/**
	 * {@return the annotation's line followed by the lines of the macros that generated its code, innermost first}
	 */
	@Nonnull
	public ImmutableList<Integer> getLines()
	{
		return this.snippet.getLines();
	}

	@Override
	public int compareTo(@Nonnull CompilerAnnotation other)
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
			this.severity,
			this.message,
			this.getCompilationUnit().getFilenameWithoutDirectory(),
			this.getLine(),
			this.getCharPositionInLine() + 1
		);
	}
}
