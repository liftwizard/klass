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
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import cool.klass.model.meta.grammar.KlassParser;
import cool.klass.model.meta.grammar.KlassParser.ClassBodyContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceBodyContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;

public class ClassTemporalPropertyInferencePhase extends AbstractCompilerPhase {

    public ClassTemporalPropertyInferencePhase(@Nonnull CompilerState compilerState) {
        super(compilerState);
    }

    @Nonnull
    @Override
    public String getName() {
        return "Temporal modifier";
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

        MutableList<AntlrModifier> validTemporalModifiers = declaredModifiers.select(
            (modifier) -> modifier.is("validTemporal") || modifier.is("bitemporal")
        );
        MutableList<AntlrModifier> systemTemporalModifiers = declaredModifiers.select(
            (modifier) -> modifier.is("systemTemporal") || modifier.is("bitemporal")
        );

        if (validTemporalModifiers.size() == 1) {
            AntlrModifier validTemporalModifier = validTemporalModifiers.getOnly();
            StringBuilder sourceCodeText = new StringBuilder();
            if (allDataTypeProperties.noneSatisfy(AntlrDataTypeProperty::isValidRange)) {
                sourceCodeText.append("    valid    : TemporalRange?   valid private;\n");
            }
            if (allDataTypeProperties.noneSatisfy(AntlrDataTypeProperty::isValidFrom)) {
                sourceCodeText.append("    validFrom: TemporalInstant? valid from;\n");
            }
            if (allDataTypeProperties.noneSatisfy(AntlrDataTypeProperty::isValidTo)) {
                sourceCodeText.append("    validTo  : TemporalInstant? valid to;\n");
            }
            this.runCompilerMacro(inPlaceContext, sourceCodeText.toString(), validTemporalModifier);
        }

        if (systemTemporalModifiers.size() == 1) {
            AntlrModifier systemTemporalModifier = systemTemporalModifiers.getOnly();
            StringBuilder sourceCodeText = new StringBuilder();
            if (allDataTypeProperties.noneSatisfy(AntlrDataTypeProperty::isSystemRange)) {
                sourceCodeText.append("    system    : TemporalRange?   system private;\n");
            }
            if (allDataTypeProperties.noneSatisfy(AntlrDataTypeProperty::isSystemFrom)) {
                sourceCodeText.append("    systemFrom: TemporalInstant? system from;\n");
            }
            if (allDataTypeProperties.noneSatisfy(AntlrDataTypeProperty::isSystemTo)) {
                sourceCodeText.append("    systemTo  : TemporalInstant? system to;\n");
            }
            this.runCompilerMacro(inPlaceContext, sourceCodeText.toString(), systemTemporalModifier);
        }
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
