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

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.meta.domain.AbstractElement.ElementBuilder;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.grammar.KlassParser.CompilationUnitContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;

public abstract class AntlrElement implements IAntlrElement {

    public static final ParserRuleContext AMBIGUOUS_PARENT = new ParserRuleContext();
    public static final ParserRuleContext NOT_FOUND_PARENT = new ParserRuleContext();

    @Nonnull
    protected final ParserRuleContext elementContext;

    // TODO: Consider creating a "native" source file with native declarations of PrimitiveTypes
    /**
     * The type of compilationUnit is Optional because some Elements, specifically PrimitiveTypes are not declared in SourceCode
     */
    @Nonnull
    protected final Optional<CompilationUnit> compilationUnit;

    protected AntlrElement(
        @Nonnull ParserRuleContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit
    ) {
        this.elementContext = Objects.requireNonNull(elementContext);
        this.compilationUnit = Objects.requireNonNull(compilationUnit);

        compilationUnit.ifPresent(cu -> AntlrElement.assertContextContains(cu.getParserContext(), elementContext));
    }

    private static void assertContextContains(ParserRuleContext parentContext, ParserRuleContext childContext) {
        if (parentContext == childContext) {
            return;
        }

        ParserRuleContext nextParent = childContext.getParent();
        Objects.requireNonNull(nextParent);
        AntlrElement.assertContextContains(parentContext, nextParent);
    }

    /**
     * This method is meant to be used by data renderers in the IntelliJ debugger. It is lenient to accept sentinels like AMBIGUOUS and NOT_FOUND.
     *
     * @param parserRuleContext the parserRuleContext to get the source text of
     *
     * @return the source text of the given parserRuleContext, or "AMBIGUOUS" or "NOT_FOUND" if the parserRuleContext is a sentinel
     *
     * @throws AssertionError if the parserRuleContext is not a sentinel but looks too much like a sentinel
     */
    @SuppressWarnings("unused")
    public static String getSourceTextLenient(ParserRuleContext parserRuleContext) {
        Objects.requireNonNull(parserRuleContext);

        Token start = parserRuleContext.getStart();
        Token stop = parserRuleContext.getStop();
        ParserRuleContext parent = parserRuleContext.getParent();
        RuleContext payload = parserRuleContext.getPayload();
        RuleContext ruleContext = parserRuleContext.getRuleContext();
        int childCount = parserRuleContext.getChildCount();

        if (
            start == null ||
            stop == null ||
            (parent == null && !(parserRuleContext instanceof CompilationUnitContext)) ||
            payload != parserRuleContext ||
            ruleContext != parserRuleContext ||
            childCount == 0
        ) {
            if (start != null) {
                throw new AssertionError();
            }
            if (stop != null) {
                throw new AssertionError();
            }
            if (payload != parserRuleContext) {
                throw new AssertionError();
            }
            if (ruleContext != parserRuleContext) {
                throw new AssertionError();
            }
            if (childCount != 0) {
                throw new AssertionError();
            }
            if (parent == AMBIGUOUS_PARENT) {
                return "AMBIGUOUS";
            }
            if (parent == NOT_FOUND_PARENT) {
                return "NOT_FOUND";
            }

            throw new AssertionError();
        }

        int startIndex = start.getStartIndex();
        int stopIndex = stop.getStopIndex();
        Interval interval = new Interval(startIndex, stopIndex);
        return start.getInputStream().getText(interval);
    }

    protected static String getSourceText(ParserRuleContext parserRuleContext) {
        Objects.requireNonNull(parserRuleContext.getStart());
        Objects.requireNonNull(parserRuleContext.getStop());
        int startIndex = parserRuleContext.getStart().getStartIndex();
        int stopIndex = parserRuleContext.getStop().getStopIndex();
        Interval interval = new Interval(startIndex, stopIndex);
        return parserRuleContext.getStart().getInputStream().getText(interval);
    }

    @Override
    @Nonnull
    public ParserRuleContext getElementContext() {
        return this.elementContext;
    }

    @Nonnull
    public ElementBuilder<?> getElementBuilder() {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".getElementBuilder() not implemented yet"
        );
    }

    @Override
    @Nonnull
    public Optional<AntlrElement> getMacroElement() {
        return this.compilationUnit.flatMap(CompilationUnit::getMacroElement);
    }

    public boolean hasMacro() {
        return this.getMacroElement().isPresent();
    }

    @Nonnull
    protected Optional<ElementBuilder<?>> getMacroElementBuilder() {
        return this.getMacroElement().map(antlrElement -> Objects.requireNonNull(antlrElement.getElementBuilder()));
    }

    protected SourceCodeBuilder getSourceCodeBuilder() {
        return this.compilationUnit.map(CompilationUnit::build).orElseThrow();
    }

    @Nonnull
    @Override
    public Optional<CompilationUnit> getCompilationUnit() {
        return this.compilationUnit;
    }

    public boolean isInSameCompilationUnit(AntlrElement other) {
        return (
            this.compilationUnit.isPresent() &&
            other.compilationUnit.isPresent() &&
            this.compilationUnit.equals(other.compilationUnit)
        );
    }

    public boolean isForwardReference(AntlrElement other) {
        return (
            this.isInSameCompilationUnit(other) &&
            this.getElementContext().getStart().getStartIndex() < other.getElementContext().getStart().getStartIndex()
        );
    }

    @Override
    public String toString() {
        return AntlrElement.getSourceText(this.getElementContext());
    }
}
