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
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.projection.AntlrProjection;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionDeclarationContext;

public class ProjectionDeclarationPhase extends AbstractCompilerPhase {

    @Nullable
    private AntlrProjection projection;

    public ProjectionDeclarationPhase(@Nonnull CompilerState compilerState) {
        super(compilerState);
    }

    @Override
    public void enterProjectionDeclaration(@Nonnull ProjectionDeclarationContext ctx) {
        super.enterProjectionDeclaration(ctx);

        String classifierName = ctx.classifierReference().identifier().getText();
        AntlrClassifier classifier = this.compilerState.getDomainModel().getClassifierByName(classifierName);
        IdentifierContext nameContext = ctx.identifier();
        CompilationUnit currentCompilationUnit = this.compilerState.getCompilerWalk().getCurrentCompilationUnit();
        this.projection = new AntlrProjection(
            ctx,
            Optional.of(currentCompilationUnit),
            this.compilerState.getOrdinal(ctx),
            nameContext,
            this.compilerState.getCompilerWalk().getCompilationUnit(),
            classifier,
            this.compilerState.getCompilerWalk().getPackageName()
        );
    }

    @Override
    public void exitProjectionDeclaration(@Nonnull ProjectionDeclarationContext ctx) {
        this.compilerState.getDomainModel().exitProjectionDeclaration(this.projection);

        super.exitProjectionDeclaration(ctx);
    }
}
