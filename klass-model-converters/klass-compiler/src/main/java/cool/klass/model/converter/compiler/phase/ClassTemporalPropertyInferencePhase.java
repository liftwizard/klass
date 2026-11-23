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
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;

public class ClassTemporalPropertyInferencePhase
	extends AbstractClassifierPropertyMacroPhase
{
	public ClassTemporalPropertyInferencePhase(@Nonnull CompilerState compilerState)
	{
		super(compilerState);
	}

	@Nonnull
	@Override
	public String getName()
	{
		return "Temporal modifier";
	}

	@Override
	protected void runCompilerMacro(ParserRuleContext inPlaceContext)
	{
		AntlrClassifier classifier = this.compilerState.getCompilerWalk().getClassifier();
		MutableList<AntlrModifier> declaredModifiers = classifier.getDeclaredModifiers();
		ImmutableList<AntlrDataTypeProperty<?>> allDataTypeProperties = classifier.getAllDataTypeProperties();

		MutableList<AntlrModifier> validTemporalModifiers = declaredModifiers.select(
			(modifier) -> modifier.is("validTemporal") || modifier.is("bitemporal")
		);
		MutableList<AntlrModifier> systemTemporalModifiers = declaredModifiers.select(
			(modifier) -> modifier.is("systemTemporal") || modifier.is("bitemporal")
		);

		if (validTemporalModifiers.size() == 1)
		{
			AntlrModifier validTemporalModifier = validTemporalModifiers.getOnly();
			var sourceCodeText = new StringBuilder();
			if (allDataTypeProperties.noneSatisfy(AntlrDataTypeProperty::isValidRange))
			{
				sourceCodeText.append("    valid    : TemporalRange?   valid private;\n");
			}
			if (allDataTypeProperties.noneSatisfy(AntlrDataTypeProperty::isValidFrom))
			{
				sourceCodeText.append("    validFrom: TemporalInstant? valid from;\n");
			}
			if (allDataTypeProperties.noneSatisfy(AntlrDataTypeProperty::isValidTo))
			{
				sourceCodeText.append("    validTo  : TemporalInstant? valid to;\n");
			}
			this.runCompilerMacro(inPlaceContext, sourceCodeText.toString(), validTemporalModifier);
		}

		if (systemTemporalModifiers.size() == 1)
		{
			AntlrModifier systemTemporalModifier = systemTemporalModifiers.getOnly();
			var sourceCodeText = new StringBuilder();
			if (allDataTypeProperties.noneSatisfy(AntlrDataTypeProperty::isSystemRange))
			{
				sourceCodeText.append("    system    : TemporalRange?   system private;\n");
			}
			if (allDataTypeProperties.noneSatisfy(AntlrDataTypeProperty::isSystemFrom))
			{
				sourceCodeText.append("    systemFrom: TemporalInstant? system from;\n");
			}
			if (allDataTypeProperties.noneSatisfy(AntlrDataTypeProperty::isSystemTo))
			{
				sourceCodeText.append("    systemTo  : TemporalInstant? system to;\n");
			}
			this.runCompilerMacro(inPlaceContext, sourceCodeText.toString(), systemTemporalModifier);
		}
	}
}
