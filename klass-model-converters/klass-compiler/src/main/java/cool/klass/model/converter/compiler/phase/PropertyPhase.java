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

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.CompilerWalkState;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.AntlrClassifierReference;
import cool.klass.model.converter.compiler.state.AntlrClassifierReferenceOwner;
import cool.klass.model.converter.compiler.state.AntlrDomainModel;
import cool.klass.model.converter.compiler.state.AntlrEnumeration;
import cool.klass.model.converter.compiler.state.AntlrMultiplicity;
import cool.klass.model.converter.compiler.state.AntlrMultiplicityOwner;
import cool.klass.model.converter.compiler.state.AntlrPrimitiveType;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEndSignature;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrEnumerationProperty;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import cool.klass.model.converter.compiler.state.property.AntlrPrimitiveProperty;
import cool.klass.model.converter.compiler.state.property.validation.AntlrMaxLengthPropertyValidation;
import cool.klass.model.converter.compiler.state.property.validation.AntlrMaxPropertyValidation;
import cool.klass.model.converter.compiler.state.property.validation.AntlrMinLengthPropertyValidation;
import cool.klass.model.converter.compiler.state.property.validation.AntlrMinPropertyValidation;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndModifierContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndSignatureContext;
import cool.klass.model.meta.grammar.KlassParser.ClassifierReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.DataTypePropertyModifierContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationPropertyContext;
import cool.klass.model.meta.grammar.KlassParser.IntegerLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.MaxLengthValidationContext;
import cool.klass.model.meta.grammar.KlassParser.MaxValidationContext;
import cool.klass.model.meta.grammar.KlassParser.MinLengthValidationContext;
import cool.klass.model.meta.grammar.KlassParser.MinValidationContext;
import cool.klass.model.meta.grammar.KlassParser.MultiplicityContext;
import cool.klass.model.meta.grammar.KlassParser.PrimitivePropertyContext;

public class PropertyPhase extends AbstractCompilerPhase {

	@Nullable
	private AntlrDataTypeProperty<?> dataTypeProperty;

	@Nullable
	private AntlrAssociationEndSignature associationEndSignature;

	@Nullable
	private AntlrClassifierReferenceOwner classifierReferenceOwner;

	@Nullable
	private AntlrMultiplicityOwner multiplicityOwner;

	public PropertyPhase(@Nonnull CompilerState compilerState) {
		super(compilerState);
	}

	@Override
	public void enterAssociationEndSignature(@Nonnull AssociationEndSignatureContext ctx) {
		super.enterAssociationEndSignature(ctx);

		if (this.associationEndSignature != null) {
			throw new IllegalStateException();
		}
		if (this.classifierReferenceOwner != null) {
			throw new IllegalStateException();
		}
		if (this.multiplicityOwner != null) {
			throw new IllegalStateException();
		}

		CompilerWalkState compilerWalk = this.compilerState.getCompilerWalk();
		this.associationEndSignature = new AntlrAssociationEndSignature(
			ctx,
			Optional.of(compilerWalk.getCurrentCompilationUnit()),
			compilerWalk.getNumClassifierMembers(),
			ctx.identifier(),
			this.getClassifier()
		);
		this.classifierReferenceOwner = this.associationEndSignature;
		this.multiplicityOwner = this.associationEndSignature;
		this.getClassifier().enterAssociationEndSignature(this.associationEndSignature);
	}

	@Override
	public void exitAssociationEndSignature(@Nonnull AssociationEndSignatureContext ctx) {
		Objects.requireNonNull(this.associationEndSignature);
		this.associationEndSignature = null;
		this.classifierReferenceOwner = null;
		this.multiplicityOwner = null;
		super.exitAssociationEndSignature(ctx);
	}

	@Override
	public void enterPrimitiveProperty(@Nonnull PrimitivePropertyContext ctx) {
		super.enterPrimitiveProperty(ctx);

		boolean isOptional = ctx.optionalMarker() != null;
		String primitiveTypeName = ctx.primitiveType().getText();
		PrimitiveType primitiveType = PrimitiveType.byPrettyName(primitiveTypeName);
		AntlrPrimitiveType primitiveTypeState = AntlrPrimitiveType.valueOf(primitiveType);

		if (this.dataTypeProperty != null) {
			throw new IllegalStateException();
		}

		CompilerWalkState compilerWalk = this.compilerState.getCompilerWalk();
		this.dataTypeProperty = new AntlrPrimitiveProperty(
			ctx,
			Optional.of(compilerWalk.getCurrentCompilationUnit()),
			compilerWalk.getNumClassifierMembers(),
			ctx.identifier(),
			this.getClassifier(),
			isOptional,
			primitiveTypeState
		);

		this.getClassifier().enterDataTypeProperty(this.dataTypeProperty);
	}

	@Override
	public void exitPrimitiveProperty(@Nonnull PrimitivePropertyContext ctx) {
		Objects.requireNonNull(this.dataTypeProperty);
		this.dataTypeProperty = null;
		super.exitPrimitiveProperty(ctx);
	}

	@Override
	public void enterEnumerationProperty(@Nonnull EnumerationPropertyContext ctx) {
		super.enterEnumerationProperty(ctx);

		boolean isOptional = ctx.optionalMarker() != null;
		AntlrDomainModel domainModel = this.compilerState.getDomainModel();
		String enumerationName = ctx.enumerationReference().getText();
		AntlrEnumeration enumeration = domainModel.getEnumerationByName(enumerationName);

		if (this.dataTypeProperty != null) {
			throw new IllegalStateException();
		}
		CompilerWalkState compilerWalk = this.compilerState.getCompilerWalk();
		this.dataTypeProperty = new AntlrEnumerationProperty(
			ctx,
			Optional.of(compilerWalk.getCurrentCompilationUnit()),
			compilerWalk.getNumClassifierMembers(),
			ctx.identifier(),
			this.getClassifier(),
			isOptional,
			enumeration
		);

		this.getClassifier().enterDataTypeProperty(this.dataTypeProperty);
	}

	@Override
	public void exitEnumerationProperty(@Nonnull EnumerationPropertyContext ctx) {
		Objects.requireNonNull(this.dataTypeProperty);
		this.dataTypeProperty = null;

		super.exitEnumerationProperty(ctx);
	}

	@Override
	public void enterMinLengthValidation(@Nonnull MinLengthValidationContext ctx) {
		super.enterMinLengthValidation(ctx);

		IntegerLiteralContext integerLiteralContext = ctx.integerValidationParameter().integerLiteral();
		int length = this.getIntegerFromLiteral(integerLiteralContext);
		AntlrMinLengthPropertyValidation minLengthValidation = new AntlrMinLengthPropertyValidation(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			this.dataTypeProperty,
			length
		);
		this.dataTypeProperty.addMinLengthValidation(minLengthValidation);
	}

	@Override
	public void enterMaxLengthValidation(@Nonnull MaxLengthValidationContext ctx) {
		super.enterMaxLengthValidation(ctx);

		IntegerLiteralContext integerLiteralContext = ctx.integerValidationParameter().integerLiteral();
		int length = this.getIntegerFromLiteral(integerLiteralContext);
		AntlrMaxLengthPropertyValidation maxLengthValidation = new AntlrMaxLengthPropertyValidation(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			this.dataTypeProperty,
			length
		);
		this.dataTypeProperty.addMaxLengthValidation(maxLengthValidation);
	}

	@Override
	public void enterMinValidation(@Nonnull MinValidationContext ctx) {
		super.enterMinValidation(ctx);

		IntegerLiteralContext integerLiteralContext = ctx.integerValidationParameter().integerLiteral();
		int minimum = this.getIntegerFromLiteral(integerLiteralContext);
		AntlrMinPropertyValidation minValidation = new AntlrMinPropertyValidation(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			this.dataTypeProperty,
			minimum
		);
		this.dataTypeProperty.addMinValidation(minValidation);
	}

	@Override
	public void enterMaxValidation(@Nonnull MaxValidationContext ctx) {
		super.enterMaxValidation(ctx);

		IntegerLiteralContext integerLiteralContext = ctx.integerValidationParameter().integerLiteral();
		int maximum = this.getIntegerFromLiteral(integerLiteralContext);
		AntlrMaxPropertyValidation maxValidation = new AntlrMaxPropertyValidation(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			this.dataTypeProperty,
			maximum
		);
		this.dataTypeProperty.addMaxValidation(maxValidation);
	}

	@Override
	public void enterMultiplicity(@Nonnull MultiplicityContext ctx) {
		super.enterMultiplicity(ctx);

		if (this.multiplicityOwner == null) {
			return;
		}

		AntlrMultiplicity multiplicity = new AntlrMultiplicity(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			this.multiplicityOwner
		);

		this.associationEndSignature.enterMultiplicity(multiplicity);
	}

	@Override
	public void enterDataTypePropertyModifier(DataTypePropertyModifierContext ctx) {
		super.enterDataTypePropertyModifier(ctx);
		AntlrModifier modifier = new AntlrModifier(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			this.dataTypeProperty.getNumModifiers() + 1,
			this.dataTypeProperty
		);
		this.dataTypeProperty.enterModifier(modifier);
	}

	@Override
	public void enterAssociationEndModifier(@Nonnull AssociationEndModifierContext ctx) {
		super.enterAssociationEndModifier(ctx);

		if (this.associationEndSignature == null) {
			return;
		}

		AntlrModifier antlrAssociationEndModifier = new AntlrModifier(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			this.associationEndSignature.getNumModifiers() + 1,
			this.associationEndSignature
		);
		this.associationEndSignature.enterModifier(antlrAssociationEndModifier);
	}

	@Override
	public void enterClassifierReference(@Nonnull ClassifierReferenceContext ctx) {
		super.enterClassifierReference(ctx);

		if (this.classifierReferenceOwner == null) {
			return;
		}

		String classifierName = ctx.identifier().getText();
		AntlrDomainModel domainModel = this.compilerState.getDomainModel();
		AntlrClassifier classifier = domainModel.getClassifierByName(classifierName);
		AntlrClassifierReference classifierReference = new AntlrClassifierReference(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			this.classifierReferenceOwner,
			classifier
		);

		this.classifierReferenceOwner.enterClassifierReference(classifierReference);
	}

	private int getIntegerFromLiteral(@Nonnull IntegerLiteralContext integerLiteralContext) {
		String integerText = integerLiteralContext.getText();
		String withoutUnderscores = integerText.replaceAll("_", "");
		return Integer.decode(withoutUnderscores);
	}

	@Nullable
	private AntlrClassifier getClassifier() {
		return this.compilerState.getCompilerWalk().getClassifier();
	}
}
