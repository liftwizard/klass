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

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.google.common.base.Stopwatch;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.annotation.RootCompilerAnnotation;
import cool.klass.model.converter.compiler.phase.AssociationPhase;
import cool.klass.model.converter.compiler.phase.AuditAssociationInferencePhase;
import cool.klass.model.converter.compiler.phase.AuditPropertyInferencePhase;
import cool.klass.model.converter.compiler.phase.ClassTemporalPropertyInferencePhase;
import cool.klass.model.converter.compiler.phase.ClassifierPhase;
import cool.klass.model.converter.compiler.phase.CompilationUnitPhase;
import cool.klass.model.converter.compiler.phase.EnumerationsPhase;
import cool.klass.model.converter.compiler.phase.InheritancePhase;
import cool.klass.model.converter.compiler.phase.OrderByDirectionInferencePhase;
import cool.klass.model.converter.compiler.phase.OrderByDirectionPhase;
import cool.klass.model.converter.compiler.phase.OrderByPhase;
import cool.klass.model.converter.compiler.phase.ParameterizedPropertyPhase;
import cool.klass.model.converter.compiler.phase.ProjectionDeclarationPhase;
import cool.klass.model.converter.compiler.phase.ProjectionPhase;
import cool.klass.model.converter.compiler.phase.PropertyPhase;
import cool.klass.model.converter.compiler.phase.RelationshipInferencePhase;
import cool.klass.model.converter.compiler.phase.RelationshipPhase;
import cool.klass.model.converter.compiler.phase.ServiceCriteriaInferencePhase;
import cool.klass.model.converter.compiler.phase.ServiceCriteriaPhase;
import cool.klass.model.converter.compiler.phase.ServiceMultiplicityInferencePhase;
import cool.klass.model.converter.compiler.phase.ServiceMultiplicityPhase;
import cool.klass.model.converter.compiler.phase.ServicePhase;
import cool.klass.model.converter.compiler.phase.TopLevelElementsPhase;
import cool.klass.model.converter.compiler.phase.UrlParameterPhase;
import cool.klass.model.converter.compiler.phase.VariableResolutionPhase;
import cool.klass.model.converter.compiler.phase.VersionAssociationInferencePhase;
import cool.klass.model.converter.compiler.phase.VersionClassInferencePhase;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.AnsiTokenColorizer;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.token.categories.TokenCategory;
import cool.klass.model.converter.compiler.token.categorizing.lexer.LexerBasedTokenCategorizer;
import cool.klass.model.converter.compiler.token.categorizing.parser.ParserBasedTokenCategorizer;
import cool.klass.model.meta.grammar.KlassListener;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMapIterable;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KlassCompiler {

    public static final ImmutableList<Function<CompilerState, KlassListener>> COMPILER_PHASE_BUILDERS =
        Lists.immutable.with(
            CompilationUnitPhase::new,
            TopLevelElementsPhase::new,
            EnumerationsPhase::new,
            ClassifierPhase::new,
            PropertyPhase::new,
            InheritancePhase::new,
            ParameterizedPropertyPhase::new,
            VersionClassInferencePhase::new,
            ClassTemporalPropertyInferencePhase::new,
            AuditPropertyInferencePhase::new,
            AssociationPhase::new,
            VersionAssociationInferencePhase::new,
            AuditAssociationInferencePhase::new,
            RelationshipPhase::new,
            RelationshipInferencePhase::new,
            ProjectionDeclarationPhase::new,
            ProjectionPhase::new,
            ServicePhase::new,
            ServiceMultiplicityPhase::new,
            ServiceMultiplicityInferencePhase::new,
            UrlParameterPhase::new,
            ServiceCriteriaPhase::new,
            ServiceCriteriaInferencePhase::new,
            VariableResolutionPhase::new,
            OrderByPhase::new,
            OrderByDirectionPhase::new,
            OrderByDirectionInferencePhase::new
        );

    private static final Logger LOGGER = LoggerFactory.getLogger(KlassCompiler.class);

    private final CompilerState compilerState;
    private final AnsiColorScheme colorScheme;

    public KlassCompiler(CompilationUnit compilationUnit, AnsiColorScheme colorScheme) {
        this(Lists.immutable.with(compilationUnit), colorScheme);
    }

    public KlassCompiler(ImmutableList<CompilationUnit> compilationUnits, AnsiColorScheme colorScheme) {
        this.compilerState = new CompilerState(compilationUnits);
        this.colorScheme = Objects.requireNonNull(colorScheme);
    }

    private void executeCompilerPhase(KlassListener compilerPhase) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        // Compiler macros may add new compilation units within a compiler phase, so take an immutable copy
        ImmutableList<CompilationUnit> immutableCompilationUnits = this.compilerState.getCompilerInput()
            .getCompilationUnits()
            .toImmutable();

        ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
        for (CompilationUnit compilationUnit : immutableCompilationUnits) {
            try {
                this.compilerState.getCompilerWalk().assertEmpty();
                parseTreeWalker.walk(compilerPhase, compilationUnit.getParserContext());
                this.compilerState.getCompilerWalk().assertEmpty();
            } catch (RuntimeException e) {
                String message = "Exception in compiler during phase: %s for compilation unit: %s".formatted(
                    compilerPhase.getClass().getSimpleName(),
                    compilationUnit.getFullPathSourceName()
                );
                throw new RuntimeException(message, e);
            }
        }

        Stopwatch stopped = stopwatch.stop();
        long sElapsed = stopped.elapsed(TimeUnit.SECONDS);
        long msElapsed = stopped.elapsed(TimeUnit.MILLISECONDS);
        LOGGER.info(
            "Executed compiler phase {} in {}s {}ms.",
            compilerPhase.getClass().getSimpleName(),
            sElapsed,
            msElapsed
        );
    }

    @Nonnull
    public CompilationResult compile() {
        ImmutableList<KlassListener> compilerPhases = COMPILER_PHASE_BUILDERS.collectWith(
            Function::apply,
            this.compilerState
        );
        for (KlassListener compilerPhase : compilerPhases) {
            this.executeCompilerPhase(compilerPhase);
        }

        CompilerInputState compilerInputState = this.compilerState.getCompilerInput();
        ImmutableList<CompilationUnit> compilationUnits = compilerInputState.getCompilationUnits().toImmutable();
        MapIterable<Token, TokenCategory> tokenCategoriesFromLexer = this.getTokenCategoriesFromLexer(compilationUnits);
        MapIterable<Token, TokenCategory> tokenCategoriesFromParser = this.getTokenCategoriesFromParser(
            compilationUnits
        );

        CompilerAnnotationHolder compilerAnnotationHolder = this.compilerState.getCompilerAnnotationHolder();

        var ansiTokenColorizer = new AnsiTokenColorizer(
            this.colorScheme,
            tokenCategoriesFromParser,
            tokenCategoriesFromLexer
        );
        compilerAnnotationHolder.setAnsiTokenColorizer(ansiTokenColorizer);

        this.compilerState.reportErrors();
        ImmutableList<RootCompilerAnnotation> compilerAnnotations = compilerAnnotationHolder.getCompilerAnnotations();

        return this.compilerState.getCompilationResult(compilerAnnotations);
    }

    private MapIterable<Token, TokenCategory> getTokenCategoriesFromLexer(
        ImmutableList<CompilationUnit> compilationUnits
    ) {
        MutableMapIterable<Token, TokenCategory> tokenCategoriesFromLexer = OrderedMapAdapter.adapt(
            new LinkedHashMap<>()
        );
        compilationUnits
            .collect(CompilationUnit::getTokenStream)
            .forEachWith(LexerBasedTokenCategorizer::findTokenCategoriesFromLexer, tokenCategoriesFromLexer);
        return tokenCategoriesFromLexer.asUnmodifiable();
    }

    private MapIterable<Token, TokenCategory> getTokenCategoriesFromParser(
        ImmutableList<CompilationUnit> compilationUnits
    ) {
        var listener = new ParserBasedTokenCategorizer();

        compilationUnits
            .collect(CompilationUnit::getParserContext)
            .forEachWith(ParserBasedTokenCategorizer::findTokenCategoriesFromParser, listener);
        return listener.getTokenCategories();
    }
}
