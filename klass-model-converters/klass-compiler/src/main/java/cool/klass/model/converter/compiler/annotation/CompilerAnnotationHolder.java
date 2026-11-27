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

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.state.AntlrNamedElement;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;

public class CompilerAnnotationHolder
{
	private final MutableList<CompilerAnnotation> compilerAnnotations = Lists.mutable.empty();

	public void add(@Nonnull String annotationCode, @Nonnull String message, @Nonnull IAntlrElement element)
	{
		this.add(annotationCode, message, element, AnnotationSeverity.ERROR);
	}

	public void add(
		@Nonnull String annotationCode,
		@Nonnull String message,
		@Nonnull IAntlrElement element,
		@Nonnull AnnotationSeverity severity
	)
	{
		Objects.requireNonNull(element);
		if (element instanceof AntlrNamedElement namedElement)
		{
			this.add(annotationCode, message, element, namedElement.getNameContext(), severity);
		}
		else
		{
			this.add(annotationCode, message, element, element.getElementContext(), severity);
		}
	}

	public void add(
		@Nonnull String annotationCode,
		@Nonnull String message,
		@Nonnull IAntlrElement element,
		@Nonnull ParserRuleContext offendingContext
	)
	{
		this.add(annotationCode, message, element, offendingContext, AnnotationSeverity.ERROR);
	}

	public void add(
		@Nonnull String annotationCode,
		@Nonnull String message,
		@Nonnull IAntlrElement element,
		@Nonnull ParserRuleContext offendingContext,
		@Nonnull AnnotationSeverity severity
	)
	{
		this.add(annotationCode, message, element, Lists.immutable.with(offendingContext), severity);
	}

	public void add(
		@Nonnull String annotationCode,
		@Nonnull String message,
		@Nonnull IAntlrElement element,
		@Nonnull ImmutableList<ParserRuleContext> offendingContexts
	)
	{
		this.add(annotationCode, message, element, offendingContexts, AnnotationSeverity.ERROR);
	}

	public void add(
		@Nonnull String annotationCode,
		@Nonnull String message,
		@Nonnull IAntlrElement element,
		@Nonnull ImmutableList<ParserRuleContext> offendingContexts,
		@Nonnull AnnotationSeverity severity
	)
	{
		this.add(annotationCode, message, element, element.getSurroundingElements(), offendingContexts, severity);
	}

	public void add(
		@Nonnull String annotationCode,
		@Nonnull String message,
		@Nonnull IAntlrElement element,
		@Nonnull ImmutableList<IAntlrElement> surroundingElements,
		@Nonnull ImmutableList<ParserRuleContext> offendingContexts
	)
	{
		this.add(annotationCode, message, element, surroundingElements, offendingContexts, AnnotationSeverity.ERROR);
	}

	public void add(
		@Nonnull String annotationCode,
		@Nonnull String message,
		@Nonnull IAntlrElement element,
		@Nonnull ImmutableList<IAntlrElement> surroundingElements,
		@Nonnull ImmutableList<ParserRuleContext> offendingContexts,
		@Nonnull AnnotationSeverity severity
	)
	{
		SnippetSource snippetSource = getDefaultSnippetSource(element, surroundingElements);
		this.add(annotationCode, message, element, surroundingElements, offendingContexts, snippetSource, severity);
	}

	/**
	 * Adds an error whose snippet source the caller chose explicitly, because it knows the error spans a root compilation unit and an in-place macro.
	 */
	public void add(
		@Nonnull String annotationCode,
		@Nonnull String message,
		@Nonnull IAntlrElement element,
		@Nonnull ImmutableList<IAntlrElement> surroundingElements,
		@Nonnull ImmutableList<ParserRuleContext> offendingContexts,
		@Nonnull SnippetSource snippetSource
	)
	{
		this.add(
			annotationCode,
			message,
			element,
			surroundingElements,
			offendingContexts,
			snippetSource,
			AnnotationSeverity.ERROR
		);
	}

	private void add(
		@Nonnull String annotationCode,
		@Nonnull String message,
		@Nonnull IAntlrElement element,
		@Nonnull ImmutableList<IAntlrElement> surroundingElements,
		@Nonnull ImmutableList<ParserRuleContext> offendingContexts,
		@Nonnull SnippetSource snippetSource,
		@Nonnull AnnotationSeverity severity
	)
	{
		if (snippetSource.annotatedCompilationUnit() != element.getCompilationUnit().get())
		{
			throw new AssertionError("Expected " + snippetSource + " to annotate the compilation unit of " + element);
		}
		var snippet = new Snippet(snippetSource, offendingContexts, surroundingElements);
		this.compilerAnnotations.add(new CompilerAnnotation(annotationCode, message, severity, snippet));
	}

	/**
	 * {@return a single-file source, unless the element was generated by an in-place macro}
	 * A generic error reporter cannot know statically whether it points into macro code, so this checks whether the element's compilation unit is grafted into the root compilation unit of its surrounding elements.
	 */
	@Nonnull
	private static SnippetSource getDefaultSnippetSource(
		@Nonnull IAntlrElement element,
		@Nonnull ImmutableList<IAntlrElement> surroundingElements
	)
	{
		CompilationUnit compilationUnit = element.getCompilationUnit().get();
		if (!compilationUnit.isGrafted())
		{
			return new SingleFileSource(compilationUnit);
		}

		MutableList<CompilationUnit> rootCompilationUnits = IAntlrElement.getCompilationUnits(
			surroundingElements
		).reject(CompilationUnit::isGrafted);
		if (rootCompilationUnits.size() != 1)
		{
			throw new AssertionError(
				"Expected the surrounding elements of " +
					element +
					" to have one root compilation unit, but got " +
					rootCompilationUnits
			);
		}
		return new GraftedCodeSource(rootCompilationUnits.getOnly(), compilationUnit);
	}

	public ImmutableList<CompilerAnnotation> getCompilerAnnotations()
	{
		return this.compilerAnnotations.toSortedList().toImmutable();
	}

	public boolean hasCompilerAnnotations()
	{
		return this.compilerAnnotations.notEmpty();
	}
}
