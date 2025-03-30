/*
 * Copyright 2024 Craig Motlin
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
import cool.klass.model.meta.domain.EnumerationLiteralImpl.EnumerationLiteralBuilder;
import cool.klass.model.meta.grammar.KlassParser.EnumerationLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.tuple.Pair;

public class AntlrEnumerationLiteral extends AntlrIdentifierElement {

    public static final AntlrEnumerationLiteral AMBIGUOUS = new AntlrEnumerationLiteral(
        new EnumerationLiteralContext(AMBIGUOUS_PARENT, -1),
        Optional.empty(),
        -1,
        AMBIGUOUS_IDENTIFIER_CONTEXT,
        Optional.empty(),
        AntlrEnumeration.AMBIGUOUS
    );

    public static final AntlrEnumerationLiteral NOT_FOUND = new AntlrEnumerationLiteral(
        new EnumerationLiteralContext(NOT_FOUND_PARENT, -1),
        Optional.empty(),
        -1,
        NOT_FOUND_IDENTIFIER_CONTEXT,
        Optional.empty(),
        AntlrEnumeration.NOT_FOUND
    );

    @Nonnull
    private final Optional<String> prettyName;

    @Nonnull
    private final AntlrEnumeration owningEnumeration;

    private EnumerationLiteralBuilder elementBuilder;

    public AntlrEnumerationLiteral(
        @Nonnull EnumerationLiteralContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        int ordinal,
        @Nonnull IdentifierContext nameContext,
        @Nonnull Optional<String> prettyName,
        @Nonnull AntlrEnumeration owningEnumeration
    ) {
        super(elementContext, compilationUnit, ordinal, nameContext);
        this.prettyName = prettyName;
        this.owningEnumeration = Objects.requireNonNull(owningEnumeration);
    }

    @Nonnull
    @Override
    public EnumerationLiteralContext getElementContext() {
        return (EnumerationLiteralContext) super.getElementContext();
    }

    @Nonnull
    @Override
    public Optional<IAntlrElement> getSurroundingElement() {
        return Optional.of(this.owningEnumeration);
    }

    @Override
    public Pair<Token, Token> getContextBefore() {
        return this.getEntireContext();
    }

    @Nonnull
    public Optional<String> getPrettyName() {
        return this.prettyName;
    }

    @Nonnull
    @Override
    protected Pattern getNamePattern() {
        return CONSTANT_NAME_PATTERN;
    }

    public EnumerationLiteralBuilder build() {
        if (this.elementBuilder != null) {
            throw new IllegalStateException();
        }

        this.elementBuilder = new EnumerationLiteralBuilder(
            this.getElementContext(),
            this.getMacroElementBuilder(),
            this.getSourceCodeBuilder(),
            this.ordinal,
            this.getNameContext(),
            this.prettyName,
            this.owningEnumeration.getElementBuilder()
        );
        return this.elementBuilder;
    }

    public void reportDuplicateName(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        String message = String.format("Duplicate enumeration literal: '%s'.", this.getName());
        compilerAnnotationHolder.add("ERR_DUP_ENM", message, this);
    }

    public void reportDuplicatePrettyName(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        String message = String.format("Duplicate enumeration pretty name: '%s'.", this.prettyName.get());
        compilerAnnotationHolder.add("ERR_DUP_LIT", message, this, this.getElementContext().enumerationPrettyName());
    }

    @Override
    public boolean isContext() {
        return true;
    }
}
