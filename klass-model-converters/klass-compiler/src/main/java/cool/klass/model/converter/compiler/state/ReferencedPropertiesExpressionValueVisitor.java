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

package cool.klass.model.converter.compiler.state;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.state.parameter.AntlrParameter;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEnd;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.value.AntlrExpressionValueVisitor;
import cool.klass.model.converter.compiler.state.value.AntlrParameterReference;
import cool.klass.model.converter.compiler.state.value.AntlrThisMemberReferencePath;
import cool.klass.model.converter.compiler.state.value.AntlrTypeMemberReferencePath;
import cool.klass.model.converter.compiler.state.value.literal.AntlrBooleanLiteralValue;
import cool.klass.model.converter.compiler.state.value.literal.AntlrFloatingPointLiteralValue;
import cool.klass.model.converter.compiler.state.value.literal.AntlrIntegerLiteralValue;
import cool.klass.model.converter.compiler.state.value.literal.AntlrLiteralListValue;
import cool.klass.model.converter.compiler.state.value.literal.AntlrNullLiteral;
import cool.klass.model.converter.compiler.state.value.literal.AntlrStringLiteralValue;
import cool.klass.model.converter.compiler.state.value.literal.AntlrUserLiteral;
import org.eclipse.collections.api.list.ImmutableList;

public class ReferencedPropertiesExpressionValueVisitor implements AntlrExpressionValueVisitor {

    private final Set<AntlrAssociationEnd> associationEndsReferencedByCriteria = new LinkedHashSet<>();
    private final Set<AntlrDataTypeProperty<?>> dataTypePropertiesReferencedByCriteria = new LinkedHashSet<>();
    private final Set<AntlrParameter> parametersReferencedByCriteria = new LinkedHashSet<>();

    public Set<AntlrAssociationEnd> getAssociationEndsReferencedByCriteria() {
        return this.associationEndsReferencedByCriteria;
    }

    public Set<AntlrDataTypeProperty<?>> getDataTypePropertiesReferencedByCriteria() {
        return this.dataTypePropertiesReferencedByCriteria;
    }

    public Set<AntlrParameter> getParametersReferencedByCriteria() {
        return this.parametersReferencedByCriteria;
    }

    @Override
    public void visitTypeMember(@Nonnull AntlrTypeMemberReferencePath typeMemberExpressionValue) {
        ImmutableList<AntlrAssociationEnd> associationEnds = typeMemberExpressionValue.getAssociationEnds();
        this.associationEndsReferencedByCriteria.addAll(associationEnds.castToList());
        AntlrDataTypeProperty<?> dataTypeProperty = typeMemberExpressionValue.getDataTypeProperty();
        this.dataTypePropertiesReferencedByCriteria.add(dataTypeProperty);
    }

    @Override
    public void visitThisMember(@Nonnull AntlrThisMemberReferencePath thisMemberExpressionValue) {
        ImmutableList<AntlrAssociationEnd> associationEnds = thisMemberExpressionValue.getAssociationEnds();
        this.associationEndsReferencedByCriteria.addAll(associationEnds.castToList());
        AntlrDataTypeProperty<?> dataTypeProperty = thisMemberExpressionValue.getDataTypeProperty();
        this.dataTypePropertiesReferencedByCriteria.add(dataTypeProperty);
    }

    @Override
    public void visitParameterReference(@Nonnull AntlrParameterReference parameterReference) {
        AntlrParameter antlrParameter = parameterReference.getAntlrParameter();
        this.parametersReferencedByCriteria.add(antlrParameter);
    }

    @Override
    public void visitBooleanLiteral(@Nonnull AntlrBooleanLiteralValue booleanLiteralValue) {}

    @Override
    public void visitIntegerLiteral(@Nonnull AntlrIntegerLiteralValue integerLiteralValue) {}

    @Override
    public void visitFloatingPointLiteral(@Nonnull AntlrFloatingPointLiteralValue floatingPointLiteral) {}

    @Override
    public void visitStringLiteral(@Nonnull AntlrStringLiteralValue stringLiteralValue) {}

    @Override
    public void visitLiteralList(@Nonnull AntlrLiteralListValue literalListValue) {}

    @Override
    public void visitUserLiteral(@Nonnull AntlrUserLiteral userLiteral) {}

    @Override
    public void visitNullLiteral(@Nonnull AntlrNullLiteral nullLiteral) {}
}
