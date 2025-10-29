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

package cool.klass.model.converter.compiler.state;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

public abstract class AntlrNamedElement extends AntlrOrdinalElement {

    protected static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile("^[a-z]+(\\.[a-z][a-z0-9]*)*$");
    protected static final Pattern TYPE_NAME_PATTERN = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");
    protected static final Pattern MEMBER_NAME_PATTERN = Pattern.compile("^[a-z][a-zA-Z0-9]*$");
    protected static final Pattern CONSTANT_NAME_PATTERN = Pattern.compile("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$");

    protected static final ImmutableList<String> JAVA_KEYWORDS = Lists.immutable.with(
        "abstract",
        "assert",
        "boolean",
        "break",
        "byte",
        "case",
        "catch",
        "char",
        "class",
        "const",
        "continue",
        "default",
        "do",
        "double",
        "else",
        "enum",
        "extends",
        "final",
        "finally",
        "float",
        "for",
        "goto",
        "if",
        "implements",
        "import",
        "instanceof",
        "int",
        "interface",
        "long",
        "native",
        "new",
        "package",
        "private",
        "protected",
        "public",
        "return",
        "short",
        "static",
        "strictfp",
        "super",
        "switch",
        "synchronized",
        "this",
        "throw",
        "throws",
        "transient",
        "try",
        "var",
        "void",
        "volatile",
        "while"
    );

    protected static final ImmutableList<String> JAVA_LITERALS = Lists.immutable.with("true", "false", "null");

    protected static final ImmutableList<String> SQL_KEYWORDS = Lists.immutable.with();

    protected static final ImmutableList<String> RELADOMO_TYPES = Lists.immutable.with("OrderBy");

    @Nonnull
    protected final ParserRuleContext nameContext;

    protected AntlrNamedElement(
        @Nonnull ParserRuleContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        int ordinal,
        @Nonnull ParserRuleContext nameContext
    ) {
        super(elementContext, compilationUnit, ordinal);
        this.nameContext = Objects.requireNonNull(nameContext);
    }

    @Nonnull
    public ParserRuleContext getNameContext() {
        return this.nameContext;
    }

    @Nonnull
    public String getName() {
        return this.nameContext.getText();
    }

    // TODO: 💡 Some name errors should really just be warnings. Rename CompilerError to CompilerAnnotation and implement severity.
    public void reportNameErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        this.reportKeywordCollision(compilerAnnotationHolder);

        if (!this.getNamePattern().matcher(this.getName()).matches()) {
            String message = String.format(
                "Name must match pattern %s but was '%s'.",
                this.getNamePattern(),
                this.getName()
            );
            compilerAnnotationHolder.add("ERR_NME_PAT", message, this);
        }
    }

    protected abstract Pattern getNamePattern();

    // TODO: ⬇ Potentially refine a smaller list of keywords that clash with associations/projections/services and a separate name pattern
    protected void reportKeywordCollision(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (JAVA_KEYWORDS.contains(this.getName())) {
            String message = String.format("'%s' is a reserved Java keyword.", this.getName());
            compilerAnnotationHolder.add("ERR_NME_KEY", message, this);
        }

        if (JAVA_LITERALS.contains(this.getName())) {
            String message = String.format("'%s' is a reserved Java literal.", this.getName());
            compilerAnnotationHolder.add("ERR_NME_LIT", message, this);
        }

        if (SQL_KEYWORDS.contains(this.getName())) {
            String message = String.format("'%s' is a reserved SQL keyword.", this.getName());
            compilerAnnotationHolder.add("ERR_SQL_KEY", message, this);
        }
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
