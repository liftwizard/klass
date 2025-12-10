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

package cool.klass.model.converter.compiler;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.annotation.AbstractCompilerAnnotation;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.annotation.RootCompilerAnnotation;
import cool.klass.model.converter.compiler.parser.AntlrUtils;
import cool.klass.model.converter.compiler.parser.DelegatingKlassListener;
import cool.klass.model.converter.compiler.phase.AbstractCompilerPhase;
import cool.klass.model.converter.compiler.state.AntlrDomainModel;
import cool.klass.model.converter.compiler.state.AntlrElement;
import cool.klass.model.meta.domain.DomainModelImpl.DomainModelBuilder;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import cool.klass.model.meta.grammar.KlassListener;
import cool.klass.model.meta.grammar.KlassParser;
import cool.klass.model.meta.grammar.KlassParser.CompilationUnitContext;
import cool.klass.model.meta.grammar.KlassParser.TopLevelDeclarationContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

public class CompilerState {

    @Nonnull
    private final CompilerInputState compilerInput;

    private final CompilerAnnotationHolder compilerAnnotationHolder = new CompilerAnnotationHolder();
    private final AntlrDomainModel domainModel = new AntlrDomainModel();
    private CompilerWalkState compilerWalk = new CompilerWalkState(this.domainModel);

    public CompilerState(@Nonnull ImmutableList<CompilationUnit> compilationUnits) {
        this.compilerInput = new CompilerInputState(compilationUnits);
    }

    public void runInPlaceCompilerMacro(
        @Nonnull AntlrElement macroElement,
        @Nonnull AbstractCompilerPhase macroExpansionCompilerPhase,
        @Nonnull String sourceCodeText,
        @Nonnull Function<KlassParser, ? extends ParserRuleContext> parserRule,
        ParserRuleContext inPlaceContext,
        ParseTreeListener... listeners
    ) {
        Objects.requireNonNull(macroElement);

        CompilationUnit compilationUnit = CompilationUnit.getMacroCompilationUnit(
            this.compilerInput.getCompilationUnits().size(),
            macroElement,
            macroExpansionCompilerPhase,
            sourceCodeText,
            parserRule
        );

        ParserRuleContext parserContext = compilationUnit.getParserContext();
        parserContext.setParent(inPlaceContext);
        inPlaceContext.addChild(parserContext);

        this.compilerWalk.withInPlaceCompilationUnit(compilationUnit, () ->
            this.compilerInput.runInPlaceCompilerMacro(compilationUnit, Lists.immutable.with(listeners))
        );
    }

    public void runRootCompilerMacro(
        @Nonnull AntlrElement macroElement,
        @Nonnull AbstractCompilerPhase macroExpansionCompilerPhase,
        @Nonnull String sourceCodeText,
        @Nonnull Function<KlassParser, ? extends ParserRuleContext> parserRule,
        @Nonnull ImmutableList<ParseTreeListener> listeners
    ) {
        CompilationUnit compilationUnit = CompilationUnit.getMacroCompilationUnit(
            this.compilerInput.getCompilationUnits().size(),
            macroElement,
            macroExpansionCompilerPhase,
            sourceCodeText,
            parserRule
        );

        this.runRootCompilerMacro(listeners, compilationUnit);
    }

    private void runRootCompilerMacro(
        @Nonnull ImmutableList<ParseTreeListener> listeners,
        @Nonnull CompilationUnit compilationUnit
    ) {
        CompilerWalkState oldCompilerWalk = this.compilerWalk;
        try {
            this.compilerWalk = new CompilerWalkState(this.domainModel);

            this.compilerInput.runCompilerMacro(compilationUnit, listeners);
        } finally {
            this.compilerWalk = oldCompilerWalk;
        }
    }

    public void reportErrors() {
        this.domainModel.reportErrors(this.compilerAnnotationHolder);
    }

    @Nonnull
    public CompilationResult getCompilationResult(ImmutableList<RootCompilerAnnotation> compilerAnnotations) {
        if (compilerAnnotations.anySatisfy(AbstractCompilerAnnotation::isError)) {
            return new CompilationResult(compilerAnnotations, Optional.empty());
        }
        return new CompilationResult(compilerAnnotations, Optional.of(this.buildDomainModel()));
    }

    @Nonnull
    private DomainModelWithSourceCode buildDomainModel() {
        ImmutableList<RootCompilerAnnotation> compilerAnnotations =
            this.compilerAnnotationHolder.getCompilerAnnotations();

        if (compilerAnnotations.anySatisfy(AbstractCompilerAnnotation::isError)) {
            throw new AssertionError(this.compilerAnnotationHolder.getCompilerAnnotations().makeString("\n"));
        }

        ImmutableList<CompilationUnit> compilationUnits = this.compilerInput.getCompilationUnits().toImmutable();
        DomainModelBuilder domainModelBuilder = this.domainModel.build(compilationUnits);
        return domainModelBuilder.build();
    }

    @Nonnull
    public AntlrDomainModel getDomainModel() {
        return this.domainModel;
    }

    @Nonnull
    public CompilerInputState getCompilerInput() {
        return this.compilerInput;
    }

    @Nonnull
    public CompilerAnnotationHolder getCompilerAnnotationHolder() {
        return this.compilerAnnotationHolder;
    }

    public CompilerWalkState getCompilerWalk() {
        return this.compilerWalk;
    }

    public Integer getOrdinal(@Nonnull ParserRuleContext ctx) {
        TopLevelDeclarationContext topLevelDeclarationContext = AntlrUtils.getParentOfType(
            ctx,
            TopLevelDeclarationContext.class
        );

        if (ctx == topLevelDeclarationContext) {
            throw new AssertionError(ctx);
        }

        Integer topLevelElementOrdinalByContext = this.domainModel.getTopLevelElementOrdinalByContext(
            topLevelDeclarationContext
        );
        Objects.requireNonNull(topLevelElementOrdinalByContext);
        return topLevelElementOrdinalByContext;
    }

    @Nonnull
    public KlassListener asListener() {
        return new ListenerView();
    }

    public class ListenerView extends DelegatingKlassListener {

        @Override
        protected KlassListener getDelegate() {
            return CompilerState.this.compilerWalk.asListener();
        }

        @Override
        public void enterCompilationUnit(@Nonnull CompilationUnitContext ctx) {
            super.enterCompilationUnit(ctx);
            CompilationUnit currentCompilationUnit = CompilerState.this.compilerInput.getCompilationUnitByContext(ctx);
            CompilerState.this.compilerWalk.enterCompilationUnit(currentCompilationUnit);
        }

        @Override
        public void exitCompilationUnit(@Nonnull CompilationUnitContext ctx) {
            super.exitCompilationUnit(ctx);
            CompilerState.this.compilerWalk.exitCompilationUnit();
        }
    }
}
