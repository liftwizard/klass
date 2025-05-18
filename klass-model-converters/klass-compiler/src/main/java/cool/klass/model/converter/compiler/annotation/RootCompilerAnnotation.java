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

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.FunctionalSyntaxHighlighter;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.fusesource.jansi.Ansi;

public class RootCompilerAnnotation extends AbstractCompilerAnnotation implements Comparable<RootCompilerAnnotation> {

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
        @Nonnull CompilationUnit compilationUnit,
        @Nonnull Optional<CauseCompilerAnnotation> macroCause,
        @Nonnull ImmutableList<ParserRuleContext> offendingContexts,
        @Nonnull ImmutableList<IAntlrElement> sourceContexts,
        @Nonnull String annotationCode,
        @Nonnull String message,
        @Nonnull FunctionalSyntaxHighlighter syntaxHighlighter,
        @Nonnull AnnotationSeverity severity
    ) {
        super(compilationUnit, macroCause, offendingContexts, sourceContexts, syntaxHighlighter, severity);
        this.annotationCode = Objects.requireNonNull(annotationCode);
        this.message = Objects.requireNonNull(message);
    }

    @Nonnull
    public String getAnnotationCode() {
        return this.annotationCode;
    }

    @Override
    public int compareTo(@Nonnull RootCompilerAnnotation other) {
        return COMPILER_ANNOTATION_COMPARATOR.compare(this, other);
    }

    @Nonnull
    @Override
    public String toString() {
        String contextString = this.getContextString();
        String locationMessage = this.getOptionalLocationMessage();
        String causeString = this.getCauseString();
        String severityName = this.severity == AnnotationSeverity.ERROR ? "Error" : "Warning";

        Ansi ansi = Ansi.ansi();

        // Apply theme background
        this.syntaxHighlighter.applyInitialThemeStyles(ansi);

        // Header line
        ansi.a("════════════════════════════════════════ ");
        this.syntaxHighlighter.getColorScheme().annotationCode(ansi);
        ansi.a(this.annotationCode);
        this.syntaxHighlighter.applyInitialThemeStyles(ansi);
        ansi.a(" ════════════════════════════════════════\n");

        // Error/Warning line
        if (this.severity == AnnotationSeverity.ERROR) {
            this.syntaxHighlighter.getColorScheme().errorAnnotation(ansi);
        } else {
            this.syntaxHighlighter.getColorScheme().warningAnnotation(ansi);
        }
        ansi.a(severityName).a(": ").a(this.message);

        // Apply only background for theme consistency, don't override foreground color
        this.syntaxHighlighter.getColorScheme().background(ansi);
        ansi.a("\n\nAt ").a(this.getShortLocationString()).a("\n\n");

        // Context, location, and cause
        ansi.a(contextString);
        ansi.a(locationMessage);
        ansi.a(causeString);

        // Footer line - ensure it uses theme foreground color
        this.syntaxHighlighter.getColorScheme().foreground(ansi);
        ansi.a("═════════════════════════════════════════════════════════════════════════════════════════════\n\n");

        // Final reset
        this.syntaxHighlighter.applyFinalReset(ansi);

        return ansi.toString() + "\n";
    }

    @Override
    protected void applyCaretColor(Ansi ansi) {
        switch (this.severity) {
            case ERROR -> this.syntaxHighlighter.getColorScheme().errorCaret(ansi);
            case WARNING -> this.syntaxHighlighter.getColorScheme().warningCaret(ansi);
            default -> throw new AssertionError("Unexpected severity: " + this.severity);
        }
    }

    @Override
    public String toGitHubAnnotation() {
        Pair<Token, Token> firstAndLastToken = this.getFirstAndLastToken();
        Token startToken = firstAndLastToken.getOne();
        Token endToken = firstAndLastToken.getTwo();

        String sourceName = this.compilationUnit.getSourceName();
        return "::%s file=%s,line=%d,endLine=%d,col=%d,endColumn=%d,title=%s::%s".formatted(
            this.getSeverityString(),
            sourceName,
            startToken.getLine(),
            endToken.getLine(),
            startToken.getCharPositionInLine(),
            endToken.getCharPositionInLine(),
            this.annotationCode,
            this.message
        );
    }

    @Nonnull
    protected String getSeverityString() {
        return switch (this.severity) {
            case ERROR -> "error";
            case WARNING -> "warning";
        };
    }
}
