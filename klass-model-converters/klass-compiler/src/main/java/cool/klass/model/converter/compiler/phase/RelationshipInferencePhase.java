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
import cool.klass.model.converter.compiler.state.AntlrAssociation;
import cool.klass.model.meta.grammar.KlassParser;
import cool.klass.model.meta.grammar.KlassParser.AssociationBodyContext;
import org.antlr.v4.runtime.tree.ParseTreeListener;

public class RelationshipInferencePhase
	extends AbstractCompilerPhase
{
	public RelationshipInferencePhase(@Nonnull CompilerState compilerState)
	{
		super(compilerState);
	}

	@Nonnull
	@Override
	public String getName()
	{
		return "Association relationship";
	}

	@Override
	public void exitAssociationBody(AssociationBodyContext ctx)
	{
		this.runCompilerMacro(ctx);
		super.exitAssociationBody(ctx);
	}

	private void runCompilerMacro(AssociationBodyContext inPlaceContext)
	{
		if (inPlaceContext.relationship() != null)
		{
			return;
		}

		AntlrAssociation association = this.compilerState.getCompilerWalk().getAssociation();
		association
			.getInferredRelationshipText()
			.ifPresent((text) -> this.runInPlaceCompilerMacro(inPlaceContext, "    relationship " + text));
	}

	private void runInPlaceCompilerMacro(AssociationBodyContext inPlaceContext, @Nonnull String sourceCodeText)
	{
		AntlrAssociation association = this.compilerState.getCompilerWalk().getAssociation();

		ParseTreeListener compilerPhase = new RelationshipPhase(this.compilerState);

		this.compilerState.runInPlaceCompilerMacro(
			association,
			this,
			sourceCodeText,
			KlassParser::relationship,
			inPlaceContext,
			compilerPhase
		);
	}
}
