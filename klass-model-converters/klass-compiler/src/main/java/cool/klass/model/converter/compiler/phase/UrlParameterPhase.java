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

package cool.klass.model.converter.compiler.phase;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.AntlrEnumeration;
import cool.klass.model.converter.compiler.state.AntlrMultiplicity;
import cool.klass.model.converter.compiler.state.AntlrMultiplicityOwner;
import cool.klass.model.converter.compiler.state.AntlrPrimitiveType;
import cool.klass.model.converter.compiler.state.AntlrType;
import cool.klass.model.converter.compiler.state.parameter.AntlrParameter;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import cool.klass.model.converter.compiler.state.service.url.AntlrUrl;
import cool.klass.model.converter.compiler.state.service.url.AntlrUrlConstant;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.grammar.KlassParser.EnumerationParameterDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.MultiplicityContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterModifierContext;
import cool.klass.model.meta.grammar.KlassParser.PrimitiveParameterDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.QueryParameterListContext;
import cool.klass.model.meta.grammar.KlassParser.UrlConstantContext;
import cool.klass.model.meta.grammar.KlassParser.UrlDeclarationContext;
import org.antlr.v4.runtime.ParserRuleContext;

public class UrlParameterPhase extends AbstractCompilerPhase {

	@Nullable
	private Boolean inQueryParameterList;

	@Nullable
	private AntlrParameter parameter;

	@Nullable
	private AntlrMultiplicityOwner multiplicityOwner;

	public UrlParameterPhase(@Nonnull CompilerState compilerState) {
		super(compilerState);
	}

	@Override
	public void enterUrlDeclaration(@Nonnull UrlDeclarationContext ctx) {
		super.enterUrlDeclaration(ctx);
		this.inQueryParameterList = false;
	}

	@Override
	public void exitUrlDeclaration(@Nonnull UrlDeclarationContext ctx) {
		this.inQueryParameterList = null;
		super.exitUrlDeclaration(ctx);
	}

	@Override
	public void enterUrlConstant(@Nonnull UrlConstantContext ctx) {
		super.enterUrlConstant(ctx);

		AntlrUrl url = this.compilerState.getCompilerWalk().getUrl();
		AntlrUrlConstant antlrUrlConstant = new AntlrUrlConstant(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			url.getNumPathSegments() + 1
		);
		url.enterUrlConstant(antlrUrlConstant);
	}

	@Override
	public void enterQueryParameterList(@Nonnull QueryParameterListContext ctx) {
		super.enterQueryParameterList(ctx);
		this.inQueryParameterList = true;
	}

	@Override
	public void enterPrimitiveParameterDeclaration(@Nonnull PrimitiveParameterDeclarationContext ctx) {
		super.enterPrimitiveParameterDeclaration(ctx);

		if (this.compilerState.getCompilerWalk().getUrl() == null) {
			return;
		}

		String primitiveTypeName = ctx.primitiveType().getText();
		PrimitiveType primitiveType = PrimitiveType.byPrettyName(primitiveTypeName);
		AntlrPrimitiveType primitiveTypeState = AntlrPrimitiveType.valueOf(primitiveType);

		this.enterParameterDeclaration(ctx, primitiveTypeState, ctx.identifier());
	}

	@Override
	public void exitPrimitiveParameterDeclaration(@Nonnull PrimitiveParameterDeclarationContext ctx) {
		this.parameter = null;
		this.multiplicityOwner = null;

		super.exitPrimitiveParameterDeclaration(ctx);
	}

	@Override
	public void enterEnumerationParameterDeclaration(@Nonnull EnumerationParameterDeclarationContext ctx) {
		super.enterEnumerationParameterDeclaration(ctx);

		if (this.compilerState.getCompilerWalk().getUrl() == null) {
			return;
		}

		String enumerationName = ctx.enumerationReference().getText();
		AntlrEnumeration enumeration = this.compilerState.getDomainModel().getEnumerationByName(enumerationName);

		this.enterParameterDeclaration(ctx, enumeration, ctx.identifier());
	}

	@Override
	public void exitEnumerationParameterDeclaration(@Nonnull EnumerationParameterDeclarationContext ctx) {
		this.parameter = null;
		this.multiplicityOwner = null;

		super.exitEnumerationParameterDeclaration(ctx);
	}

	@Override
	public void enterParameterModifier(@Nonnull ParameterModifierContext ctx) {
		super.enterParameterModifier(ctx);

		// TODO: Check if parameter is non null?
		int ordinal = this.parameter.getNumModifiers();
		AntlrModifier modifier = new AntlrModifier(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			ordinal,
			this.parameter
		);
		this.parameter.enterModifier(modifier);
	}

	private void enterParameterDeclaration(
		@Nonnull ParserRuleContext ctx,
		@Nonnull AntlrType type,
		@Nonnull IdentifierContext identifierContext
	) {
		if (this.parameter != null) {
			throw new AssertionError();
		}

		AntlrUrl url = this.compilerState.getCompilerWalk().getUrl();
		int ordinal = this.inQueryParameterList ? url.getNumQueryParameters() + 1 : url.getNumPathSegments() + 1;

		this.parameter = new AntlrParameter(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			ordinal,
			identifierContext,
			type,
			url
		);
		this.multiplicityOwner = this.parameter;

		if (this.inQueryParameterList) {
			url.enterQueryParameterDeclaration(this.parameter);
		} else {
			url.enterPathParameterDeclaration(this.parameter);
		}
	}

	@Override
	public void enterMultiplicity(@Nonnull MultiplicityContext ctx) {
		super.enterMultiplicity(ctx);

		if (this.multiplicityOwner != null) {
			AntlrMultiplicity multiplicity = new AntlrMultiplicity(
				ctx,
				Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
				this.multiplicityOwner
			);

			this.multiplicityOwner.enterMultiplicity(multiplicity);
		}
	}
}
