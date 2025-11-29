/*
 * Copyright 2025 Craig Motlin
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
import cool.klass.model.converter.compiler.state.AntlrNamedElement;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.AnsiTokenColorizer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

public class CompilerAnnotationHolder {

    private final MutableList<RootCompilerAnnotation> compilerAnnotations = Lists.mutable.empty();

    private AnsiTokenColorizer ansiTokenColorizer;

    public void add(@Nonnull String annotationCode, @Nonnull String message, @Nonnull IAntlrElement element) {
        this.add(annotationCode, message, element, AnnotationSeverity.ERROR);
    }

    public void add(
        @Nonnull String annotationCode,
        @Nonnull String message,
        @Nonnull IAntlrElement element,
        @Nonnull AnnotationSeverity severity
    ) {
        Objects.requireNonNull(element);
        if (element instanceof AntlrNamedElement namedElement) {
            this.add(annotationCode, message, element, namedElement.getNameContext(), severity);
        } else {
            this.add(annotationCode, message, element, element.getElementContext(), severity);
        }
    }

    public void add(
        @Nonnull String annotationCode,
        @Nonnull String message,
        @Nonnull IAntlrElement element,
        @Nonnull ParserRuleContext offendingContext
    ) {
        this.add(annotationCode, message, element, offendingContext, AnnotationSeverity.ERROR);
    }

    public void add(
        @Nonnull String annotationCode,
        @Nonnull String message,
        @Nonnull IAntlrElement element,
        @Nonnull ParserRuleContext offendingContext,
        @Nonnull AnnotationSeverity severity
    ) {
        this.add(annotationCode, message, element, Lists.immutable.with(offendingContext), severity);
    }

    public void add(
        @Nonnull String annotationCode,
        @Nonnull String message,
        @Nonnull IAntlrElement element,
        @Nonnull ImmutableList<ParserRuleContext> offendingContexts
    ) {
        this.add(annotationCode, message, element, offendingContexts, AnnotationSeverity.ERROR);
    }

    public void add(
        @Nonnull String annotationCode,
        @Nonnull String message,
        @Nonnull IAntlrElement element,
        @Nonnull ImmutableList<ParserRuleContext> offendingContexts,
        @Nonnull AnnotationSeverity severity
    ) {
        this.add(annotationCode, message, element, element.getSurroundingElements(), offendingContexts, severity);
    }

    public void add(
        @Nonnull String annotationCode,
        @Nonnull String message,
        @Nonnull IAntlrElement element,
        @Nonnull ImmutableList<IAntlrElement> surroundingElements,
        @Nonnull ImmutableList<ParserRuleContext> offendingContexts
    ) {
        this.add(annotationCode, message, element, surroundingElements, offendingContexts, AnnotationSeverity.ERROR);
    }

    public void add(
        @Nonnull String annotationCode,
        @Nonnull String message,
        @Nonnull IAntlrElement element,
        @Nonnull ImmutableList<IAntlrElement> surroundingElements,
        @Nonnull ImmutableList<ParserRuleContext> offendingContexts,
        @Nonnull AnnotationSeverity severity
    ) {
        RootCompilerAnnotation compilerAnnotation = this.getCompilerAnnotation(
            annotationCode,
            message,
            element,
            surroundingElements,
            offendingContexts,
            severity
        );
        this.compilerAnnotations.add(compilerAnnotation);
    }

    @Nonnull
    private RootCompilerAnnotation getCompilerAnnotation(
        @Nonnull String annotationCode,
        @Nonnull String message,
        @Nonnull IAntlrElement element,
        @Nonnull ImmutableList<IAntlrElement> surroundingElements,
        @Nonnull ImmutableList<ParserRuleContext> offendingContexts,
        @Nonnull AnnotationSeverity severity
    ) {
        Optional<CauseCompilerAnnotation> macroCause = this.getCauseCompilerAnnotation(element, severity);

        CompilationUnit compilationUnit = element.getCompilationUnit().get();
        return new RootCompilerAnnotation(
            compilationUnit,
            macroCause,
            offendingContexts,
            surroundingElements,
            annotationCode,
            message,
            this.ansiTokenColorizer,
            severity
        );
    }

    @Nonnull
    private CauseCompilerAnnotation getCauseCompilerAnnotation(
        @Nonnull IAntlrElement element,
        @Nonnull ImmutableList<IAntlrElement> surroundingElements,
        @Nonnull ImmutableList<ParserRuleContext> offendingContexts,
        @Nonnull AnnotationSeverity severity
    ) {
        Optional<CauseCompilerAnnotation> macroCause = this.getCauseCompilerAnnotation(element, severity);

        CompilationUnit compilationUnit = element.getCompilationUnit().get();
        return new CauseCompilerAnnotation(
            compilationUnit,
            macroCause,
            offendingContexts,
            surroundingElements,
            this.ansiTokenColorizer,
            severity
        );
    }

    private Optional<CauseCompilerAnnotation> getCauseCompilerAnnotation(
        @Nonnull IAntlrElement element,
        @Nonnull AnnotationSeverity severity
    ) {
        return element
            .getMacroElement()
            .map((macroElement) ->
                this.getCauseCompilerAnnotation(
                    macroElement,
                    macroElement.getSurroundingElements(),
                    Lists.immutable.with(macroElement.getElementContext()),
                    severity
                )
            );
    }

    public ImmutableList<RootCompilerAnnotation> getCompilerAnnotations() {
        return this.compilerAnnotations.toSortedList().toImmutable();
    }

    public boolean hasCompilerAnnotations() {
        return this.compilerAnnotations.notEmpty();
    }

    public void setAnsiTokenColorizer(@Nonnull AnsiTokenColorizer ansiTokenColorizer) {
        this.ansiTokenColorizer = Objects.requireNonNull(ansiTokenColorizer);
    }
}
