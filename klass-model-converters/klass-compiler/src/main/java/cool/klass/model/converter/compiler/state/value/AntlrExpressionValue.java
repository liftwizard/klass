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

package cool.klass.model.converter.compiler.state.value;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrElement;
import cool.klass.model.converter.compiler.state.AntlrType;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.parameter.AntlrParameter;
import cool.klass.model.meta.domain.value.AbstractExpressionValue.AbstractExpressionValueBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.OrderedMap;

public abstract class AntlrExpressionValue extends AntlrElement {

    private final IAntlrElement expressionValueOwner;

    protected AntlrExpressionValue(
        @Nonnull ParserRuleContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        @Nonnull IAntlrElement expressionValueOwner
    ) {
        super(elementContext, compilationUnit);
        this.expressionValueOwner = Objects.requireNonNull(expressionValueOwner);
    }

    @Nonnull
    @Override
    public Optional<IAntlrElement> getSurroundingElement() {
        return Optional.of(this.expressionValueOwner);
    }

    @Nonnull
    public abstract AbstractExpressionValueBuilder<?> build();

    @Override
    @Nonnull
    public abstract AbstractExpressionValueBuilder<?> getElementBuilder();

    public abstract void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder);

    @Nonnull
    public abstract ImmutableList<AntlrType> getPossibleTypes();

    public void resolveServiceVariables(@Nonnull OrderedMap<String, AntlrParameter> formalParametersByName) {
        // Intentionally blank
    }

    public abstract void visit(AntlrExpressionValueVisitor visitor);
}
