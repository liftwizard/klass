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

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.meta.grammar.KlassParser.CompilationUnitContext;
import cool.klass.model.meta.grammar.KlassParser.PackageDeclarationContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

public class AntlrCompilationUnit extends AntlrElement {

    public static final AntlrCompilationUnit AMBIGUOUS = new AntlrCompilationUnit(
        new CompilationUnitContext(AMBIGUOUS_PARENT, -1),
        Optional.empty()
    );

    public static final AntlrCompilationUnit NOT_FOUND = new AntlrCompilationUnit(
        new CompilationUnitContext(NOT_FOUND_PARENT, -1),
        Optional.empty()
    );

    private AntlrPackage pkg;

    public AntlrCompilationUnit(
        @Nonnull CompilationUnitContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit
    ) {
        super(elementContext, compilationUnit);
    }

    @Nonnull
    @Override
    public CompilationUnitContext getElementContext() {
        return (CompilationUnitContext) super.getElementContext();
    }

    @Nonnull
    @Override
    public Optional<IAntlrElement> getSurroundingElement() {
        return Optional.empty();
    }

    @Override
    public boolean isContext() {
        return true;
    }

    @Override
    public Pair<Token, Token> getContextBefore() {
        PackageDeclarationContext context = this.getElementContext().packageDeclaration();
        return Tuples.pair(context.getStart(), context.getStop());
    }

    public void enterPackageDeclaration(AntlrPackage pkg) {
        if (this.pkg != null) {
            throw new IllegalStateException();
        }
        this.pkg = Objects.requireNonNull(pkg);
    }

    public AntlrPackage getPackage() {
        return this.pkg;
    }

    public void reportNameErrors(CompilerAnnotationHolder compilerAnnotationHolder) {
        this.pkg.reportNameErrors(compilerAnnotationHolder);
    }

    @Override
    public String toString() {
        return this.pkg.toString();
    }
}
