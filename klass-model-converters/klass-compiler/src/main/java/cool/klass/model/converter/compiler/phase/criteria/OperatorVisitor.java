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

package cool.klass.model.converter.compiler.phase.criteria;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.operator.AntlrEqualityOperator;
import cool.klass.model.converter.compiler.state.operator.AntlrInOperator;
import cool.klass.model.converter.compiler.state.operator.AntlrInequalityOperator;
import cool.klass.model.converter.compiler.state.operator.AntlrOperator;
import cool.klass.model.converter.compiler.state.operator.AntlrStringOperator;
import cool.klass.model.meta.grammar.KlassBaseVisitor;
import cool.klass.model.meta.grammar.KlassParser.EqualityOperatorContext;
import cool.klass.model.meta.grammar.KlassParser.InOperatorContext;
import cool.klass.model.meta.grammar.KlassParser.InequalityOperatorContext;
import cool.klass.model.meta.grammar.KlassParser.StringOperatorContext;
import org.antlr.v4.runtime.tree.TerminalNode;

public class OperatorVisitor extends KlassBaseVisitor<AntlrOperator> {

	private final CompilerState compilerState;

	public OperatorVisitor(CompilerState compilerState) {
		this.compilerState = Objects.requireNonNull(compilerState);
	}

	@Nonnull
	@Override
	public AntlrOperator visitTerminal(TerminalNode node) {
		throw new AssertionError();
	}

	@Nonnull
	@Override
	public AntlrEqualityOperator visitEqualityOperator(@Nonnull EqualityOperatorContext ctx) {
		return new AntlrEqualityOperator(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			ctx.getText()
		);
	}

	@Nonnull
	@Override
	public AntlrInequalityOperator visitInequalityOperator(@Nonnull InequalityOperatorContext ctx) {
		return new AntlrInequalityOperator(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			ctx.getText()
		);
	}

	@Nonnull
	@Override
	public AntlrInOperator visitInOperator(@Nonnull InOperatorContext ctx) {
		return new AntlrInOperator(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			ctx.getText()
		);
	}

	@Nonnull
	@Override
	public AntlrStringOperator visitStringOperator(@Nonnull StringOperatorContext ctx) {
		return new AntlrStringOperator(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			ctx.getText()
		);
	}
}
