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

package cool.klass.model.converter.compiler.state.value;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrEnumeration;
import cool.klass.model.converter.compiler.state.AntlrType;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.parameter.AntlrParameter;
import cool.klass.model.meta.domain.value.ParameterReferenceImpl.ParameterReferenceBuilder;
import cool.klass.model.meta.grammar.KlassParser.ParameterReferenceContext;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.OrderedMap;

public class AntlrParameterReference extends AntlrExpressionValue {

    @Nonnull
    private final String variableName;

    @Nullable
    private AntlrParameter antlrParameter;

    private ParameterReferenceBuilder elementBuilder;

    public AntlrParameterReference(
        @Nonnull ParameterReferenceContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        @Nonnull String variableName,
        @Nonnull IAntlrElement expressionValueOwner
    ) {
        super(elementContext, compilationUnit, expressionValueOwner);
        this.variableName = Objects.requireNonNull(variableName);
    }

    @Nullable
    public AntlrParameter getAntlrParameter() {
        return this.antlrParameter;
    }

    @Nonnull
    @Override
    public ParameterReferenceBuilder build() {
        if (this.elementBuilder != null) {
            throw new IllegalStateException();
        }
        this.elementBuilder = new ParameterReferenceBuilder(
            (ParameterReferenceContext) this.elementContext,
            this.getMacroElementBuilder(),
            this.getSourceCodeBuilder(),
            this.antlrParameter.getElementBuilder()
        );
        return this.elementBuilder;
    }

    @Nonnull
    @Override
    public ParameterReferenceBuilder getElementBuilder() {
        return Objects.requireNonNull(this.elementBuilder);
    }

    @Override
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (this.antlrParameter == AntlrParameter.AMBIGUOUS) {
            return;
        }

        if (this.antlrParameter == AntlrParameter.NOT_FOUND) {
            String message = String.format("Cannot find parameter '%s'.", this.elementContext.getText());
            compilerAnnotationHolder.add("ERR_VAR_REF", message, this);
        }
    }

    @Nonnull
    @Override
    public ImmutableList<AntlrType> getPossibleTypes() {
        Objects.requireNonNull(this.antlrParameter);
        AntlrType type = this.antlrParameter.getType();
        return type == AntlrEnumeration.NOT_FOUND || type == AntlrEnumeration.AMBIGUOUS
            ? Lists.immutable.empty()
            : type.getPotentialWiderTypes();
    }

    @Override
    public void resolveServiceVariables(@Nonnull OrderedMap<String, AntlrParameter> formalParametersByName) {
        this.antlrParameter = formalParametersByName.getIfAbsentValue(this.variableName, AntlrParameter.NOT_FOUND);
    }

    @Override
    public void visit(AntlrExpressionValueVisitor visitor) {
        visitor.visitParameterReference(this);
    }
}
