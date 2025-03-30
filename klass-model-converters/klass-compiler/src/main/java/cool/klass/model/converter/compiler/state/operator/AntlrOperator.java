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

package cool.klass.model.converter.compiler.state.operator;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrElement;
import cool.klass.model.converter.compiler.state.AntlrType;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.criteria.OperatorAntlrCriteria;
import cool.klass.model.meta.domain.operator.AbstractOperator.AbstractOperatorBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ListIterable;

public abstract class AntlrOperator extends AntlrElement {

    protected final String operatorText;
    protected OperatorAntlrCriteria owningOperatorAntlrCriteria;

    protected AntlrOperator(
        @Nonnull ParserRuleContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        @Nonnull String operatorText
    ) {
        super(elementContext, compilationUnit);
        this.operatorText = Objects.requireNonNull(operatorText);
    }

    public void setOwningOperatorAntlrCriteria(OperatorAntlrCriteria operatorAntlrCriteria) {
        this.owningOperatorAntlrCriteria = Objects.requireNonNull(operatorAntlrCriteria);
    }

    @Nonnull
    @Override
    public Optional<IAntlrElement> getSurroundingElement() {
        return Optional.of(this.owningOperatorAntlrCriteria);
    }

    @Nonnull
    public abstract AbstractOperatorBuilder<?> build();

    @Override
    @Nonnull
    public abstract AbstractOperatorBuilder<?> getElementBuilder();

    public abstract void checkTypes(
        CompilerAnnotationHolder compilerAnnotationHolder,
        ListIterable<AntlrType> sourceTypes,
        ListIterable<AntlrType> targetTypes
    );
}
