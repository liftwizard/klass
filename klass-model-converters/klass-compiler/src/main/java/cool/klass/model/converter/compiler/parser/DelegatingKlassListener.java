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

package cool.klass.model.converter.compiler.parser;

import javax.annotation.OverridingMethodsMustInvokeSuper;

import cool.klass.model.meta.grammar.KlassListener;
import cool.klass.model.meta.grammar.KlassParser.AbstractDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ArgumentContext;
import cool.klass.model.meta.grammar.KlassParser.ArgumentListContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationBlockContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationBodyContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndModifierContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndSignatureContext;
import cool.klass.model.meta.grammar.KlassParser.BooleanLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.CharacterLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.ClassBlockContext;
import cool.klass.model.meta.grammar.KlassParser.ClassBodyContext;
import cool.klass.model.meta.grammar.KlassParser.ClassDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ClassHeaderContext;
import cool.klass.model.meta.grammar.KlassParser.ClassMemberContext;
import cool.klass.model.meta.grammar.KlassParser.ClassOrUserContext;
import cool.klass.model.meta.grammar.KlassParser.ClassReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ClassServiceModifierContext;
import cool.klass.model.meta.grammar.KlassParser.ClassifierModifierContext;
import cool.klass.model.meta.grammar.KlassParser.ClassifierReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.CompilationUnitContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaAllContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaEdgePointContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaExpressionAndContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaExpressionGroupContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaExpressionOrContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaNativeContext;
import cool.klass.model.meta.grammar.KlassParser.CriteriaOperatorContext;
import cool.klass.model.meta.grammar.KlassParser.DataTypePropertyContext;
import cool.klass.model.meta.grammar.KlassParser.DataTypePropertyModifierContext;
import cool.klass.model.meta.grammar.KlassParser.DataTypePropertyValidationContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationBlockContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationBodyContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationParameterDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationPrettyNameContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationPropertyContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.EqualityOperatorContext;
import cool.klass.model.meta.grammar.KlassParser.ExpressionMemberReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ExpressionValueContext;
import cool.klass.model.meta.grammar.KlassParser.ExtendsDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.FloatingPointLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.HeaderContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.ImplementsDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.InOperatorContext;
import cool.klass.model.meta.grammar.KlassParser.InequalityOperatorContext;
import cool.klass.model.meta.grammar.KlassParser.IntegerLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.IntegerValidationParameterContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceBlockContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceBodyContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceHeaderContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceMemberContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.InvalidParameterDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.KeywordValidAsIdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.LiteralContext;
import cool.klass.model.meta.grammar.KlassParser.LiteralListContext;
import cool.klass.model.meta.grammar.KlassParser.MaxLengthValidationContext;
import cool.klass.model.meta.grammar.KlassParser.MaxLengthValidationKeywordContext;
import cool.klass.model.meta.grammar.KlassParser.MaxValidationContext;
import cool.klass.model.meta.grammar.KlassParser.MaxValidationKeywordContext;
import cool.klass.model.meta.grammar.KlassParser.MemberReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.MinLengthValidationContext;
import cool.klass.model.meta.grammar.KlassParser.MinLengthValidationKeywordContext;
import cool.klass.model.meta.grammar.KlassParser.MinValidationContext;
import cool.klass.model.meta.grammar.KlassParser.MinValidationKeywordContext;
import cool.klass.model.meta.grammar.KlassParser.MultiplicityBodyContext;
import cool.klass.model.meta.grammar.KlassParser.MultiplicityContext;
import cool.klass.model.meta.grammar.KlassParser.NativeLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.NullLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.OperatorContext;
import cool.klass.model.meta.grammar.KlassParser.OptionalMarkerContext;
import cool.klass.model.meta.grammar.KlassParser.OrderByDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.OrderByDirectionContext;
import cool.klass.model.meta.grammar.KlassParser.OrderByMemberReferencePathContext;
import cool.klass.model.meta.grammar.KlassParser.PackageDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.PackageNameContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterDeclarationListContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterModifierContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterizedPropertyContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterizedPropertyModifierContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterizedPropertySignatureContext;
import cool.klass.model.meta.grammar.KlassParser.PrimitiveParameterDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.PrimitivePropertyContext;
import cool.klass.model.meta.grammar.KlassParser.PrimitiveTypeContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionBlockContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionBodyContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionMemberContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionParameterizedPropertyContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionPrimitiveMemberContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionProjectionReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionReferencePropertyContext;
import cool.klass.model.meta.grammar.KlassParser.QueryParameterListContext;
import cool.klass.model.meta.grammar.KlassParser.RelationshipContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceBlockContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceBodyContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceCategoryModifierContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceCriteriaDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceCriteriaKeywordContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceGroupBlockContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceGroupBodyContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceGroupDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceMultiplicityContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceMultiplicityDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceOrderByDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceProjectionDispatchContext;
import cool.klass.model.meta.grammar.KlassParser.StringLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.StringOperatorContext;
import cool.klass.model.meta.grammar.KlassParser.ThisMemberReferencePathContext;
import cool.klass.model.meta.grammar.KlassParser.TopLevelDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.TypeMemberReferencePathContext;
import cool.klass.model.meta.grammar.KlassParser.UrlConstantContext;
import cool.klass.model.meta.grammar.KlassParser.UrlContext;
import cool.klass.model.meta.grammar.KlassParser.UrlDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.UrlParameterDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.UrlParameterDeclarationEOFContext;
import cool.klass.model.meta.grammar.KlassParser.UrlPathSegmentContext;
import cool.klass.model.meta.grammar.KlassParser.VerbContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

// Deliberately not abstract
// Implements every method of KlassListener by delegating to getDelegate()
public class DelegatingKlassListener implements KlassListener {

    protected KlassListener getDelegate() {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".getDelegate() not implemented yet");
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterCompilationUnit(CompilationUnitContext ctx) {
        this.getDelegate().enterCompilationUnit(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitCompilationUnit(CompilationUnitContext ctx) {
        this.getDelegate().exitCompilationUnit(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterUrlParameterDeclarationEOF(UrlParameterDeclarationEOFContext ctx) {
        this.getDelegate().enterUrlParameterDeclarationEOF(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitUrlParameterDeclarationEOF(UrlParameterDeclarationEOFContext ctx) {
        this.getDelegate().exitUrlParameterDeclarationEOF(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterPackageDeclaration(PackageDeclarationContext ctx) {
        this.getDelegate().enterPackageDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitPackageDeclaration(PackageDeclarationContext ctx) {
        this.getDelegate().exitPackageDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterPackageName(PackageNameContext ctx) {
        this.getDelegate().enterPackageName(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitPackageName(PackageNameContext ctx) {
        this.getDelegate().exitPackageName(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterTopLevelDeclaration(TopLevelDeclarationContext ctx) {
        this.getDelegate().enterTopLevelDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitTopLevelDeclaration(TopLevelDeclarationContext ctx) {
        this.getDelegate().exitTopLevelDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterInterfaceDeclaration(InterfaceDeclarationContext ctx) {
        this.getDelegate().enterInterfaceDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitInterfaceDeclaration(InterfaceDeclarationContext ctx) {
        this.getDelegate().exitInterfaceDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterInterfaceHeader(InterfaceHeaderContext ctx) {
        this.getDelegate().enterInterfaceHeader(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitInterfaceHeader(InterfaceHeaderContext ctx) {
        this.getDelegate().exitInterfaceHeader(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterInterfaceBlock(InterfaceBlockContext ctx) {
        this.getDelegate().enterInterfaceBlock(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitInterfaceBlock(InterfaceBlockContext ctx) {
        this.getDelegate().exitInterfaceBlock(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterInterfaceBody(InterfaceBodyContext ctx) {
        this.getDelegate().enterInterfaceBody(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitInterfaceBody(InterfaceBodyContext ctx) {
        this.getDelegate().exitInterfaceBody(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterClassDeclaration(ClassDeclarationContext ctx) {
        this.getDelegate().enterClassDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitClassDeclaration(ClassDeclarationContext ctx) {
        this.getDelegate().exitClassDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterClassHeader(ClassHeaderContext ctx) {
        this.getDelegate().enterClassHeader(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitClassHeader(ClassHeaderContext ctx) {
        this.getDelegate().exitClassHeader(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterClassOrUser(ClassOrUserContext ctx) {
        this.getDelegate().enterClassOrUser(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitClassOrUser(ClassOrUserContext ctx) {
        this.getDelegate().exitClassOrUser(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterClassServiceModifier(ClassServiceModifierContext ctx) {
        this.getDelegate().enterClassServiceModifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitClassServiceModifier(ClassServiceModifierContext ctx) {
        this.getDelegate().exitClassServiceModifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterServiceCategoryModifier(ServiceCategoryModifierContext ctx) {
        this.getDelegate().enterServiceCategoryModifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitServiceCategoryModifier(ServiceCategoryModifierContext ctx) {
        this.getDelegate().exitServiceCategoryModifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterClassBlock(ClassBlockContext ctx) {
        this.getDelegate().enterClassBlock(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitClassBlock(ClassBlockContext ctx) {
        this.getDelegate().exitClassBlock(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterClassBody(ClassBodyContext ctx) {
        this.getDelegate().enterClassBody(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitClassBody(ClassBodyContext ctx) {
        this.getDelegate().exitClassBody(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterAbstractDeclaration(AbstractDeclarationContext ctx) {
        this.getDelegate().enterAbstractDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitAbstractDeclaration(AbstractDeclarationContext ctx) {
        this.getDelegate().exitAbstractDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterExtendsDeclaration(ExtendsDeclarationContext ctx) {
        this.getDelegate().enterExtendsDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitExtendsDeclaration(ExtendsDeclarationContext ctx) {
        this.getDelegate().exitExtendsDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterImplementsDeclaration(ImplementsDeclarationContext ctx) {
        this.getDelegate().enterImplementsDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitImplementsDeclaration(ImplementsDeclarationContext ctx) {
        this.getDelegate().exitImplementsDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterEnumerationDeclaration(EnumerationDeclarationContext ctx) {
        this.getDelegate().enterEnumerationDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitEnumerationDeclaration(EnumerationDeclarationContext ctx) {
        this.getDelegate().exitEnumerationDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterEnumerationBlock(EnumerationBlockContext ctx) {
        this.getDelegate().enterEnumerationBlock(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitEnumerationBlock(EnumerationBlockContext ctx) {
        this.getDelegate().exitEnumerationBlock(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterEnumerationBody(EnumerationBodyContext ctx) {
        this.getDelegate().enterEnumerationBody(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitEnumerationBody(EnumerationBodyContext ctx) {
        this.getDelegate().exitEnumerationBody(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterEnumerationLiteral(EnumerationLiteralContext ctx) {
        this.getDelegate().enterEnumerationLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitEnumerationLiteral(EnumerationLiteralContext ctx) {
        this.getDelegate().exitEnumerationLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterEnumerationPrettyName(EnumerationPrettyNameContext ctx) {
        this.getDelegate().enterEnumerationPrettyName(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitEnumerationPrettyName(EnumerationPrettyNameContext ctx) {
        this.getDelegate().exitEnumerationPrettyName(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterAssociationDeclaration(AssociationDeclarationContext ctx) {
        this.getDelegate().enterAssociationDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitAssociationDeclaration(AssociationDeclarationContext ctx) {
        this.getDelegate().exitAssociationDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterAssociationBlock(AssociationBlockContext ctx) {
        this.getDelegate().enterAssociationBlock(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitAssociationBlock(AssociationBlockContext ctx) {
        this.getDelegate().exitAssociationBlock(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterAssociationBody(AssociationBodyContext ctx) {
        this.getDelegate().enterAssociationBody(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitAssociationBody(AssociationBodyContext ctx) {
        this.getDelegate().exitAssociationBody(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterAssociationEnd(AssociationEndContext ctx) {
        this.getDelegate().enterAssociationEnd(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitAssociationEnd(AssociationEndContext ctx) {
        this.getDelegate().exitAssociationEnd(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterAssociationEndSignature(AssociationEndSignatureContext ctx) {
        this.getDelegate().enterAssociationEndSignature(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitAssociationEndSignature(AssociationEndSignatureContext ctx) {
        this.getDelegate().exitAssociationEndSignature(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterRelationship(RelationshipContext ctx) {
        this.getDelegate().enterRelationship(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitRelationship(RelationshipContext ctx) {
        this.getDelegate().exitRelationship(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterProjectionDeclaration(ProjectionDeclarationContext ctx) {
        this.getDelegate().enterProjectionDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitProjectionDeclaration(ProjectionDeclarationContext ctx) {
        this.getDelegate().exitProjectionDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterProjectionBlock(ProjectionBlockContext ctx) {
        this.getDelegate().enterProjectionBlock(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitProjectionBlock(ProjectionBlockContext ctx) {
        this.getDelegate().exitProjectionBlock(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterProjectionBody(ProjectionBodyContext ctx) {
        this.getDelegate().enterProjectionBody(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitProjectionBody(ProjectionBodyContext ctx) {
        this.getDelegate().exitProjectionBody(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterProjectionMember(ProjectionMemberContext ctx) {
        this.getDelegate().enterProjectionMember(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitProjectionMember(ProjectionMemberContext ctx) {
        this.getDelegate().exitProjectionMember(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterProjectionPrimitiveMember(ProjectionPrimitiveMemberContext ctx) {
        this.getDelegate().enterProjectionPrimitiveMember(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitProjectionPrimitiveMember(ProjectionPrimitiveMemberContext ctx) {
        this.getDelegate().exitProjectionPrimitiveMember(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterProjectionReferenceProperty(ProjectionReferencePropertyContext ctx) {
        this.getDelegate().enterProjectionReferenceProperty(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitProjectionReferenceProperty(ProjectionReferencePropertyContext ctx) {
        this.getDelegate().exitProjectionReferenceProperty(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterProjectionProjectionReference(ProjectionProjectionReferenceContext ctx) {
        this.getDelegate().enterProjectionProjectionReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitProjectionProjectionReference(ProjectionProjectionReferenceContext ctx) {
        this.getDelegate().exitProjectionProjectionReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterProjectionParameterizedProperty(ProjectionParameterizedPropertyContext ctx) {
        this.getDelegate().enterProjectionParameterizedProperty(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitProjectionParameterizedProperty(ProjectionParameterizedPropertyContext ctx) {
        this.getDelegate().exitProjectionParameterizedProperty(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterHeader(HeaderContext ctx) {
        this.getDelegate().enterHeader(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitHeader(HeaderContext ctx) {
        this.getDelegate().exitHeader(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterServiceGroupDeclaration(ServiceGroupDeclarationContext ctx) {
        this.getDelegate().enterServiceGroupDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitServiceGroupDeclaration(ServiceGroupDeclarationContext ctx) {
        this.getDelegate().exitServiceGroupDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterServiceGroupBlock(ServiceGroupBlockContext ctx) {
        this.getDelegate().enterServiceGroupBlock(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitServiceGroupBlock(ServiceGroupBlockContext ctx) {
        this.getDelegate().exitServiceGroupBlock(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterServiceGroupBody(ServiceGroupBodyContext ctx) {
        this.getDelegate().enterServiceGroupBody(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitServiceGroupBody(ServiceGroupBodyContext ctx) {
        this.getDelegate().exitServiceGroupBody(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterUrlDeclaration(UrlDeclarationContext ctx) {
        this.getDelegate().enterUrlDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitUrlDeclaration(UrlDeclarationContext ctx) {
        this.getDelegate().exitUrlDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterUrl(UrlContext ctx) {
        this.getDelegate().enterUrl(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitUrl(UrlContext ctx) {
        this.getDelegate().exitUrl(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterUrlPathSegment(UrlPathSegmentContext ctx) {
        this.getDelegate().enterUrlPathSegment(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitUrlPathSegment(UrlPathSegmentContext ctx) {
        this.getDelegate().exitUrlPathSegment(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterUrlConstant(UrlConstantContext ctx) {
        this.getDelegate().enterUrlConstant(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitUrlConstant(UrlConstantContext ctx) {
        this.getDelegate().exitUrlConstant(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterQueryParameterList(QueryParameterListContext ctx) {
        this.getDelegate().enterQueryParameterList(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitQueryParameterList(QueryParameterListContext ctx) {
        this.getDelegate().exitQueryParameterList(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterUrlParameterDeclaration(UrlParameterDeclarationContext ctx) {
        this.getDelegate().enterUrlParameterDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitUrlParameterDeclaration(UrlParameterDeclarationContext ctx) {
        this.getDelegate().exitUrlParameterDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterServiceDeclaration(ServiceDeclarationContext ctx) {
        this.getDelegate().enterServiceDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitServiceDeclaration(ServiceDeclarationContext ctx) {
        this.getDelegate().exitServiceDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterServiceBlock(ServiceBlockContext ctx) {
        this.getDelegate().enterServiceBlock(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitServiceBlock(ServiceBlockContext ctx) {
        this.getDelegate().exitServiceBlock(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterServiceBody(ServiceBodyContext ctx) {
        this.getDelegate().enterServiceBody(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitServiceBody(ServiceBodyContext ctx) {
        this.getDelegate().exitServiceBody(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterServiceMultiplicityDeclaration(ServiceMultiplicityDeclarationContext ctx) {
        this.getDelegate().enterServiceMultiplicityDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitServiceMultiplicityDeclaration(ServiceMultiplicityDeclarationContext ctx) {
        this.getDelegate().exitServiceMultiplicityDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterServiceMultiplicity(ServiceMultiplicityContext ctx) {
        this.getDelegate().enterServiceMultiplicity(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitServiceMultiplicity(ServiceMultiplicityContext ctx) {
        this.getDelegate().exitServiceMultiplicity(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterServiceCriteriaDeclaration(ServiceCriteriaDeclarationContext ctx) {
        this.getDelegate().enterServiceCriteriaDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitServiceCriteriaDeclaration(ServiceCriteriaDeclarationContext ctx) {
        this.getDelegate().exitServiceCriteriaDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterServiceCriteriaKeyword(ServiceCriteriaKeywordContext ctx) {
        this.getDelegate().enterServiceCriteriaKeyword(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitServiceCriteriaKeyword(ServiceCriteriaKeywordContext ctx) {
        this.getDelegate().exitServiceCriteriaKeyword(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterServiceProjectionDispatch(ServiceProjectionDispatchContext ctx) {
        this.getDelegate().enterServiceProjectionDispatch(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitServiceProjectionDispatch(ServiceProjectionDispatchContext ctx) {
        this.getDelegate().exitServiceProjectionDispatch(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterServiceOrderByDeclaration(ServiceOrderByDeclarationContext ctx) {
        this.getDelegate().enterServiceOrderByDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitServiceOrderByDeclaration(ServiceOrderByDeclarationContext ctx) {
        this.getDelegate().exitServiceOrderByDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterVerb(VerbContext ctx) {
        this.getDelegate().enterVerb(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitVerb(VerbContext ctx) {
        this.getDelegate().exitVerb(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterInterfaceMember(InterfaceMemberContext ctx) {
        this.getDelegate().enterInterfaceMember(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitInterfaceMember(InterfaceMemberContext ctx) {
        this.getDelegate().exitInterfaceMember(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterClassMember(ClassMemberContext ctx) {
        this.getDelegate().enterClassMember(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitClassMember(ClassMemberContext ctx) {
        this.getDelegate().exitClassMember(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterDataTypeProperty(DataTypePropertyContext ctx) {
        this.getDelegate().enterDataTypeProperty(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitDataTypeProperty(DataTypePropertyContext ctx) {
        this.getDelegate().exitDataTypeProperty(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterPrimitiveProperty(PrimitivePropertyContext ctx) {
        this.getDelegate().enterPrimitiveProperty(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitPrimitiveProperty(PrimitivePropertyContext ctx) {
        this.getDelegate().exitPrimitiveProperty(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterEnumerationProperty(EnumerationPropertyContext ctx) {
        this.getDelegate().enterEnumerationProperty(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitEnumerationProperty(EnumerationPropertyContext ctx) {
        this.getDelegate().exitEnumerationProperty(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterParameterizedProperty(ParameterizedPropertyContext ctx) {
        this.getDelegate().enterParameterizedProperty(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitParameterizedProperty(ParameterizedPropertyContext ctx) {
        this.getDelegate().exitParameterizedProperty(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterParameterizedPropertySignature(ParameterizedPropertySignatureContext ctx) {
        this.getDelegate().enterParameterizedPropertySignature(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitParameterizedPropertySignature(ParameterizedPropertySignatureContext ctx) {
        this.getDelegate().exitParameterizedPropertySignature(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterOptionalMarker(OptionalMarkerContext ctx) {
        this.getDelegate().enterOptionalMarker(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitOptionalMarker(OptionalMarkerContext ctx) {
        this.getDelegate().exitOptionalMarker(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterDataTypePropertyValidation(DataTypePropertyValidationContext ctx) {
        this.getDelegate().enterDataTypePropertyValidation(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitDataTypePropertyValidation(DataTypePropertyValidationContext ctx) {
        this.getDelegate().exitDataTypePropertyValidation(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterMinLengthValidation(MinLengthValidationContext ctx) {
        this.getDelegate().enterMinLengthValidation(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitMinLengthValidation(MinLengthValidationContext ctx) {
        this.getDelegate().exitMinLengthValidation(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterMaxLengthValidation(MaxLengthValidationContext ctx) {
        this.getDelegate().enterMaxLengthValidation(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitMaxLengthValidation(MaxLengthValidationContext ctx) {
        this.getDelegate().exitMaxLengthValidation(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterMinValidation(MinValidationContext ctx) {
        this.getDelegate().enterMinValidation(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitMinValidation(MinValidationContext ctx) {
        this.getDelegate().exitMinValidation(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterMaxValidation(MaxValidationContext ctx) {
        this.getDelegate().enterMaxValidation(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitMaxValidation(MaxValidationContext ctx) {
        this.getDelegate().exitMaxValidation(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterIntegerValidationParameter(IntegerValidationParameterContext ctx) {
        this.getDelegate().enterIntegerValidationParameter(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitIntegerValidationParameter(IntegerValidationParameterContext ctx) {
        this.getDelegate().exitIntegerValidationParameter(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterMinLengthValidationKeyword(MinLengthValidationKeywordContext ctx) {
        this.getDelegate().enterMinLengthValidationKeyword(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitMinLengthValidationKeyword(MinLengthValidationKeywordContext ctx) {
        this.getDelegate().exitMinLengthValidationKeyword(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterMaxLengthValidationKeyword(MaxLengthValidationKeywordContext ctx) {
        this.getDelegate().enterMaxLengthValidationKeyword(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitMaxLengthValidationKeyword(MaxLengthValidationKeywordContext ctx) {
        this.getDelegate().exitMaxLengthValidationKeyword(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterMinValidationKeyword(MinValidationKeywordContext ctx) {
        this.getDelegate().enterMinValidationKeyword(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitMinValidationKeyword(MinValidationKeywordContext ctx) {
        this.getDelegate().exitMinValidationKeyword(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterMaxValidationKeyword(MaxValidationKeywordContext ctx) {
        this.getDelegate().enterMaxValidationKeyword(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitMaxValidationKeyword(MaxValidationKeywordContext ctx) {
        this.getDelegate().exitMaxValidationKeyword(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterParameterDeclaration(ParameterDeclarationContext ctx) {
        this.getDelegate().enterParameterDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitParameterDeclaration(ParameterDeclarationContext ctx) {
        this.getDelegate().exitParameterDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterPrimitiveParameterDeclaration(PrimitiveParameterDeclarationContext ctx) {
        this.getDelegate().enterPrimitiveParameterDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitPrimitiveParameterDeclaration(PrimitiveParameterDeclarationContext ctx) {
        this.getDelegate().exitPrimitiveParameterDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterEnumerationParameterDeclaration(EnumerationParameterDeclarationContext ctx) {
        this.getDelegate().enterEnumerationParameterDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitEnumerationParameterDeclaration(EnumerationParameterDeclarationContext ctx) {
        this.getDelegate().exitEnumerationParameterDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterInvalidParameterDeclaration(InvalidParameterDeclarationContext ctx) {
        this.getDelegate().enterInvalidParameterDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitInvalidParameterDeclaration(InvalidParameterDeclarationContext ctx) {
        this.getDelegate().exitInvalidParameterDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterParameterDeclarationList(ParameterDeclarationListContext ctx) {
        this.getDelegate().enterParameterDeclarationList(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitParameterDeclarationList(ParameterDeclarationListContext ctx) {
        this.getDelegate().exitParameterDeclarationList(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterArgumentList(ArgumentListContext ctx) {
        this.getDelegate().enterArgumentList(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitArgumentList(ArgumentListContext ctx) {
        this.getDelegate().exitArgumentList(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterArgument(ArgumentContext ctx) {
        this.getDelegate().enterArgument(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitArgument(ArgumentContext ctx) {
        this.getDelegate().exitArgument(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterMultiplicity(MultiplicityContext ctx) {
        this.getDelegate().enterMultiplicity(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitMultiplicity(MultiplicityContext ctx) {
        this.getDelegate().exitMultiplicity(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterMultiplicityBody(MultiplicityBodyContext ctx) {
        this.getDelegate().enterMultiplicityBody(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitMultiplicityBody(MultiplicityBodyContext ctx) {
        this.getDelegate().exitMultiplicityBody(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterPrimitiveType(PrimitiveTypeContext ctx) {
        this.getDelegate().enterPrimitiveType(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitPrimitiveType(PrimitiveTypeContext ctx) {
        this.getDelegate().exitPrimitiveType(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterClassifierModifier(ClassifierModifierContext ctx) {
        this.getDelegate().enterClassifierModifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitClassifierModifier(ClassifierModifierContext ctx) {
        this.getDelegate().exitClassifierModifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterDataTypePropertyModifier(DataTypePropertyModifierContext ctx) {
        this.getDelegate().enterDataTypePropertyModifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitDataTypePropertyModifier(DataTypePropertyModifierContext ctx) {
        this.getDelegate().exitDataTypePropertyModifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterAssociationEndModifier(AssociationEndModifierContext ctx) {
        this.getDelegate().enterAssociationEndModifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitAssociationEndModifier(AssociationEndModifierContext ctx) {
        this.getDelegate().exitAssociationEndModifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterParameterizedPropertyModifier(ParameterizedPropertyModifierContext ctx) {
        this.getDelegate().enterParameterizedPropertyModifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitParameterizedPropertyModifier(ParameterizedPropertyModifierContext ctx) {
        this.getDelegate().exitParameterizedPropertyModifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterParameterModifier(ParameterModifierContext ctx) {
        this.getDelegate().enterParameterModifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitParameterModifier(ParameterModifierContext ctx) {
        this.getDelegate().exitParameterModifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterOrderByDeclaration(OrderByDeclarationContext ctx) {
        this.getDelegate().enterOrderByDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitOrderByDeclaration(OrderByDeclarationContext ctx) {
        this.getDelegate().exitOrderByDeclaration(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterOrderByMemberReferencePath(OrderByMemberReferencePathContext ctx) {
        this.getDelegate().enterOrderByMemberReferencePath(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitOrderByMemberReferencePath(OrderByMemberReferencePathContext ctx) {
        this.getDelegate().exitOrderByMemberReferencePath(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterOrderByDirection(OrderByDirectionContext ctx) {
        this.getDelegate().enterOrderByDirection(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitOrderByDirection(OrderByDirectionContext ctx) {
        this.getDelegate().exitOrderByDirection(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterCriteriaEdgePoint(CriteriaEdgePointContext ctx) {
        this.getDelegate().enterCriteriaEdgePoint(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitCriteriaEdgePoint(CriteriaEdgePointContext ctx) {
        this.getDelegate().exitCriteriaEdgePoint(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterCriteriaExpressionAnd(CriteriaExpressionAndContext ctx) {
        this.getDelegate().enterCriteriaExpressionAnd(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitCriteriaExpressionAnd(CriteriaExpressionAndContext ctx) {
        this.getDelegate().exitCriteriaExpressionAnd(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterCriteriaNative(CriteriaNativeContext ctx) {
        this.getDelegate().enterCriteriaNative(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitCriteriaNative(CriteriaNativeContext ctx) {
        this.getDelegate().exitCriteriaNative(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterCriteriaExpressionGroup(CriteriaExpressionGroupContext ctx) {
        this.getDelegate().enterCriteriaExpressionGroup(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitCriteriaExpressionGroup(CriteriaExpressionGroupContext ctx) {
        this.getDelegate().exitCriteriaExpressionGroup(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterCriteriaAll(CriteriaAllContext ctx) {
        this.getDelegate().enterCriteriaAll(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitCriteriaAll(CriteriaAllContext ctx) {
        this.getDelegate().exitCriteriaAll(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterCriteriaOperator(CriteriaOperatorContext ctx) {
        this.getDelegate().enterCriteriaOperator(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitCriteriaOperator(CriteriaOperatorContext ctx) {
        this.getDelegate().exitCriteriaOperator(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterCriteriaExpressionOr(CriteriaExpressionOrContext ctx) {
        this.getDelegate().enterCriteriaExpressionOr(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitCriteriaExpressionOr(CriteriaExpressionOrContext ctx) {
        this.getDelegate().exitCriteriaExpressionOr(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterExpressionValue(ExpressionValueContext ctx) {
        this.getDelegate().enterExpressionValue(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitExpressionValue(ExpressionValueContext ctx) {
        this.getDelegate().exitExpressionValue(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterExpressionMemberReference(ExpressionMemberReferenceContext ctx) {
        this.getDelegate().enterExpressionMemberReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitExpressionMemberReference(ExpressionMemberReferenceContext ctx) {
        this.getDelegate().exitExpressionMemberReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterLiteralList(LiteralListContext ctx) {
        this.getDelegate().enterLiteralList(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitLiteralList(LiteralListContext ctx) {
        this.getDelegate().exitLiteralList(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterNativeLiteral(NativeLiteralContext ctx) {
        this.getDelegate().enterNativeLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitNativeLiteral(NativeLiteralContext ctx) {
        this.getDelegate().exitNativeLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterOperator(OperatorContext ctx) {
        this.getDelegate().enterOperator(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitOperator(OperatorContext ctx) {
        this.getDelegate().exitOperator(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterEqualityOperator(EqualityOperatorContext ctx) {
        this.getDelegate().enterEqualityOperator(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitEqualityOperator(EqualityOperatorContext ctx) {
        this.getDelegate().exitEqualityOperator(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterInequalityOperator(InequalityOperatorContext ctx) {
        this.getDelegate().enterInequalityOperator(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitInequalityOperator(InequalityOperatorContext ctx) {
        this.getDelegate().exitInequalityOperator(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterInOperator(InOperatorContext ctx) {
        this.getDelegate().enterInOperator(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitInOperator(InOperatorContext ctx) {
        this.getDelegate().exitInOperator(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterStringOperator(StringOperatorContext ctx) {
        this.getDelegate().enterStringOperator(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitStringOperator(StringOperatorContext ctx) {
        this.getDelegate().exitStringOperator(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterInterfaceReference(InterfaceReferenceContext ctx) {
        this.getDelegate().enterInterfaceReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitInterfaceReference(InterfaceReferenceContext ctx) {
        this.getDelegate().exitInterfaceReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterClassReference(ClassReferenceContext ctx) {
        this.getDelegate().enterClassReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitClassReference(ClassReferenceContext ctx) {
        this.getDelegate().exitClassReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterClassifierReference(ClassifierReferenceContext ctx) {
        this.getDelegate().enterClassifierReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitClassifierReference(ClassifierReferenceContext ctx) {
        this.getDelegate().exitClassifierReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterEnumerationReference(EnumerationReferenceContext ctx) {
        this.getDelegate().enterEnumerationReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitEnumerationReference(EnumerationReferenceContext ctx) {
        this.getDelegate().exitEnumerationReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterProjectionReference(ProjectionReferenceContext ctx) {
        this.getDelegate().enterProjectionReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitProjectionReference(ProjectionReferenceContext ctx) {
        this.getDelegate().exitProjectionReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterMemberReference(MemberReferenceContext ctx) {
        this.getDelegate().enterMemberReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitMemberReference(MemberReferenceContext ctx) {
        this.getDelegate().exitMemberReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterAssociationEndReference(AssociationEndReferenceContext ctx) {
        this.getDelegate().enterAssociationEndReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitAssociationEndReference(AssociationEndReferenceContext ctx) {
        this.getDelegate().exitAssociationEndReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterParameterReference(ParameterReferenceContext ctx) {
        this.getDelegate().enterParameterReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitParameterReference(ParameterReferenceContext ctx) {
        this.getDelegate().exitParameterReference(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterThisMemberReferencePath(ThisMemberReferencePathContext ctx) {
        this.getDelegate().enterThisMemberReferencePath(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitThisMemberReferencePath(ThisMemberReferencePathContext ctx) {
        this.getDelegate().exitThisMemberReferencePath(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterTypeMemberReferencePath(TypeMemberReferencePathContext ctx) {
        this.getDelegate().enterTypeMemberReferencePath(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitTypeMemberReferencePath(TypeMemberReferencePathContext ctx) {
        this.getDelegate().exitTypeMemberReferencePath(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterIdentifier(IdentifierContext ctx) {
        this.getDelegate().enterIdentifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitIdentifier(IdentifierContext ctx) {
        this.getDelegate().exitIdentifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterKeywordValidAsIdentifier(KeywordValidAsIdentifierContext ctx) {
        this.getDelegate().enterKeywordValidAsIdentifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitKeywordValidAsIdentifier(KeywordValidAsIdentifierContext ctx) {
        this.getDelegate().exitKeywordValidAsIdentifier(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterLiteral(LiteralContext ctx) {
        this.getDelegate().enterLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitLiteral(LiteralContext ctx) {
        this.getDelegate().exitLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterIntegerLiteral(IntegerLiteralContext ctx) {
        this.getDelegate().enterIntegerLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitIntegerLiteral(IntegerLiteralContext ctx) {
        this.getDelegate().exitIntegerLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterFloatingPointLiteral(FloatingPointLiteralContext ctx) {
        this.getDelegate().enterFloatingPointLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitFloatingPointLiteral(FloatingPointLiteralContext ctx) {
        this.getDelegate().exitFloatingPointLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterBooleanLiteral(BooleanLiteralContext ctx) {
        this.getDelegate().enterBooleanLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitBooleanLiteral(BooleanLiteralContext ctx) {
        this.getDelegate().exitBooleanLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterCharacterLiteral(CharacterLiteralContext ctx) {
        this.getDelegate().enterCharacterLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitCharacterLiteral(CharacterLiteralContext ctx) {
        this.getDelegate().exitCharacterLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterStringLiteral(StringLiteralContext ctx) {
        this.getDelegate().enterStringLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitStringLiteral(StringLiteralContext ctx) {
        this.getDelegate().exitStringLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterNullLiteral(NullLiteralContext ctx) {
        this.getDelegate().enterNullLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitNullLiteral(NullLiteralContext ctx) {
        this.getDelegate().exitNullLiteral(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void visitTerminal(TerminalNode node) {
        this.getDelegate().visitTerminal(node);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void visitErrorNode(ErrorNode node) {
        this.getDelegate().visitErrorNode(node);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void enterEveryRule(ParserRuleContext ctx) {
        this.getDelegate().enterEveryRule(ctx);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitEveryRule(ParserRuleContext ctx) {
        this.getDelegate().exitEveryRule(ctx);
    }
}
