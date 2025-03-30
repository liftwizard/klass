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

package cool.klass.generator.reladomo;

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.operator.EqualityOperator;
import cool.klass.model.meta.domain.api.operator.InOperator;
import cool.klass.model.meta.domain.api.operator.InequalityOperator;
import cool.klass.model.meta.domain.api.operator.Operator;
import cool.klass.model.meta.domain.api.operator.OperatorVisitor;
import cool.klass.model.meta.domain.api.operator.StringOperator;

class OperatorToRelationshipVisitor implements OperatorVisitor {

    @Nonnull
    private final StringBuilder stringBuilder;

    OperatorToRelationshipVisitor(@Nonnull StringBuilder stringBuilder) {
        this.stringBuilder = Objects.requireNonNull(stringBuilder);
    }

    @Override
    public void visitEquality(@Nonnull EqualityOperator equalityOperator) {
        this.stringBuilder.append(" = ");
    }

    @Override
    public void visitInequality(@Nonnull InequalityOperator inequalityOperator) {
        this.appendOperatorText(inequalityOperator);
    }

    @Override
    public void visitIn(@Nonnull InOperator inOperator) {
        this.appendOperatorText(inOperator);
    }

    @Override
    public void visitString(@Nonnull StringOperator stringOperator) {
        this.appendOperatorText(stringOperator);
    }

    private void appendOperatorText(@Nonnull Operator operator) {
        this.stringBuilder.append(' ');
        this.stringBuilder.append(operator.getOperatorText());
        this.stringBuilder.append(' ');
    }
}
