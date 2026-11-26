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

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.state.IAntlrElement;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;

public class CompilerAnnotationHolder
{
	private final MutableList<CompilerAnnotation> compilerAnnotations = Lists.mutable.empty();

	/**
	 * {@return a builder for an error on the element, added by its {@link CompilerAnnotationBuilder#add()}}
	 */
	@Nonnull
	@CheckReturnValue
	public CompilerAnnotationBuilder error(
		@Nonnull String annotationCode,
		@Nonnull String message,
		@Nonnull IAntlrElement element
	)
	{
		return this.report(AnnotationSeverity.ERROR, annotationCode, message, element);
	}

	/**
	 * {@return a builder for a warning on the element, added by its {@link CompilerAnnotationBuilder#add()}}
	 */
	@Nonnull
	@CheckReturnValue
	public CompilerAnnotationBuilder warning(
		@Nonnull String annotationCode,
		@Nonnull String message,
		@Nonnull IAntlrElement element
	)
	{
		return this.report(AnnotationSeverity.WARNING, annotationCode, message, element);
	}

	/**
	 * {@return a builder for an annotation whose severity the caller decides at runtime, added by its {@link CompilerAnnotationBuilder#add()}}
	 */
	@Nonnull
	@CheckReturnValue
	public CompilerAnnotationBuilder report(
		@Nonnull AnnotationSeverity severity,
		@Nonnull String annotationCode,
		@Nonnull String message,
		@Nonnull IAntlrElement element
	)
	{
		return new CompilerAnnotationBuilder(this, severity, annotationCode, message, element);
	}

	void add(@Nonnull CompilerAnnotation compilerAnnotation)
	{
		this.compilerAnnotations.add(Objects.requireNonNull(compilerAnnotation));
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
