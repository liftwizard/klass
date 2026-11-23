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

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import cool.klass.model.meta.grammar.KlassParser;
import cool.klass.model.meta.grammar.KlassParser.ClassBodyContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceBodyContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * A phase that, on leaving each class or interface body, may splice generated property declarations into that body.
 */
public abstract class AbstractClassifierPropertyMacroPhase
	extends AbstractCompilerPhase
{
	protected AbstractClassifierPropertyMacroPhase(@Nonnull CompilerState compilerState)
	{
		super(compilerState);
	}

	@Override
	public void exitInterfaceBody(InterfaceBodyContext ctx)
	{
		this.runCompilerMacro(ctx);
		super.exitInterfaceBody(ctx);
	}

	@Override
	public void exitClassBody(ClassBodyContext ctx)
	{
		this.runCompilerMacro(ctx);
		super.exitClassBody(ctx);
	}

	protected abstract void runCompilerMacro(ParserRuleContext inPlaceContext);

	protected void runCompilerMacro(
		ParserRuleContext inPlaceContext,
		@Nonnull String sourceCodeText,
		AntlrModifier macroElement
	)
	{
		if (sourceCodeText.isEmpty())
		{
			return;
		}
		ParseTreeListener compilerPhase = new PropertyPhase(this.compilerState);

		this.compilerState.runInPlaceCompilerMacro(
			macroElement,
			this,
			sourceCodeText,
			KlassParser::classBody,
			inPlaceContext,
			compilerPhase
		);
	}
}
