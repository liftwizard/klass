/*
 * Copyright 2026 Craig Motlin
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

package cool.klass.model.converter.compiler.phase.criteria;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.value.literal.AbstractAntlrLiteralValue;
import cool.klass.model.converter.compiler.state.value.literal.AntlrBooleanLiteralValue;
import cool.klass.model.converter.compiler.state.value.literal.AntlrFloatingPointLiteralValue;
import cool.klass.model.converter.compiler.state.value.literal.AntlrIntegerLiteralValue;
import cool.klass.model.converter.compiler.state.value.literal.AntlrNullLiteral;
import cool.klass.model.converter.compiler.state.value.literal.AntlrStringLiteralValue;
import cool.klass.model.meta.grammar.KlassBaseVisitor;
import cool.klass.model.meta.grammar.KlassParser.BooleanLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.CharacterLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.FloatingPointLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.IntegerLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.NullLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.StringLiteralContext;

public class LiteralValueVisitor extends KlassBaseVisitor<AbstractAntlrLiteralValue> {

	@Nonnull
	private final CompilerState compilerState;

	@Nonnull
	private final IAntlrElement expressionValueOwner;

	public LiteralValueVisitor(@Nonnull CompilerState compilerState, @Nonnull IAntlrElement expressionValueOwner) {
		this.compilerState = Objects.requireNonNull(compilerState);
		this.expressionValueOwner = Objects.requireNonNull(expressionValueOwner);
	}

	@Nonnull
	@Override
	public AntlrIntegerLiteralValue visitIntegerLiteral(@Nonnull IntegerLiteralContext ctx) {
		long value = Long.parseLong(ctx.getText());
		return new AntlrIntegerLiteralValue(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			value,
			this.expressionValueOwner
		);
	}

	@Nonnull
	@Override
	public AntlrFloatingPointLiteralValue visitFloatingPointLiteral(FloatingPointLiteralContext ctx) {
		double value = Double.parseDouble(ctx.getText());
		return new AntlrFloatingPointLiteralValue(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			value,
			this.expressionValueOwner
		);
	}

	@Nonnull
	@Override
	public AntlrBooleanLiteralValue visitBooleanLiteral(BooleanLiteralContext ctx) {
		boolean value = Boolean.parseBoolean(ctx.getText());
		return new AntlrBooleanLiteralValue(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			value,
			this.expressionValueOwner
		);
	}

	@Nonnull
	@Override
	public AbstractAntlrLiteralValue visitCharacterLiteral(CharacterLiteralContext ctx) {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".visitCharacterLiteral() not implemented yet"
		);
	}

	@Nonnull
	@Override
	public AntlrStringLiteralValue visitStringLiteral(@Nonnull StringLiteralContext ctx) {
		String quotedText = ctx.getText();
		String text = quotedText.substring(1, quotedText.length() - 1);
		return new AntlrStringLiteralValue(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			text,
			this.expressionValueOwner
		);
	}

	@Nonnull
	@Override
	public AntlrNullLiteral visitNullLiteral(@Nonnull NullLiteralContext ctx) {
		return new AntlrNullLiteral(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			this.expressionValueOwner
		);
	}
}
