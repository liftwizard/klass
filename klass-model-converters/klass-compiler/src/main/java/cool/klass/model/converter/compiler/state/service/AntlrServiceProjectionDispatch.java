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

package cool.klass.model.converter.compiler.state.service;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.AntlrElement;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.projection.AntlrProjection;
import cool.klass.model.meta.domain.service.ServiceProjectionDispatchImpl.ServiceProjectionDispatchBuilder;
import cool.klass.model.meta.grammar.KlassParser.ProjectionReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceProjectionDispatchContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.tuple.Pair;

public class AntlrServiceProjectionDispatch extends AntlrElement {

    @Nonnull
    private final AntlrService service;

    @Nonnull
    private final AntlrProjection projection;

    private ServiceProjectionDispatchBuilder elementBuilder;

    public AntlrServiceProjectionDispatch(
        @Nonnull ServiceProjectionDispatchContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        @Nonnull AntlrService service,
        @Nonnull AntlrProjection projection
    ) {
        super(elementContext, compilationUnit);
        this.service = Objects.requireNonNull(service);
        this.projection = Objects.requireNonNull(projection);
    }

    @Nonnull
    @Override
    public Optional<IAntlrElement> getSurroundingElement() {
        return Optional.of(this.service);
    }

    @Override
    public boolean isContext() {
        return true;
    }

    @Override
    public Pair<Token, Token> getContextBefore() {
        return this.getEntireContext();
    }

    @Nonnull
    public AntlrProjection getProjection() {
        return this.projection;
    }

    // <editor-fold desc="Report Compiler Errors">
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (this.projection == AntlrProjection.NOT_FOUND) {
            ProjectionReferenceContext reference = this.getElementContext().projectionReference();

            compilerAnnotationHolder.add(
                "ERR_SER_PRJ",
                String.format("Cannot find projection '%s'", reference.getText()),
                this,
                reference
            );
        }

        AntlrClassifier projectionClassifier = this.projection.getClassifier();
        if (projectionClassifier == AntlrClass.NOT_FOUND || projectionClassifier == AntlrClass.AMBIGUOUS) {
            throw new AssertionError();
        }

        if (projectionClassifier == AntlrClassifier.NOT_FOUND || projectionClassifier == AntlrClassifier.AMBIGUOUS) {
            return;
        }

        AntlrClass serviceGroupKlass = this.service.getUrl().getServiceGroup().getKlass();
        if (serviceGroupKlass == AntlrClass.AMBIGUOUS || serviceGroupKlass == AntlrClass.NOT_FOUND) {
            return;
        }

        if (serviceGroupKlass == AntlrClassifier.NOT_FOUND || serviceGroupKlass == AntlrClassifier.AMBIGUOUS) {
            throw new AssertionError();
        }

        if (serviceGroupKlass != projectionClassifier && !serviceGroupKlass.isSubTypeOf(projectionClassifier)) {
            String error = String.format(
                "Expected projection referencing '%s' but projection '%s' references '%s'.",
                serviceGroupKlass.getName(),
                this.projection.getName(),
                projectionClassifier.getName()
            );
            compilerAnnotationHolder.add("ERR_SRV_PRJ", error, this, this.getElementContext().projectionReference());
        }

        this.reportForwardReference(compilerAnnotationHolder);
    }

    private void reportForwardReference(CompilerAnnotationHolder compilerAnnotationHolder) {
        if (!this.isForwardReference(this.projection)) {
            return;
        }

        String message = String.format(
            "Service group '%s' is declared on line %d and has a forward reference to projection '%s' which is declared later in the source file '%s' on line %d.",
            this,
            this.getElementContext().getStart().getLine(),
            this.projection.getName(),
            this.getCompilationUnit().get().getSourceName(),
            this.projection.getElementContext().getStart().getLine()
        );
        compilerAnnotationHolder.add("ERR_FWD_REF", message, this, this.getElementContext().projectionReference());
    }

    // </editor-fold>

    @Nonnull
    @Override
    public ServiceProjectionDispatchContext getElementContext() {
        return (ServiceProjectionDispatchContext) super.getElementContext();
    }

    @Nonnull
    public ServiceProjectionDispatchBuilder build() {
        if (this.elementBuilder != null) {
            throw new IllegalStateException();
        }
        this.elementBuilder = new ServiceProjectionDispatchBuilder(
            (ServiceProjectionDispatchContext) this.elementContext,
            this.getMacroElementBuilder(),
            this.getSourceCodeBuilder(),
            this.projection.getElementBuilder()
        );
        return this.elementBuilder;
    }

    @Override
    @Nonnull
    public ServiceProjectionDispatchBuilder getElementBuilder() {
        return Objects.requireNonNull(this.elementBuilder);
    }
}
