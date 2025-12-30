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

package cool.klass.model.meta.domain.api.value;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.value.literal.BooleanLiteralValue;
import cool.klass.model.meta.domain.api.value.literal.FloatingPointLiteralValue;
import cool.klass.model.meta.domain.api.value.literal.IntegerLiteralValue;
import cool.klass.model.meta.domain.api.value.literal.LiteralListValue;
import cool.klass.model.meta.domain.api.value.literal.NullLiteral;
import cool.klass.model.meta.domain.api.value.literal.StringLiteralValue;
import cool.klass.model.meta.domain.api.value.literal.UserLiteral;

public interface ExpressionValueVisitor {
	void visitTypeMember(@Nonnull TypeMemberReferencePath typeMemberExpressionValue);

	void visitThisMember(@Nonnull ThisMemberReferencePath thisMemberExpressionValue);

	void visitParameterReference(@Nonnull ParameterReference parameterReference);

	void visitBooleanLiteral(@Nonnull BooleanLiteralValue booleanLiteralValue);

	void visitIntegerLiteral(@Nonnull IntegerLiteralValue integerLiteralValue);

	void visitFloatingPointLiteral(@Nonnull FloatingPointLiteralValue floatingPointLiteralValue);

	void visitStringLiteral(@Nonnull StringLiteralValue stringLiteralValue);

	void visitLiteralList(@Nonnull LiteralListValue literalListValue);

	void visitUserLiteral(@Nonnull UserLiteral userLiteral);

	void visitNullLiteral(@Nonnull NullLiteral nullLiteral);
}
