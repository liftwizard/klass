/*
 * Copyright 2024 Craig Motlin
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

package cool.klass.model.converter.compiler.state.property;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.AntlrPrimitiveType;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.property.ModifierImpl.ModifierBuilder;
import cool.klass.model.meta.domain.property.PrimitivePropertyImpl.PrimitivePropertyBuilder;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.PrimitivePropertyContext;
import cool.klass.model.meta.grammar.KlassParser.PrimitiveTypeContext;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;

public class AntlrPrimitiveProperty extends AntlrDataTypeProperty<PrimitiveType> {

    public static final AntlrPrimitiveProperty AMBIGUOUS = new AntlrPrimitiveProperty(
        new PrimitivePropertyContext(AMBIGUOUS_PARENT, -1),
        Optional.empty(),
        -1,
        AMBIGUOUS_IDENTIFIER_CONTEXT,
        AntlrClassifier.AMBIGUOUS,
        false,
        AntlrPrimitiveType.AMBIGUOUS
    );

    @Nonnull
    private final AntlrPrimitiveType antlrPrimitiveType;

    private PrimitivePropertyBuilder elementBuilder;

    public AntlrPrimitiveProperty(
        @Nonnull PrimitivePropertyContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        int ordinal,
        @Nonnull IdentifierContext nameContext,
        @Nonnull AntlrClassifier owningClassifier,
        boolean isOptional,
        @Nonnull AntlrPrimitiveType primitiveType
    ) {
        super(elementContext, compilationUnit, ordinal, nameContext, owningClassifier, isOptional);
        this.antlrPrimitiveType = Objects.requireNonNull(primitiveType);
    }

    @Nonnull
    @Override
    public AntlrPrimitiveType getType() {
        return this.antlrPrimitiveType;
    }

    @Override
    protected PrimitiveTypeContext getTypeParserRuleContext() {
        return this.getElementContext().primitiveType();
    }

    @Nonnull
    @Override
    public PrimitivePropertyBuilder build() {
        if (this.elementBuilder != null) {
            throw new IllegalStateException();
        }

        this.elementBuilder = new PrimitivePropertyBuilder(
            (PrimitivePropertyContext) this.elementContext,
            this.getMacroElementBuilder(),
            this.getSourceCodeBuilder(),
            this.ordinal,
            this.getNameContext(),
            this.antlrPrimitiveType.getPrimitiveType(),
            this.owningClassifier.getElementBuilder(),
            this.isOptional
        );

        ImmutableList<ModifierBuilder> modifiers = this.getModifiers().collect(AntlrModifier::build).toImmutable();
        this.elementBuilder.setModifierBuilders(modifiers);

        this.buildValidations();

        return this.elementBuilder;
    }

    @Nonnull
    @Override
    public PrimitivePropertyContext getElementContext() {
        return (PrimitivePropertyContext) super.getElementContext();
    }

    @Nonnull
    @Override
    public PrimitivePropertyBuilder getElementBuilder() {
        return Objects.requireNonNull(this.elementBuilder);
    }

    @Override
    public String getTypeName() {
        return this.getElementContext().primitiveType().getText();
    }

    // <editor-fold desc="Report Compiler Errors">
    @Override
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        super.reportErrors(compilerAnnotationHolder);

        this.reportInvalidTemporalMultiplicity(compilerAnnotationHolder);
        this.reportInvalidTemporalVisibility(compilerAnnotationHolder);
        this.reportInvalidStringValidations(compilerAnnotationHolder);
        this.reportInvalidNumericValidations(compilerAnnotationHolder);
    }

    @Override
    protected void reportInvalidIdProperties(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        PrimitiveType primitiveType = this.antlrPrimitiveType.getPrimitiveType();
        if (primitiveType.isId()) {
            this.reportNonKeyIdProperty(compilerAnnotationHolder);
            this.reportOverriddenIdProperty(compilerAnnotationHolder);
        } else {
            this.reportInvalidTypeIdProperty(compilerAnnotationHolder);
        }
    }

    private void reportNonKeyIdProperty(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (this.isKey()) {
            return;
        }

        ListIterable<AntlrModifier> idModifiers = this.getModifiersByName("id");
        for (AntlrModifier idModifier : idModifiers) {
            String message = "Properties with the 'id' modifier must also have the 'key' modifier.";
            compilerAnnotationHolder.add("ERR_NKY_IDP", message, idModifier);
        }
    }

    private void reportInvalidTypeIdProperty(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        PrimitiveType primitiveType = this.antlrPrimitiveType.getPrimitiveType();

        ListIterable<AntlrModifier> idModifiers = this.getModifiersByName("id");
        for (AntlrModifier idModifier : idModifiers) {
            String message = String.format(
                "Primitive properties with type %s may not be auto-generated ids. Only types %s may be id properties.",
                primitiveType.getPrettyName(),
                PrimitiveType.ID_PRIMITIVE_TYPES
            );
            compilerAnnotationHolder.add("ERR_PRP_IDP", message, idModifier);
        }
    }

    private void reportOverriddenIdProperty(CompilerAnnotationHolder compilerAnnotationHolder) {
        if (!(this.owningClassifier instanceof AntlrClass owningClass)) {
            return;
        }

        if (owningClass.getSuperClass().isEmpty()) {
            return;
        }

        AntlrClass superClass = owningClass.getSuperClass().get();
        AntlrDataTypeProperty<?> overriddenProperty = superClass.getDataTypePropertyByName(this.getName());
        if (overriddenProperty == AntlrEnumerationProperty.NOT_FOUND) {
            return;
        }

        if (!(overriddenProperty.getOwningClassifier() instanceof AntlrClass overriddenPropertyOwningClass)) {
            return;
        }

        String message =
            "'id' properties may not be overridden. The property '%s' in class '%s' overrides the 'id' property in class '%s'.".formatted(
                    this.getName(),
                    owningClass.getName(),
                    overriddenPropertyOwningClass.getName()
                );
        ListIterable<AntlrModifier> idModifiers = this.getModifiersByName("id");
        if (idModifiers.notEmpty()) {
            for (AntlrModifier idModifier : idModifiers) {
                compilerAnnotationHolder.add("ERR_OVR_IDP", message, idModifier);
            }
        } else {
            if (overriddenProperty.isId()) {
                compilerAnnotationHolder.add("ERR_OVR_IDP", message, this);
            }
        }
    }

    private void reportInvalidTemporalMultiplicity(CompilerAnnotationHolder compilerAnnotationHolder) {
        PrimitiveType primitiveType = this.antlrPrimitiveType.getPrimitiveType();
        if (!primitiveType.isTemporal()) {
            return;
        }

        if (this.isOptional) {
            return;
        }

        String message = String.format(
            "Primitive properties with type %s may not be required.",
            primitiveType.getPrettyName()
        );
        compilerAnnotationHolder.add("ERR_REQ_TMP", message, this, this.getTypeParserRuleContext());
    }

    private void reportInvalidTemporalVisibility(CompilerAnnotationHolder compilerAnnotationHolder) {
        PrimitiveType primitiveType = this.antlrPrimitiveType.getPrimitiveType();
        if (primitiveType != PrimitiveType.TEMPORAL_RANGE) {
            return;
        }

        if (this.isPrivate()) {
            return;
        }

        String message = String.format(
            "Primitive properties with type %s must be private.",
            primitiveType.getPrettyName()
        );
        compilerAnnotationHolder.add("ERR_REQ_PRV", message, this, this.getTypeParserRuleContext());
    }

    private void reportInvalidStringValidations(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        PrimitiveType primitiveType = this.antlrPrimitiveType.getPrimitiveType();
        if (primitiveType == PrimitiveType.STRING) {
            return;
        }

        this.minLengthValidations.each(each -> each.reportInvalidType(compilerAnnotationHolder, primitiveType));
        this.maxLengthValidations.each(each -> each.reportInvalidType(compilerAnnotationHolder, primitiveType));
    }

    private void reportInvalidNumericValidations(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        PrimitiveType primitiveType = this.antlrPrimitiveType.getPrimitiveType();
        if (primitiveType.isNumeric()) {
            return;
        }

        this.minValidations.each(each -> each.reportInvalidType(compilerAnnotationHolder, primitiveType));
        this.maxValidations.each(each -> each.reportInvalidType(compilerAnnotationHolder, primitiveType));
    }
    // </editor-fold>
}
