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

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.syntax.highlighter.ansi.FunctionalSyntaxHighlighter;
import org.fusesource.jansi.Ansi;

public abstract class AbstractContextString {

    private final int line;

    @Nonnull
    private final String string;

    @Nonnull
    private final FunctionalSyntaxHighlighter syntaxHighlighter;

    protected AbstractContextString(
        int line,
        @Nonnull String string,
        @Nonnull FunctionalSyntaxHighlighter syntaxHighlighter
    ) {
        this.line = line;
        this.string = Objects.requireNonNull(string);
        this.syntaxHighlighter = Objects.requireNonNull(syntaxHighlighter);

        // Enforce that content strings don't contain newlines - they should represent single logical lines
        if (this.string.contains("\n")) {
            throw new AssertionError(
                "Context string content must not contain newlines. Found: " +
                this.string.replace('\n', '↵').replace('\r', '↵')
            );
        }
    }

    private static String padLeft(String string, int width) {
        return String.format("%" + width + "s║", string);
    }

    public int getLine() {
        return this.line;
    }

    public String toString(int lineNumberWidth) {
        // Content should never contain newlines, so don't split
        // If content contains newlines, this will be caught by the assertion in the constructor
        return this.toString(this.string, 0, lineNumberWidth);
    }

    private String toString(String string, int offset, int lineNumberWidth) {
        String lineNumberString = this.getLineNumberString(this.line + offset);
        String paddedLineNumberString = AbstractContextString.padLeft(lineNumberString, lineNumberWidth);

        Ansi ansi = Ansi.ansi();
        ansi.reset();

        this.syntaxHighlighter.applyInitialThemeStyles(ansi);
        this.syntaxHighlighter.applyLineNumberStyle(ansi);
        ansi.a(paddedLineNumberString);

        this.syntaxHighlighter.applyInitialThemeStyles(ansi);
        ansi.a(" ").a(string);
        return ansi.toString();
    }

    @Override
    public String toString() {
        return this.toString(4);
    }

    @Nonnull
    protected abstract String getLineNumberString(int line);
}
