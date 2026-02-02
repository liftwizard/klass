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

package cool.klass.model.converter.compiler.phase;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.AntlrEnumeration;
import cool.klass.model.converter.compiler.state.AntlrEnumerationLiteral;
import cool.klass.model.meta.grammar.KlassParser.EnumerationDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationPrettyNameContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.antlr.v4.runtime.RuleContext;

public class EnumerationsPhase extends AbstractCompilerPhase {

	@Nullable
	private AntlrEnumeration enumeration;

	public EnumerationsPhase(@Nonnull CompilerState compilerState) {
		super(compilerState);
	}

	@Override
	public void enterEnumerationDeclaration(@Nonnull EnumerationDeclarationContext ctx) {
		super.enterEnumerationDeclaration(ctx);

		IdentifierContext identifier = ctx.identifier();
		this.enumeration = new AntlrEnumeration(
			ctx,
			this.compilerState.getCompilerWalk().getCompilationUnit(),
			this.compilerState.getOrdinal(ctx),
			identifier
		);
	}

	@Override
	public void exitEnumerationDeclaration(@Nonnull EnumerationDeclarationContext ctx) {
		this.compilerState.getDomainModel().exitEnumerationDeclaration(this.enumeration);
		this.enumeration = null;
		super.exitEnumerationDeclaration(ctx);
	}

	@Override
	public void enterEnumerationLiteral(@Nonnull EnumerationLiteralContext ctx) {
		super.enterEnumerationLiteral(ctx);

		Optional<EnumerationPrettyNameContext> prettyNameContext = Optional.ofNullable(ctx.enumerationPrettyName());

		Optional<String> prettyName = prettyNameContext.map(RuleContext::getText).map(this::trimQuotes);

		AntlrEnumerationLiteral enumerationLiteral = new AntlrEnumerationLiteral(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			this.enumeration.getNumLiterals() + 1,
			ctx.identifier(),
			prettyName,
			this.enumeration
		);
		this.enumeration.enterEnumerationLiteral(enumerationLiteral);
	}

	@Nonnull
	private String trimQuotes(@Nonnull String text) {
		return text.substring(1, text.length() - 1);
	}
}
