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

package cool.klass.model.converter.compiler.state.value.literal;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrType;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.value.AntlrExpressionValue;
import cool.klass.model.converter.compiler.state.value.AntlrExpressionValueVisitor;
import cool.klass.model.meta.domain.value.literal.AbstractLiteralValue.AbstractLiteralValueBuilder;
import cool.klass.model.meta.domain.value.literal.LiteralListValueImpl.LiteralListValueBuilder;
import cool.klass.model.meta.grammar.KlassParser.LiteralListContext;
import org.eclipse.collections.api.list.ImmutableList;

public class AntlrLiteralListValue extends AbstractAntlrLiteralValue {

    private ImmutableList<AbstractAntlrLiteralValue> literals;
    private LiteralListValueBuilder elementBuilder;

    public AntlrLiteralListValue(
        @Nonnull LiteralListContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        @Nonnull IAntlrElement expressionValueOwner
    ) {
        super(elementContext, compilationUnit, expressionValueOwner);
    }

    public void setLiterals(ImmutableList<AbstractAntlrLiteralValue> literals) {
        if (this.literals != null) {
            throw new IllegalStateException();
        }
        this.literals = Objects.requireNonNull(literals);
    }

    @Nonnull
    @Override
    public LiteralListValueBuilder build() {
        if (this.elementBuilder != null) {
            throw new IllegalStateException();
        }
        this.elementBuilder = new LiteralListValueBuilder(
            (LiteralListContext) this.elementContext,
            this.getMacroElementBuilder(),
            this.getSourceCodeBuilder(),
            this.getInferredType().getTypeGetter()
        );

        ImmutableList<AbstractLiteralValueBuilder<?>> literalValueBuilders = this.literals.<
            AbstractLiteralValueBuilder<?>
        >collect(AbstractAntlrLiteralValue::build).toImmutable();
        this.elementBuilder.setLiteralValueBuilders(literalValueBuilders);

        return this.elementBuilder;
    }

    @Nonnull
    @Override
    public LiteralListValueBuilder getElementBuilder() {
        return Objects.requireNonNull(this.elementBuilder);
    }

    @Override
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (this.getPossibleTypes().isEmpty()) {
            // TODO: Cover this with a test

            compilerAnnotationHolder.add("ERR_LIT_LST", "Literal list with heterogeneous values.", this);
        }
    }

    @Nonnull
    @Override
    public ImmutableList<AntlrType> getPossibleTypes() {
        return this.literals.flatCollect(AntlrExpressionValue::getPossibleTypes)
            .toBag()
            .selectByOccurrences(occurrences -> occurrences == this.literals.size())
            .toList()
            .distinct()
            .toImmutable();
    }

    @Override
    public void visit(AntlrExpressionValueVisitor visitor) {
        visitor.visitLiteralList(this);
    }
}
