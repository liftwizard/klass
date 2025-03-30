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

package cool.klass.model.converter.compiler.phase;

import javax.annotation.Nonnull;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.AntlrAssociation;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEnd;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.meta.grammar.KlassParser;
import cool.klass.model.meta.grammar.KlassParser.AssociationBodyContext;
import cool.klass.model.meta.grammar.KlassParser.RelationshipContext;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.eclipse.collections.api.list.ImmutableList;

public class RelationshipInferencePhase extends AbstractCompilerPhase {

    private static final Converter<String, String> LOWER_CAMEL_TO_UPPER_CAMEL = CaseFormat.LOWER_CAMEL.converterTo(
        CaseFormat.UPPER_CAMEL
    );

    private static final Converter<String, String> UPPER_TO_LOWER_CAMEL = CaseFormat.UPPER_CAMEL.converterTo(
        CaseFormat.LOWER_CAMEL
    );

    public RelationshipInferencePhase(@Nonnull CompilerState compilerState) {
        super(compilerState);
    }

    @Nonnull
    @Override
    public String getName() {
        return "Association relationship";
    }

    @Override
    public void exitAssociationBody(AssociationBodyContext ctx) {
        this.runCompilerMacro(ctx);
        super.exitAssociationBody(ctx);
    }

    private void runCompilerMacro(AssociationBodyContext inPlaceContext) {
        RelationshipContext relationship = inPlaceContext.relationship();
        if (relationship != null) {
            return;
        }

        AntlrAssociation association = this.compilerState.getCompilerWalk().getAssociation();
        if (association.isManyToMany()) {
            return;
        }

        AntlrAssociationEnd sourceEnd = association.getSourceEnd();
        AntlrAssociationEnd targetEnd = association.getTargetEnd();

        // [_..*] --> [_..1];
        if (targetEnd.isToOne() && sourceEnd.isToMany()) {
            this.handleSourceAssociationEnd(inPlaceContext, sourceEnd);
        }
        // [_..1] owned --> [_..*];
        else if (sourceEnd.isToOne() && targetEnd.isToMany()) {
            this.handleTargetAssociationEnd(inPlaceContext, targetEnd);
        }
        // [0..1] --> [1..1]
        else if (sourceEnd.isToOneOptional() && targetEnd.isToOneRequired()) {
            this.handleSourceAssociationEnd(inPlaceContext, sourceEnd);
        }
        // [1..1] --> [0..1]
        else if (targetEnd.isToOneOptional() && sourceEnd.isToOneRequired()) {
            this.handleTargetAssociationEnd(inPlaceContext, targetEnd);
        } else {
            throw new IllegalStateException("Unhandled association end combination: " + association);
        }
    }

    private void handleSourceAssociationEnd(AssociationBodyContext inPlaceContext, AntlrAssociationEnd associationEnd) {
        AntlrClass oppositeType = associationEnd.getOpposite().getType();

        ImmutableList<AntlrDataTypeProperty<?>> allKeyProperties = oppositeType.getAllKeyProperties();
        if (allKeyProperties.isEmpty()) {
            return;
        }

        String sourceCodeText = allKeyProperties
            .collect(
                each ->
                    "this.%s%s == %s.%s".formatted(
                            UPPER_TO_LOWER_CAMEL.convert(oppositeType.getName()),
                            LOWER_CAMEL_TO_UPPER_CAMEL.convert(each.getName()),
                            oppositeType.getName(),
                            each.getName()
                        )
            )
            .makeString("relationship ", " && ", "");

        this.runInPlaceCompilerMacro(inPlaceContext, sourceCodeText);
    }

    private void handleTargetAssociationEnd(AssociationBodyContext inPlaceContext, AntlrAssociationEnd associationEnd) {
        AntlrClass oppositeType = associationEnd.getOpposite().getType();

        ImmutableList<AntlrDataTypeProperty<?>> allKeyProperties = oppositeType.getAllKeyProperties();
        if (allKeyProperties.isEmpty()) {
            return;
        }

        String sourceCodeText = allKeyProperties
            .collect(
                each ->
                    "this.%s == %s.%s%s".formatted(
                            each.getName(),
                            associationEnd.getType().getName(),
                            UPPER_TO_LOWER_CAMEL.convert(oppositeType.getName()),
                            LOWER_CAMEL_TO_UPPER_CAMEL.convert(each.getName())
                        )
            )
            .makeString("relationship ", " && ", "");

        this.runInPlaceCompilerMacro(inPlaceContext, sourceCodeText);
    }

    private void runInPlaceCompilerMacro(AssociationBodyContext inPlaceContext, @Nonnull String sourceCodeText) {
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
