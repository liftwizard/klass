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

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import cool.klass.model.converter.compiler.state.property.validation.AbstractAntlrPropertyValidation;
import cool.klass.model.meta.grammar.KlassParser;
import cool.klass.model.meta.grammar.KlassParser.ClassifierModifierContext;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;

public class VersionClassInferencePhase extends AbstractCompilerPhase {

	public VersionClassInferencePhase(@Nonnull CompilerState compilerState) {
		super(compilerState);
	}

	@Nonnull
	@Override
	public String getName() {
		return "Version class";
	}

	@Override
	public void enterClassifierModifier(@Nonnull ClassifierModifierContext ctx) {
		super.enterClassifierModifier(ctx);
		String modifierText = ctx.getText();
		if (!"versioned".equals(modifierText)) {
			return;
		}

		String klassSourceCode = this.getSourceCode();

		AntlrModifier classifierModifierState = this.compilerState.getCompilerWalk().getClassifierModifier();

		ImmutableList<ParseTreeListener> compilerPhases = Lists.immutable.with(
			new CompilationUnitPhase(this.compilerState),
			new TopLevelElementsPhase(this.compilerState),
			new ClassifierPhase(this.compilerState),
			new PropertyPhase(this.compilerState)
		);

		this.compilerState.runRootCompilerMacro(
			classifierModifierState,
			this,
			klassSourceCode,
			KlassParser::compilationUnit,
			compilerPhases
		);
	}

	@Nonnull
	private String getSourceCode() {
		AntlrClass klass = this.compilerState.getCompilerWalk().getKlass();
		String propertySourceCode = klass
			.getAllDataTypeProperties()
			.select((property) -> property.isKey() || property.isValid() || property.isSystem() || property.isAudit())
			.collect(this::getSourceCode)
			.collect((each) -> String.format("    %s%n", each))
			.makeString("");

		AntlrModifier auditedModifier = klass.getModifierByName("audited");
		String auditedSourceCode = auditedModifier == AntlrModifier.NOT_FOUND ? "" : " audited";

		// TODO: If main class is transient, version should also be transient, so copy classifier modifiers
		// language=Klass
		return (
			"package "
			+ klass.getPackageName()
			+ "\n"
			+ "\n"
			+ "class "
			+ klass.getName()
			+ "Version systemTemporal"
			+ auditedSourceCode
			+ "\n"
			+ "{\n"
			+ propertySourceCode
			+ "    number: Integer version;\n"
			+ "}\n"
		);
	}

	private String getSourceCode(@Nonnull AntlrDataTypeProperty<?> dataTypeProperty) {
		String isOptionalString = dataTypeProperty.isOptional() ? "?" : "";

		ListIterable<AntlrModifier> modifiers = dataTypeProperty.getModifiers().reject(AntlrModifier::isId);
		String modifierSourceCode = modifiers.isEmpty()
			? ""
			: modifiers.collect(AntlrModifier::getKeyword).makeString(" ", " ", "");

		ListIterable<AbstractAntlrPropertyValidation> validations = dataTypeProperty.getValidations();
		String validationSourceCode = validations.isEmpty() ? "" : validations.makeString(" ", " ", "");

		return String.format(
			"%s: %s%s%s%s;",
			dataTypeProperty.getName(),
			dataTypeProperty.getType().getName(),
			isOptionalString,
			modifierSourceCode,
			validationSourceCode
		);
	}
}
