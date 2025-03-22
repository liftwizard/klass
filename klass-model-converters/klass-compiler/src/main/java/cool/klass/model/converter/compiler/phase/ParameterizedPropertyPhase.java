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

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.CompilerWalkState;
import cool.klass.model.converter.compiler.phase.criteria.CriteriaVisitor;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrEnumeration;
import cool.klass.model.converter.compiler.state.AntlrPrimitiveType;
import cool.klass.model.converter.compiler.state.AntlrType;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.criteria.AntlrCriteria;
import cool.klass.model.converter.compiler.state.parameter.AntlrParameter;
import cool.klass.model.converter.compiler.state.parameter.AntlrParameterOwner;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import cool.klass.model.converter.compiler.state.property.AntlrParameterizedProperty;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.grammar.KlassParser.CriteriaExpressionContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationParameterDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterDeclarationListContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterModifierContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterizedPropertyContext;
import cool.klass.model.meta.grammar.KlassParser.PrimitiveParameterDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.RelationshipContext;
import cool.klass.model.meta.grammar.KlassVisitor;
import org.antlr.v4.runtime.ParserRuleContext;

public class ParameterizedPropertyPhase
        extends ReferencePropertyPhase
{
    @Nullable
    private AntlrParameterizedProperty parameterizedProperty;
    @Nullable
    private AntlrParameter             parameter;

    // TODO: Make better use of these Owner interfaces in shared compiler phases
    @Nullable
    private IAntlrElement       criteriaOwner;
    @Nullable
    private AntlrParameterOwner parameterOwner;

    public ParameterizedPropertyPhase(@Nonnull CompilerState compilerState)
    {
        super(compilerState);
    }

    @Override
    public void enterParameterizedProperty(@Nonnull ParameterizedPropertyContext ctx)
    {
        super.enterParameterizedProperty(ctx);

        if (this.parameterizedProperty != null)
        {
            throw new IllegalStateException();
        }
        if (this.parameterOwner != null)
        {
            throw new IllegalStateException();
        }
        if (this.criteriaOwner != null)
        {
            throw new IllegalStateException();
        }
        if (this.classReferenceOwner != null)
        {
            throw new IllegalStateException();
        }
        if (this.multiplicityOwner != null)
        {
            throw new IllegalStateException();
        }

        // TODO: Parameterized Property modifiers

        CompilerWalkState compilerWalk  = this.compilerState.getCompilerWalk();
        AntlrClass        thisReference = (AntlrClass) compilerWalk.getThisReference();

        this.parameterizedProperty = new AntlrParameterizedProperty(
                ctx,
                Optional.of(compilerWalk.getCurrentCompilationUnit()),
                compilerWalk.getNumClassifierMembers(),
                ctx.identifier(),
                thisReference);

        thisReference.enterParameterizedProperty(this.parameterizedProperty);

        this.parameterOwner      = this.parameterizedProperty;
        this.criteriaOwner       = this.parameterizedProperty;
        this.classReferenceOwner = this.parameterizedProperty;
        this.multiplicityOwner   = this.parameterizedProperty;

        this.handleClassReference(ctx.classReference());
        this.handleMultiplicity(ctx.multiplicity());

        KlassVisitor<AntlrCriteria> visitor = new CriteriaVisitor(
                this.compilerState,
                this.parameterizedProperty);

        CriteriaExpressionContext criteriaExpressionContext = ctx.criteriaExpression();
        AntlrCriteria             criteria                  = visitor.visit(criteriaExpressionContext);
        this.parameterizedProperty.setCriteria(criteria);
    }

    @Override
    public void exitParameterizedProperty(@Nonnull ParameterizedPropertyContext ctx)
    {
        this.parameterizedProperty = null;
        this.parameterOwner        = null;
        this.criteriaOwner         = null;
        this.classReferenceOwner   = null;
        this.multiplicityOwner     = null;

        super.exitParameterizedProperty(ctx);
    }

    // TODO: Should probably be moved to its own phase for consistency
    @Override
    public void enterRelationship(@Nonnull RelationshipContext ctx)
    {
        super.enterRelationship(ctx);

        if (this.parameterizedProperty == null)
        {
            return;
        }

        KlassVisitor<AntlrCriteria> visitor = new CriteriaVisitor(
                this.compilerState,
                this.criteriaOwner);
        AntlrCriteria criteria = visitor.visit(ctx.criteriaExpression());
        this.parameterizedProperty.setCriteria(criteria);
    }

    @Override
    public void enterPrimitiveParameterDeclaration(@Nonnull PrimitiveParameterDeclarationContext ctx)
    {
        super.enterPrimitiveParameterDeclaration(ctx);

        if (this.parameterizedProperty == null)
        {
            return;
        }

        String             primitiveTypeName  = ctx.primitiveType().getText();
        PrimitiveType      primitiveType      = PrimitiveType.byPrettyName(primitiveTypeName);
        AntlrPrimitiveType primitiveTypeState = AntlrPrimitiveType.valueOf(primitiveType);

        this.enterParameterDeclaration(ctx, primitiveTypeState, ctx.identifier());
    }

    @Override
    public void exitPrimitiveParameterDeclaration(@Nonnull PrimitiveParameterDeclarationContext ctx)
    {
        this.parameter         = null;
        this.multiplicityOwner = null;

        super.exitPrimitiveParameterDeclaration(ctx);
    }

    @Override
    public void enterEnumerationParameterDeclaration(@Nonnull EnumerationParameterDeclarationContext ctx)
    {
        super.enterEnumerationParameterDeclaration(ctx);

        if (this.parameterizedProperty == null)
        {
            return;
        }

        String           enumerationName = ctx.enumerationReference().getText();
        AntlrEnumeration enumeration     = this.compilerState.getDomainModel().getEnumerationByName(enumerationName);

        this.enterParameterDeclaration(ctx, enumeration, ctx.identifier());
    }

    @Override
    public void exitEnumerationParameterDeclaration(@Nonnull EnumerationParameterDeclarationContext ctx)
    {
        this.parameter         = null;
        this.multiplicityOwner = null;

        super.exitEnumerationParameterDeclaration(ctx);
    }

    @Override
    public void enterParameterDeclarationList(@Nonnull ParameterDeclarationListContext ctx)
    {
        super.enterParameterDeclarationList(ctx);
        throw new UnsupportedOperationException(this.getClass().getSimpleName()
                + ".enterParameterDeclarationList() not implemented yet");
    }

    @Override
    public void exitParameterDeclarationList(@Nonnull ParameterDeclarationListContext ctx)
    {
        super.exitParameterDeclarationList(ctx);
        throw new UnsupportedOperationException(this.getClass().getSimpleName()
                + ".exitParameterDeclarationList() not implemented yet");
    }

    @Override
    public void enterParameterModifier(@Nonnull ParameterModifierContext ctx)
    {
        super.enterParameterModifier(ctx);
        if (this.parameterizedProperty == null && this.parameter == null)
        {
            return;
        }

        int ordinal = this.parameter.getNumModifiers();
        AntlrModifier modifier = new AntlrModifier(
                ctx,
                Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
                ordinal,
                this.parameter);
        this.parameter.enterModifier(modifier);
    }

    private void enterParameterDeclaration(
            @Nonnull ParserRuleContext ctx,
            @Nonnull AntlrType typeState,
            @Nonnull IdentifierContext identifierContext)
    {
        if (this.parameter != null)
        {
            throw new IllegalStateException();
        }

        this.parameter         = new AntlrParameter(
                ctx,
                Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
                this.parameterOwner.getNumParameters() + 1,
                identifierContext,
                typeState,
                (IAntlrElement) this.parameterOwner);
        this.multiplicityOwner = this.parameter;
        this.parameterOwner.enterParameterDeclaration(this.parameter);
    }
}
