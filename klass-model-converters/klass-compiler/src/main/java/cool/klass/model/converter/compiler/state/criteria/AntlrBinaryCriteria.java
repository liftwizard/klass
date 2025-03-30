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

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.parameter.AntlrParameter;
import cool.klass.model.meta.domain.criteria.AbstractBinaryCriteria.AbstractBinaryCriteriaBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.map.OrderedMap;

public abstract class AntlrBinaryCriteria extends AntlrCriteria {

    protected AntlrCriteria left;
    protected AntlrCriteria right;

    protected AntlrBinaryCriteria(
        @Nonnull ParserRuleContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        @Nonnull IAntlrElement criteriaOwner
    ) {
        super(elementContext, compilationUnit, criteriaOwner);
    }

    @Nonnull
    @Override
    public abstract AbstractBinaryCriteriaBuilder<?> build();

    @Nonnull
    @Override
    public abstract AbstractBinaryCriteriaBuilder<?> getElementBuilder();

    public void setLeft(@Nonnull AntlrCriteria left) {
        if (this.left != null) {
            throw new IllegalStateException();
        }
        this.left = Objects.requireNonNull(left);
    }

    public void setRight(@Nonnull AntlrCriteria right) {
        if (this.right != null) {
            throw new IllegalStateException();
        }
        this.right = Objects.requireNonNull(right);
    }

    @Override
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        // TODO: Error if both clauses are identical, or if any left true subclause is a subclause of the right
        // Java | Probable bugs | Constant conditions & exceptions

        this.left.reportErrors(compilerAnnotationHolder);
        this.right.reportErrors(compilerAnnotationHolder);
    }

    @Override
    public final void resolveServiceVariables(@Nonnull OrderedMap<String, AntlrParameter> formalParametersByName) {
        this.left.resolveServiceVariables(formalParametersByName);
        this.right.resolveServiceVariables(formalParametersByName);
    }

    @Override
    public final void resolveTypes() {
        this.left.resolveTypes();
        this.right.resolveTypes();
    }
}
