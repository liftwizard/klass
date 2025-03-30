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

package cool.klass.model.converter.compiler.state.criteria;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrType;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.parameter.AntlrParameter;
import cool.klass.model.converter.compiler.state.value.AntlrMemberReferencePath;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.criteria.EdgePointCriteriaImpl.EdgePointCriteriaBuilder;
import cool.klass.model.meta.grammar.KlassParser.CriteriaEdgePointContext;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.OrderedMap;

public class EdgePointAntlrCriteria extends AntlrCriteria {

    @Nullable
    private AntlrMemberReferencePath memberExpressionValue;

    private EdgePointCriteriaBuilder elementBuilder;

    public EdgePointAntlrCriteria(
        @Nonnull CriteriaEdgePointContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        @Nonnull IAntlrElement criteriaOwner
    ) {
        super(elementContext, compilationUnit, criteriaOwner);
    }

    @Nonnull
    @Override
    public CriteriaEdgePointContext getElementContext() {
        return (CriteriaEdgePointContext) super.getElementContext();
    }

    @Nonnull
    public AntlrMemberReferencePath getMemberExpressionValue() {
        return Objects.requireNonNull(this.memberExpressionValue);
    }

    public void setMemberExpressionValue(@Nullable AntlrMemberReferencePath memberExpressionValue) {
        if (this.memberExpressionValue != null) {
            throw new IllegalStateException();
        }
        this.memberExpressionValue = Objects.requireNonNull(memberExpressionValue);
    }

    @Nonnull
    @Override
    public EdgePointCriteriaBuilder build() {
        if (this.elementBuilder != null) {
            throw new IllegalStateException();
        }
        this.elementBuilder = new EdgePointCriteriaBuilder(
            (CriteriaEdgePointContext) this.elementContext,
            this.getMacroElementBuilder(),
            this.getSourceCodeBuilder(),
            this.memberExpressionValue.build()
        );
        return this.elementBuilder;
    }

    @Nonnull
    @Override
    public EdgePointCriteriaBuilder getElementBuilder() {
        return Objects.requireNonNull(this.elementBuilder);
    }

    @Override
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        this.memberExpressionValue.reportErrors(compilerAnnotationHolder);
        ListIterable<AntlrType> possibleTypes = this.memberExpressionValue.getPossibleTypes();
        if (possibleTypes.anySatisfy(each -> each.getTypeGetter() == PrimitiveType.TEMPORAL_RANGE)) {
            return;
        }

        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".reportErrors() not implemented yet"
        );
    }

    @Override
    public void resolveServiceVariables(@Nonnull OrderedMap<String, AntlrParameter> formalParametersByName) {
        // Intentionally blank
    }

    @Override
    public void resolveTypes() {
        // Intentionally blank
    }

    @Override
    public void visit(AntlrCriteriaVisitor visitor) {
        visitor.visitEdgePoint(this);
    }
}
