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

package cool.klass.model.converter.bootstrap.writer;

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.value.ExpressionValue;
import cool.klass.model.meta.domain.api.value.ExpressionValueVisitor;
import cool.klass.model.meta.domain.api.value.ThisMemberReferencePath;
import cool.klass.model.meta.domain.api.value.TypeMemberReferencePath;
import cool.klass.model.meta.domain.api.value.VariableReference;
import cool.klass.model.meta.domain.api.value.literal.BooleanLiteralValue;
import cool.klass.model.meta.domain.api.value.literal.FloatingPointLiteralValue;
import cool.klass.model.meta.domain.api.value.literal.IntegerLiteralValue;
import cool.klass.model.meta.domain.api.value.literal.LiteralListValue;
import cool.klass.model.meta.domain.api.value.literal.NullLiteral;
import cool.klass.model.meta.domain.api.value.literal.StringLiteralValue;
import cool.klass.model.meta.domain.api.value.literal.UserLiteral;
import klass.model.meta.domain.MemberReferencePath;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;

public class BootstrapExpressionValueVisitor2
        implements ExpressionValueVisitor
{
    private final ImmutableMap<ExpressionValue, klass.model.meta.domain.ExpressionValue> expressionValuesByExpressionValue;

    private final MutableList<MemberReferencePath>     bootstrappedMemberReferencePaths     = Lists.mutable.empty();
    private final MutableList<klass.model.meta.domain.TypeMemberReferencePath> bootstrappedTypeMemberReferencePaths = Lists.mutable.empty();
    private final MutableList<klass.model.meta.domain.ThisMemberReferencePath> bootstrappedThisMemberReferencePaths = Lists.mutable.empty();

    public BootstrapExpressionValueVisitor2(ImmutableMap<ExpressionValue, klass.model.meta.domain.ExpressionValue> expressionValuesByExpressionValue)
    {
        this.expressionValuesByExpressionValue = Objects.requireNonNull(expressionValuesByExpressionValue);
    }

    public MutableList<MemberReferencePath> getBootstrappedMemberReferencePaths()
    {
        return this.bootstrappedMemberReferencePaths;
    }

    public MutableList<klass.model.meta.domain.TypeMemberReferencePath> getBootstrappedTypeMemberReferencePaths()
    {
        return this.bootstrappedTypeMemberReferencePaths;
    }

    public MutableList<klass.model.meta.domain.ThisMemberReferencePath> getBootstrappedThisMemberReferencePaths()
    {
        return this.bootstrappedThisMemberReferencePaths;
    }

    @Override
    public void visitTypeMember(@Nonnull TypeMemberReferencePath typeMemberExpressionValue)
    {
        var bootstrappedExpressionValue = this.expressionValuesByExpressionValue.get(typeMemberExpressionValue);

        Klass            klass    = typeMemberExpressionValue.getKlass();
        DataTypeProperty property = typeMemberExpressionValue.getProperty();

        var bootstrappedMemberReferencePath = new MemberReferencePath();
        this.bootstrappedMemberReferencePaths.add(bootstrappedMemberReferencePath);
        bootstrappedMemberReferencePath.setId(bootstrappedExpressionValue.getId());
        bootstrappedMemberReferencePath.setClassName(klass.getName());
        bootstrappedMemberReferencePath.setPropertyClassName(property.getOwningClassifier().getName());
        bootstrappedMemberReferencePath.setPropertyName(property.getName());

        var bootstrappedTypeMemberReferencePath = new klass.model.meta.domain.TypeMemberReferencePath();
        this.bootstrappedTypeMemberReferencePaths.add(bootstrappedTypeMemberReferencePath);
        bootstrappedTypeMemberReferencePath.setId(bootstrappedExpressionValue.getId());

        if (typeMemberExpressionValue.getAssociationEnds().notEmpty())
        {
            throw new AssertionError("TODO");
        }
    }

    @Override
    public void visitThisMember(@Nonnull ThisMemberReferencePath thisMemberExpressionValue)
    {
        var bootstrappedExpressionValue = this.expressionValuesByExpressionValue.get(thisMemberExpressionValue);

        Klass            klass    = thisMemberExpressionValue.getKlass();
        DataTypeProperty property = thisMemberExpressionValue.getProperty();

        var bootstrappedMemberReferencePath = new MemberReferencePath();
        this.bootstrappedMemberReferencePaths.add(bootstrappedMemberReferencePath);
        bootstrappedMemberReferencePath.setId(bootstrappedExpressionValue.getId());
        bootstrappedMemberReferencePath.setClassName(klass.getName());
        bootstrappedMemberReferencePath.setPropertyClassName(property.getOwningClassifier().getName());
        bootstrappedMemberReferencePath.setPropertyName(property.getName());

        var bootstrappedThisMemberReferencePath = new klass.model.meta.domain.ThisMemberReferencePath();
        this.bootstrappedThisMemberReferencePaths.add(bootstrappedThisMemberReferencePath);
        bootstrappedThisMemberReferencePath.setId(bootstrappedExpressionValue.getId());

        if (thisMemberExpressionValue.getAssociationEnds().notEmpty())
        {
            throw new AssertionError("TODO");
        }
    }

    @Override
    public void visitVariableReference(@Nonnull VariableReference variableReference)
    {
        var bootstrappedExpressionValue = this.expressionValuesByExpressionValue.get(variableReference);

        var bootstrappedVariableReference = new klass.model.meta.domain.VariableReference();
        bootstrappedVariableReference.setId(bootstrappedExpressionValue.getId());
        // bootstrappedVariableReference.setParameterId(parameterId);
    }

    @Override
    public void visitBooleanLiteral(@Nonnull BooleanLiteralValue booleanLiteralValue)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName()
                + ".visitBooleanLiteral() not implemented yet");
    }

    @Override
    public void visitIntegerLiteral(@Nonnull IntegerLiteralValue integerLiteralValue)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName()
                + ".visitIntegerLiteral() not implemented yet");
    }

    @Override
    public void visitFloatingPointLiteral(@Nonnull FloatingPointLiteralValue floatingPointLiteralValue)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName()
                + ".visitFloatingPointLiteral() not implemented yet");
    }

    @Override
    public void visitStringLiteral(@Nonnull StringLiteralValue stringLiteralValue)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName()
                + ".visitStringLiteral() not implemented yet");
    }

    @Override
    public void visitLiteralList(@Nonnull LiteralListValue literalListValue)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName()
                + ".visitLiteralList() not implemented yet");
    }

    @Override
    public void visitUserLiteral(@Nonnull UserLiteral userLiteral)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName()
                + ".visitUserLiteral() not implemented yet");
    }

    @Override
    public void visitNullLiteral(@Nonnull NullLiteral nullLiteral)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName()
                + ".visitNullLiteral() not implemented yet");
    }
}
