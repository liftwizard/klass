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

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.AntlrInterface;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import cool.klass.model.meta.grammar.KlassParser.AbstractDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ClassDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ClassifierModifierContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceDeclarationContext;

public class ClassifierPhase extends AbstractCompilerPhase {

    @Nullable
    private AntlrClassifier classifier;

    @Nullable
    private AntlrInterface iface;

    @Nullable
    private AntlrClass klass;

    public ClassifierPhase(@Nonnull CompilerState compilerState) {
        super(compilerState);
    }

    @Override
    public void enterInterfaceDeclaration(@Nonnull InterfaceDeclarationContext ctx) {
        super.enterInterfaceDeclaration(ctx);

        IdentifierContext identifier = ctx.interfaceHeader().identifier();
        this.iface = new AntlrInterface(
            ctx,
            this.compilerState.getCompilerWalk().getCompilationUnit(),
            this.compilerState.getOrdinal(ctx),
            identifier
        );
        this.classifier = this.iface;
    }

    @Override
    public void exitInterfaceDeclaration(@Nonnull InterfaceDeclarationContext ctx) {
        this.compilerState.getDomainModel().exitInterfaceDeclaration(this.iface);
        this.iface = null;
        this.classifier = null;
        super.exitInterfaceDeclaration(ctx);
    }

    @Override
    public void enterClassDeclaration(@Nonnull ClassDeclarationContext ctx) {
        super.enterClassDeclaration(ctx);

        String classOrUserKeyword = ctx.classHeader().classOrUser().getText();

        this.klass = new AntlrClass(
            ctx,
            this.compilerState.getCompilerWalk().getCompilationUnit(),
            this.compilerState.getOrdinal(ctx),
            ctx.classHeader().identifier(),
            classOrUserKeyword.equals("user")
        );
        this.classifier = this.klass;
    }

    @Override
    public void exitClassDeclaration(@Nonnull ClassDeclarationContext ctx) {
        this.compilerState.getDomainModel().exitClassDeclaration(this.klass);
        this.klass = null;
        this.classifier = null;
        super.exitClassDeclaration(ctx);
    }

    @Override
    public void enterAbstractDeclaration(@Nonnull AbstractDeclarationContext ctx) {
        super.enterAbstractDeclaration(ctx);
        this.klass.setAbstract(true);
    }

    @Override
    public void enterClassifierModifier(@Nonnull ClassifierModifierContext ctx) {
        super.enterClassifierModifier(ctx);

        int ordinal = this.classifier.getNumClassifierModifiers();

        AntlrModifier modifier = new AntlrModifier(
            ctx,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            ordinal + 1,
            this.classifier
        );

        this.classifier.enterModifier(modifier);
    }
}
