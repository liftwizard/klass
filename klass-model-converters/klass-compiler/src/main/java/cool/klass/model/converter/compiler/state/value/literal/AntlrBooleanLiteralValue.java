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

package cool.klass.model.converter.compiler.state.value.literal;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrPrimitiveType;
import cool.klass.model.converter.compiler.state.AntlrType;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.value.AntlrExpressionValueVisitor;
import cool.klass.model.meta.domain.value.literal.BooleanLiteralValueImpl.BooleanLiteralValueBuilder;
import cool.klass.model.meta.grammar.KlassParser.BooleanLiteralContext;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

public final class AntlrBooleanLiteralValue
        extends AbstractAntlrLiteralValue
{
    private final boolean                    value;
    private       BooleanLiteralValueBuilder elementBuilder;

    public AntlrBooleanLiteralValue(
            @Nonnull BooleanLiteralContext elementContext,
            @Nonnull Optional<CompilationUnit> compilationUnit,
            boolean value,
            @Nonnull IAntlrElement expressionValueOwner)
    {
        super(elementContext, compilationUnit, expressionValueOwner);
        this.value = value;
    }

    @Override
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder)
    {
    }

    @Nonnull
    @Override
    public BooleanLiteralValueBuilder build()
    {
        if (this.elementBuilder != null)
        {
            throw new IllegalStateException();
        }
        this.elementBuilder = new BooleanLiteralValueBuilder(
                (BooleanLiteralContext) this.elementContext,
                this.getMacroElementBuilder(),
                this.getSourceCodeBuilder(),
                this.value);
        return this.elementBuilder;
    }

    @Nonnull
    @Override
    public BooleanLiteralValueBuilder getElementBuilder()
    {
        return Objects.requireNonNull(this.elementBuilder);
    }

    @Nonnull
    @Override
    public ImmutableList<AntlrType> getPossibleTypes()
    {
        return Lists.immutable.with(AntlrPrimitiveType.BOOLEAN);
    }

    @Override
    public void visit(AntlrExpressionValueVisitor visitor)
    {
        visitor.visitBooleanLiteral(this);
    }
}
