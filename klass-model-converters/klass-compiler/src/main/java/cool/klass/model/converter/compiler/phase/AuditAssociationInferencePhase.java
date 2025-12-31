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

import com.google.common.base.CaseFormat;
import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import cool.klass.model.converter.compiler.state.property.AntlrReferenceProperty;
import cool.klass.model.meta.grammar.KlassParser;
import cool.klass.model.meta.grammar.KlassParser.ClassifierModifierContext;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

// TODO: Only put audit properties onto version types
public class AuditAssociationInferencePhase extends AbstractCompilerPhase {

	public AuditAssociationInferencePhase(@Nonnull CompilerState compilerState) {
		super(compilerState);
	}

	@Nonnull
	@Override
	public String getName() {
		return "Audit association";
	}

	@Override
	public void enterClassifierModifier(@Nonnull ClassifierModifierContext ctx) {
		super.enterClassifierModifier(ctx);

		String modifierText = ctx.getText();
		if ("audited".equals(modifierText)) {
			this.addAuditProperties();
		}
	}

	private boolean hasAuditReferenceProperty(Predicate<AntlrReferenceProperty> predicate) {
		return this.compilerState.getCompilerWalk()
			.getKlass()
			.getAllProperties()
			.selectInstancesOf(AntlrReferenceProperty.class)
			.asLazy()
			.anySatisfy(predicate);
	}

	private boolean hasAuditDataTypeProperty(Predicate<AntlrDataTypeProperty> predicate) {
		return this.compilerState.getCompilerWalk().getKlass().getAllDataTypeProperties().count(predicate) == 1;
	}

	private void addAuditProperties() {
		Optional<AntlrClass> maybeUserClass = this.compilerState.getDomainModel().getUserClass();
		if (maybeUserClass.isEmpty()) {
			return;
		}
		AntlrClass userClass = maybeUserClass.get();
		int userIdProperties = userClass.getAllDataTypeProperties().count(AntlrDataTypeProperty::isUserId);
		if (userIdProperties != 1) {
			return;
		}

		boolean needsCreatedBy =
			!this.hasAuditReferenceProperty(AntlrReferenceProperty::isCreatedBy)
			&& this.hasAuditDataTypeProperty(AntlrDataTypeProperty::isCreatedBy);
		boolean needsLastUpdatedBy =
			!this.hasAuditReferenceProperty(AntlrReferenceProperty::isLastUpdatedBy)
			&& this.hasAuditDataTypeProperty(AntlrDataTypeProperty::isLastUpdatedBy);

		if (!needsCreatedBy && !needsLastUpdatedBy) {
			return;
		}

		StringBuilder stringBuilder = new StringBuilder();
		AntlrClass klass = this.compilerState.getCompilerWalk().getKlass();
		stringBuilder.append("package ").append(klass.getPackageName()).append("\n");

		if (needsCreatedBy) {
			String sourceCode = this.getSourceCode(userClass, "createdBy", AntlrDataTypeProperty::isCreatedBy, true);
			stringBuilder.append(sourceCode);
		}
		if (needsLastUpdatedBy) {
			String sourceCode = this.getSourceCode(
				userClass,
				"lastUpdatedBy",
				AntlrDataTypeProperty::isLastUpdatedBy,
				false
			);
			stringBuilder.append(sourceCode);
		}

		this.runCompilerMacro(stringBuilder.toString());
	}

	private void runCompilerMacro(String sourceCode) {
		AntlrModifier classifierModifierState = this.compilerState.getCompilerWalk().getClassifierModifier();

		ImmutableList<ParseTreeListener> compilerPhases = Lists.immutable.with(
			new CompilationUnitPhase(this.compilerState),
			new TopLevelElementsPhase(this.compilerState),
			new AssociationPhase(this.compilerState)
		);

		this.compilerState.runRootCompilerMacro(
			classifierModifierState,
			this,
			sourceCode,
			KlassParser::compilationUnit,
			compilerPhases
		);
	}

	@Nonnull
	private String getSourceCode(
		AntlrClass userClass,
		String modifier,
		Predicate<AntlrDataTypeProperty> predicate,
		boolean isFinal
	) {
		String suffix = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, modifier);

		AntlrClass klass = this.compilerState.getCompilerWalk().getKlass();
		String className = klass.getName();
		String associationEndName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, className);

		String userIdPropertyName = userClass
			.getAllDataTypeProperties()
			.detect(AntlrDataTypeProperty::isUserId)
			.getName();

		String finalModifier = isFinal ? " final" : "";

		AntlrDataTypeProperty<?> auditProperty = klass.getAllDataTypeProperties().detect(predicate);

		// language=Klass
		return (
			"\n"
			+ "association "
			+ className
			+ "Has"
			+ suffix
			+ "\n"
			+ "{\n"
			+ "    "
			+ associationEndName
			+ suffix
			+ ": "
			+ className
			+ "[0..*];\n"
			+ "    "
			+ modifier
			+ ": "
			+ userClass.getName()
			+ "[1..1] "
			+ modifier
			+ finalModifier
			+ ";\n"
			+ "\n"
			+ "    relationship this."
			+ auditProperty.getName()
			+ " == "
			+ userClass.getName()
			+ "."
			+ userIdPropertyName
			+ "\n"
			+ "}\n"
		);
	}
}
