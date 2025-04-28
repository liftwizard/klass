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

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.state.value.literal.AntlrBooleanLiteralValue;
import cool.klass.model.converter.compiler.state.value.literal.AntlrFloatingPointLiteralValue;
import cool.klass.model.converter.compiler.state.value.literal.AntlrIntegerLiteralValue;
import cool.klass.model.converter.compiler.state.value.literal.AntlrLiteralListValue;
import cool.klass.model.converter.compiler.state.value.literal.AntlrNullLiteral;
import cool.klass.model.converter.compiler.state.value.literal.AntlrStringLiteralValue;
import cool.klass.model.converter.compiler.state.value.literal.AntlrUserLiteral;

public interface AntlrExpressionValueVisitor {
    void visitTypeMember(@Nonnull AntlrTypeMemberReferencePath typeMemberExpressionValue);

    void visitThisMember(@Nonnull AntlrThisMemberReferencePath thisMemberExpressionValue);

    void visitParameterReference(@Nonnull AntlrParameterReference parameterReference);

    void visitBooleanLiteral(@Nonnull AntlrBooleanLiteralValue booleanLiteralValue);

    void visitIntegerLiteral(@Nonnull AntlrIntegerLiteralValue integerLiteralValue);

    void visitFloatingPointLiteral(@Nonnull AntlrFloatingPointLiteralValue floatingPointLiteral);

    void visitStringLiteral(@Nonnull AntlrStringLiteralValue stringLiteralValue);

    void visitLiteralList(@Nonnull AntlrLiteralListValue literalListValue);

    void visitUserLiteral(@Nonnull AntlrUserLiteral userLiteral);

    void visitNullLiteral(@Nonnull AntlrNullLiteral nullLiteral);
}
