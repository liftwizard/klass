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

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import cool.klass.model.converter.compiler.state.property.validation.AbstractAntlrPropertyValidation;
import cool.klass.model.meta.grammar.KlassParser;
import cool.klass.model.meta.grammar.KlassParser.ClassBodyContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceBodyContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

// TODO: Only put audit properties onto version types
public class AuditPropertyInferencePhase extends AbstractCompilerPhase {

    public AuditPropertyInferencePhase(@Nonnull CompilerState compilerState) {
        super(compilerState);
    }

    @Nonnull
    @Override
    public String getName() {
        return "Audit modifier";
    }

    @Override
    public void exitInterfaceBody(InterfaceBodyContext ctx) {
        this.runCompilerMacro(ctx);
        super.exitInterfaceBody(ctx);
    }

    @Override
    public void exitClassBody(ClassBodyContext ctx) {
        this.runCompilerMacro(ctx);
        super.exitClassBody(ctx);
    }

    private void runCompilerMacro(ParserRuleContext inPlaceContext) {
        AntlrClassifier classifier = this.compilerState.getCompilerWalk().getClassifier();
        MutableList<AntlrModifier> declaredModifiers = classifier.getDeclaredModifiers();
        ImmutableList<AntlrDataTypeProperty<?>> allDataTypeProperties = classifier.getAllDataTypeProperties();

        MutableList<AntlrModifier> auditedModifiers = declaredModifiers.select(modifier -> modifier.is("audited"));
        if (auditedModifiers.size() != 1) {
            return;
        }
        AntlrModifier auditedModifier = auditedModifiers.getOnly();

        Optional<AntlrClass> maybeUserClass = this.compilerState.getDomainModel().getUserClass();
        if (maybeUserClass.isEmpty()) {
            return;
        }
        AntlrClass userClass = maybeUserClass.get();
        ImmutableList<AntlrDataTypeProperty<?>> userIdProperties = userClass
            .getAllDataTypeProperties()
            .select(AntlrDataTypeProperty::isUserId);
        if (userIdProperties.size() != 1) {
            return;
        }
        AntlrDataTypeProperty<?> userIdProperty = userIdProperties.getOnly();
        ListIterable<AbstractAntlrPropertyValidation> validations = userIdProperty.getValidations();

        String validationSourceCode = validations.isEmpty() ? "" : validations.makeString(" ", " ", "");

        StringBuilder sourceCodeText = new StringBuilder();
        if (allDataTypeProperties.noneSatisfy(AntlrDataTypeProperty::isCreatedBy)) {
            sourceCodeText
                .append("    createdById    : String createdBy private userId final")
                .append(validationSourceCode)
                .append(";\n");
        }
        if (allDataTypeProperties.noneSatisfy(AntlrDataTypeProperty::isCreatedOn)) {
            sourceCodeText.append("    createdOn      : Instant createdOn final;\n");
        }
        if (allDataTypeProperties.noneSatisfy(AntlrDataTypeProperty::isLastUpdatedBy)) {
            sourceCodeText
                .append("    lastUpdatedById: String lastUpdatedBy private userId")
                .append(validationSourceCode)
                .append(";\n");
        }

        /*
        if (!this.hasAuditReferenceProperty(AntlrReferenceProperty::isCreatedBy))
        {
            String createdBySourceCodeText = ""
                    + "    createdBy(): " + userClass.getName() + "[1..1] createdBy\n"
                    + "    {\n"
                    + "        this.createdById == " + userClass.getName() + "." + userIdProperty.getName() + "\n"
                    + "    }\n";

            this.runCompilerMacro(createdBySourceCodeText);
        }

        if (!this.hasAuditReferenceProperty(AntlrReferenceProperty::isLastUpdatedBy))
        {
            String lastUpdatedBySourceCodeText = ""
                    + "    lastUpdatedBy(): " + userClass.getName() + "[1..1] lastUpdatedBy\n"
                    + "    {\n"
                    + "        this.lastUpdatedById == " + userClass.getName() + "." + userIdProperty.getName() + "\n"
                    + "    }\n";

            this.runCompilerMacro(lastUpdatedBySourceCodeText);
        }
        */

        this.runCompilerMacro(inPlaceContext, sourceCodeText.toString(), auditedModifier);
    }

    private void runCompilerMacro(
        ParserRuleContext inPlaceContext,
        @Nonnull String sourceCodeText,
        AntlrModifier macroElement
    ) {
        if (sourceCodeText.isEmpty()) {
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
