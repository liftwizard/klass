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

package cool.klass.model.converter.compiler.phase;

import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.AntlrCompilationUnit;
import cool.klass.model.converter.compiler.state.AntlrPackage;
import cool.klass.model.meta.grammar.KlassParser.CompilationUnitContext;
import cool.klass.model.meta.grammar.KlassParser.PackageDeclarationContext;
import org.antlr.v4.runtime.ParserRuleContext;

public class CompilationUnitPhase extends AbstractCompilerPhase {

    private AntlrCompilationUnit compilationUnitState;

    public CompilationUnitPhase(@Nonnull CompilerState compilerState) {
        super(compilerState);
    }

    @Override
    public void enterCompilationUnit(@Nonnull CompilationUnitContext ctx) {
        super.enterCompilationUnit(ctx);

        CompilationUnit currentCompilationUnit = this.compilerState.getCompilerWalk().getCurrentCompilationUnit();
        ParserRuleContext parserContext = currentCompilationUnit.getParserContext();
        if (ctx != parserContext) {
            throw new AssertionError();
        }

        this.compilationUnitState = new AntlrCompilationUnit(ctx, Optional.of(currentCompilationUnit));
    }

    @Override
    public void exitCompilationUnit(@Nonnull CompilationUnitContext ctx) {
        this.compilerState.getDomainModel().exitCompilationUnit(this.compilationUnitState);
        this.compilationUnitState = null;
        super.exitCompilationUnit(ctx);
    }

    @Override
    public void enterPackageDeclaration(@Nonnull PackageDeclarationContext ctx) {
        super.enterPackageDeclaration(ctx);

        AntlrPackage pkg = new AntlrPackage(
            ctx,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            -1,
            ctx.packageName(),
            this.compilationUnitState
        );

        this.compilationUnitState.enterPackageDeclaration(pkg);
    }
}
