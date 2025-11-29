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

package cool.klass.model.converter.compiler;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.state.AntlrAssociation;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.AntlrCompilationUnit;
import cool.klass.model.converter.compiler.state.AntlrDomainModel;
import cool.klass.model.converter.compiler.state.AntlrEnumeration;
import cool.klass.model.converter.compiler.state.AntlrInterface;
import cool.klass.model.converter.compiler.state.AntlrTopLevelElement;
import cool.klass.model.converter.compiler.state.order.AntlrOrderBy;
import cool.klass.model.converter.compiler.state.order.AntlrOrderByMemberReferencePath;
import cool.klass.model.converter.compiler.state.order.AntlrOrderByOwner;
import cool.klass.model.converter.compiler.state.parameter.AntlrParameter;
import cool.klass.model.converter.compiler.state.parameter.AntlrParameterOwner;
import cool.klass.model.converter.compiler.state.projection.AntlrProjection;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEnd;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEndSignature;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import cool.klass.model.converter.compiler.state.property.AntlrParameterizedProperty;
import cool.klass.model.converter.compiler.state.service.AntlrService;
import cool.klass.model.converter.compiler.state.service.AntlrServiceGroup;
import cool.klass.model.converter.compiler.state.service.url.AntlrUrl;
import cool.klass.model.meta.grammar.KlassBaseListener;
import cool.klass.model.meta.grammar.KlassListener;
import cool.klass.model.meta.grammar.KlassParser.AssociationDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndSignatureContext;
import cool.klass.model.meta.grammar.KlassParser.ClassDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ClassMemberContext;
import cool.klass.model.meta.grammar.KlassParser.ClassifierModifierContext;
import cool.klass.model.meta.grammar.KlassParser.CompilationUnitContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceMemberContext;
import cool.klass.model.meta.grammar.KlassParser.OrderByDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.OrderByMemberReferencePathContext;
import cool.klass.model.meta.grammar.KlassParser.PackageDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.PackageNameContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterizedPropertyContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.RelationshipContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceGroupDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.TopLevelDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.UrlDeclarationContext;

public class CompilerWalkState {

    private final AntlrDomainModel domainModel;

    @Nullable
    private CompilationUnit currentCompilationUnit;

    @Nullable
    private String packageName;

    @Nullable
    private AntlrCompilationUnit compilationUnit;

    @Nullable
    private AntlrTopLevelElement topLevelDeclaration;

    @Nullable
    private AntlrEnumeration enumeration;

    @Nullable
    private AntlrClassifier classifier;

    @Nullable
    private AntlrInterface iface;

    @Nullable
    private AntlrClass klass;

    @Nullable
    private AntlrAssociation association;

    @Nullable
    private AntlrAssociationEnd associationEnd;

    @Nullable
    private AntlrAssociationEndSignature associationEndSignature;

    @Nullable
    private AntlrParameterizedProperty parameterizedProperty;

    @Nullable
    private AntlrParameter parameter;

    @Nullable
    private AntlrProjection projection;

    @Nullable
    private AntlrServiceGroup serviceGroup;

    @Nullable
    private AntlrUrl url;

    @Nullable
    private AntlrService service;

    @Nullable
    private AntlrOrderBy orderBy;

    @Nullable
    private AntlrOrderByMemberReferencePath orderByMemberReferencePath;

    @Nullable
    private AntlrClassifier thisReference;

    @Nullable
    private AntlrOrderByOwner orderByOwner;

    @Nullable
    private AntlrModifier classifierModifier;

    @Nullable
    private PackageNameContext packageNameContext;

    @Nullable
    private AntlrParameterOwner parameterOwner;

    private int numClassifierMembers;

    public CompilerWalkState(AntlrDomainModel domainModel) {
        this.domainModel = domainModel;
    }

    private static void assertNull(@Nullable Object object) {
        if (object != null) {
            throw new IllegalStateException("Expected null but was " + object);
        }
    }

    private static void assertZero(int number) {
        if (number != 0) {
            throw new IllegalStateException("Expected 0 but was " + number);
        }
    }

    @Nullable
    public String getPackageName() {
        return this.packageName;
    }

    @Nullable
    public PackageNameContext getPackageNameContext() {
        return this.packageNameContext;
    }

    @Nullable
    public AntlrClassifier getClassifier() {
        return this.classifier;
    }

    @Nullable
    public AntlrInterface getInterface() {
        return this.iface;
    }

    @Nullable
    public AntlrClass getKlass() {
        return this.klass;
    }

    @Nullable
    public AntlrAssociation getAssociation() {
        return this.association;
    }

    @Nullable
    public AntlrAssociationEnd getAssociationEnd() {
        return this.associationEnd;
    }

    @Nullable
    public AntlrServiceGroup getServiceGroup() {
        return this.serviceGroup;
    }

    @Nullable
    public AntlrUrl getUrl() {
        return this.url;
    }

    @Nullable
    public AntlrProjection getProjection() {
        return this.projection;
    }

    @Nullable
    public AntlrService getService() {
        return this.service;
    }

    @Nullable
    public AntlrOrderBy getOrderBy() {
        return this.orderBy;
    }

    @Nullable
    public AntlrOrderByMemberReferencePath getOrderByMemberReferencePath() {
        return this.orderByMemberReferencePath;
    }

    @Nullable
    public AntlrOrderByOwner getOrderByOwner() {
        return this.orderByOwner;
    }

    @Nullable
    public AntlrClassifier getThisReference() {
        return this.thisReference;
    }

    @Nullable
    public AntlrModifier getClassifierModifier() {
        return this.classifierModifier;
    }

    @Nullable
    public AntlrParameterOwner getParameterOwner() {
        return this.parameterOwner;
    }

    public int getNumClassifierMembers() {
        return this.numClassifierMembers;
    }

    @Nonnull
    public CompilationUnit getCurrentCompilationUnit() {
        return Objects.requireNonNull(this.currentCompilationUnit);
    }

    @Nullable
    public AntlrCompilationUnit getCompilationUnit() {
        return Objects.requireNonNull(this.compilationUnit);
    }

    public void enterCompilationUnit(CompilationUnit currentCompilationUnit) {
        this.currentCompilationUnit = currentCompilationUnit;

        if (this.compilationUnit == null) {
            return;
        }

        if (this.compilationUnit.getElementContext() != this.currentCompilationUnit.getParserContext()) {
            throw new AssertionError();
        }
    }

    public void exitCompilationUnit() {
        this.currentCompilationUnit = null;
        this.packageName = null;
        this.packageNameContext = null;
    }

    public void withInPlaceCompilationUnit(CompilationUnit compilationUnit, Runnable runnable) {
        CompilationUnit oldCompilationUnit = this.currentCompilationUnit;
        try {
            this.currentCompilationUnit = compilationUnit;
            runnable.run();
        } finally {
            this.currentCompilationUnit = oldCompilationUnit;
        }
    }

    public void assertEmpty() {
        if (this.packageNameContext != null) {
            throw new AssertionError();
        }
        if (this.packageName != null) {
            throw new AssertionError();
        }
        if (this.enumeration != null) {
            throw new AssertionError();
        }
        if (this.classifier != null) {
            throw new AssertionError();
        }
        if (this.iface != null) {
            throw new AssertionError();
        }
        if (this.klass != null) {
            throw new AssertionError();
        }
        if (this.association != null) {
            throw new AssertionError();
        }
        if (this.associationEnd != null) {
            throw new AssertionError();
        }
        if (this.parameterizedProperty != null) {
            throw new AssertionError();
        }
        if (this.parameter != null) {
            throw new AssertionError();
        }
        if (this.projection != null) {
            throw new AssertionError();
        }
        if (this.serviceGroup != null) {
            throw new AssertionError();
        }
        if (this.url != null) {
            throw new AssertionError();
        }
        if (this.service != null) {
            throw new AssertionError();
        }
        if (this.orderBy != null) {
            throw new AssertionError();
        }
        if (this.orderByMemberReferencePath != null) {
            throw new AssertionError();
        }
        if (this.thisReference != null) {
            throw new AssertionError();
        }
        if (this.orderByOwner != null) {
            throw new AssertionError();
        }
        if (this.classifierModifier != null) {
            throw new AssertionError();
        }
        if (this.parameterOwner != null) {
            throw new AssertionError();
        }
        if (this.numClassifierMembers != 0) {
            throw new AssertionError();
        }
    }

    public void assertEquals(@Nonnull CompilerWalkState other) {
        if (!Objects.equals(this.domainModel, other.domainModel)) {
            throw new AssertionError();
        }
        if (this.compilationUnit != other.compilationUnit) {
            throw new AssertionError();
        }
        if (this.currentCompilationUnit != other.currentCompilationUnit) {
            throw new AssertionError();
        }
        if (!Objects.equals(this.packageName, other.packageName)) {
            throw new AssertionError();
        }
        if (this.topLevelDeclaration != other.topLevelDeclaration) {
            throw new AssertionError();
        }
        if (this.enumeration != other.enumeration) {
            throw new AssertionError();
        }
        if (this.classifier != other.classifier) {
            throw new AssertionError();
        }
        if (this.iface != other.iface) {
            throw new AssertionError();
        }
        if (this.klass != other.klass) {
            throw new AssertionError();
        }
        if (this.association != other.association) {
            throw new AssertionError();
        }
        if (this.associationEnd != other.associationEnd) {
            throw new AssertionError();
        }
        if (this.associationEndSignature != other.associationEndSignature) {
            throw new AssertionError();
        }
        if (this.parameterizedProperty != other.parameterizedProperty) {
            throw new AssertionError();
        }
        if (this.parameter != other.parameter) {
            throw new AssertionError();
        }
        if (this.projection != other.projection) {
            throw new AssertionError();
        }
        if (this.serviceGroup != other.serviceGroup) {
            throw new AssertionError();
        }
        if (this.url != other.url) {
            throw new AssertionError();
        }
        if (this.service != other.service) {
            throw new AssertionError();
        }
        if (this.orderBy != other.orderBy) {
            throw new AssertionError();
        }
        if (this.orderByMemberReferencePath != other.orderByMemberReferencePath) {
            throw new AssertionError();
        }
        if (this.thisReference != other.thisReference) {
            throw new AssertionError();
        }
        if (this.orderByOwner != other.orderByOwner) {
            throw new AssertionError();
        }
        if (this.classifierModifier != other.classifierModifier) {
            throw new AssertionError();
        }
        if (this.parameterOwner != other.parameterOwner) {
            throw new AssertionError();
        }
        if (this.numClassifierMembers != other.numClassifierMembers) {
            throw new AssertionError();
        }
    }

    @Nonnull
    public KlassListener asListener() {
        return new ListenerView();
    }

    public class ListenerView extends KlassBaseListener {

        @Override
        public void enterCompilationUnit(CompilationUnitContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.compilationUnit);
            CompilerWalkState.this.compilationUnit = CompilerWalkState.this.domainModel.getCompilationUnitByContext(
                ctx
            );
        }

        @Override
        public void exitCompilationUnit(CompilationUnitContext ctx) {
            CompilerWalkState.this.compilationUnit = null;
        }

        @Override
        public void enterPackageDeclaration(@Nonnull PackageDeclarationContext packageContext) {
            CompilerWalkState.this.packageNameContext = packageContext.packageName();
            CompilerWalkState.this.packageName = CompilerWalkState.this.packageNameContext.getText();
        }

        @Override
        public void enterTopLevelDeclaration(@Nonnull TopLevelDeclarationContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.topLevelDeclaration);
            CompilerWalkState.this.topLevelDeclaration = CompilerWalkState.this.domainModel.getTopLevelElementByContext(
                ctx
            );
        }

        @Override
        public void exitTopLevelDeclaration(@Nonnull TopLevelDeclarationContext ctx) {
            CompilerWalkState.this.topLevelDeclaration = null;
        }

        @Override
        public void enterInterfaceDeclaration(@Nonnull InterfaceDeclarationContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.classifier);
            CompilerWalkState.assertNull(CompilerWalkState.this.iface);
            CompilerWalkState.assertNull(CompilerWalkState.this.thisReference);
            CompilerWalkState.assertZero(CompilerWalkState.this.numClassifierMembers);

            AntlrInterface interfaceByContext = CompilerWalkState.this.domainModel.getInterfaceByContext(ctx);

            CompilerWalkState.this.classifier = interfaceByContext;
            CompilerWalkState.this.iface = interfaceByContext;
            CompilerWalkState.this.thisReference = interfaceByContext;
        }

        @Override
        public void exitInterfaceDeclaration(@Nonnull InterfaceDeclarationContext ctx) {
            CompilerWalkState.this.classifier = null;
            CompilerWalkState.this.iface = null;
            CompilerWalkState.this.thisReference = null;
            CompilerWalkState.this.numClassifierMembers = 0;
        }

        @Override
        public void enterClassDeclaration(@Nonnull ClassDeclarationContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.classifier);
            CompilerWalkState.assertNull(CompilerWalkState.this.klass);
            CompilerWalkState.assertNull(CompilerWalkState.this.thisReference);
            CompilerWalkState.assertZero(CompilerWalkState.this.numClassifierMembers);

            AntlrClass classByContext = CompilerWalkState.this.domainModel.getClassByContext(ctx);

            CompilerWalkState.this.classifier = classByContext;
            CompilerWalkState.this.klass = classByContext;
            CompilerWalkState.this.thisReference = classByContext;
        }

        @Override
        public void exitClassDeclaration(@Nonnull ClassDeclarationContext ctx) {
            CompilerWalkState.this.classifier = null;
            CompilerWalkState.this.klass = null;
            CompilerWalkState.this.thisReference = null;
            CompilerWalkState.this.numClassifierMembers = 0;
        }

        @Override
        public void enterEnumerationDeclaration(@Nonnull EnumerationDeclarationContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.enumeration);
            CompilerWalkState.this.enumeration = CompilerWalkState.this.domainModel.getEnumerationByContext(ctx);
        }

        @Override
        public void exitEnumerationDeclaration(@Nonnull EnumerationDeclarationContext ctx) {
            CompilerWalkState.this.enumeration = null;
        }

        @Override
        public void enterAssociationDeclaration(@Nonnull AssociationDeclarationContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.association);
            CompilerWalkState.this.association = CompilerWalkState.this.domainModel.getAssociationByContext(ctx);
        }

        @Override
        public void exitAssociationDeclaration(@Nonnull AssociationDeclarationContext ctx) {
            CompilerWalkState.this.association = null;
        }

        @Override
        public void enterAssociationEnd(@Nonnull AssociationEndContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.associationEnd);
            CompilerWalkState.assertNull(CompilerWalkState.this.thisReference);
            CompilerWalkState.assertNull(CompilerWalkState.this.orderByOwner);

            if (CompilerWalkState.this.association == null) {
                return;
            }

            CompilerWalkState.this.associationEnd = CompilerWalkState.this.association.getAssociationEndByContext(ctx);
            if (CompilerWalkState.this.associationEnd == null) {
                return;
            }
            CompilerWalkState.this.orderByOwner = CompilerWalkState.this.associationEnd;
            CompilerWalkState.this.thisReference = CompilerWalkState.this.associationEnd.getType();
        }

        @Override
        public void exitAssociationEnd(@Nonnull AssociationEndContext ctx) {
            CompilerWalkState.this.associationEnd = null;
            CompilerWalkState.this.orderByOwner = null;
            CompilerWalkState.this.thisReference = null;
        }

        @Override
        public void enterAssociationEndSignature(@Nonnull AssociationEndSignatureContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.associationEndSignature);
            CompilerWalkState.assertNull(CompilerWalkState.this.orderByOwner);

            if (CompilerWalkState.this.classifier == null) {
                return;
            }

            CompilerWalkState.this.associationEndSignature =
                CompilerWalkState.this.classifier.getDeclaredAssociationEndSignatureByContext(ctx);
            if (CompilerWalkState.this.associationEndSignature == null) {
                return;
            }
            CompilerWalkState.this.orderByOwner = CompilerWalkState.this.associationEndSignature;
        }

        @Override
        public void exitAssociationEndSignature(@Nonnull AssociationEndSignatureContext ctx) {
            CompilerWalkState.this.associationEndSignature = null;
            CompilerWalkState.this.orderByOwner = null;
        }

        @Override
        public void enterRelationship(@Nonnull RelationshipContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.thisReference);

            if (CompilerWalkState.this.association == null) {
                return;
            }

            CompilerWalkState.this.thisReference = CompilerWalkState.this.association.getAssociationEnds()
                .getFirstOptional()
                .map(AntlrAssociationEnd::getType)
                .orElse(AntlrClass.NOT_FOUND);
        }

        @Override
        public void exitRelationship(@Nonnull RelationshipContext ctx) {
            CompilerWalkState.this.thisReference = null;
        }

        @Override
        public void enterProjectionDeclaration(@Nonnull ProjectionDeclarationContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.projection);

            CompilerWalkState.this.projection = CompilerWalkState.this.domainModel.getProjectionByContext(ctx);
        }

        @Override
        public void exitProjectionDeclaration(@Nonnull ProjectionDeclarationContext ctx) {
            CompilerWalkState.this.projection = null;
        }

        @Override
        public void enterServiceGroupDeclaration(@Nonnull ServiceGroupDeclarationContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.serviceGroup);
            CompilerWalkState.assertNull(CompilerWalkState.this.thisReference);

            CompilerWalkState.this.serviceGroup = CompilerWalkState.this.domainModel.getServiceGroupByContext(ctx);
            if (CompilerWalkState.this.serviceGroup == null) {
                return;
            }
            CompilerWalkState.this.thisReference = CompilerWalkState.this.serviceGroup.getKlass();
        }

        @Override
        public void exitServiceGroupDeclaration(@Nonnull ServiceGroupDeclarationContext ctx) {
            CompilerWalkState.this.serviceGroup = null;
            CompilerWalkState.this.thisReference = null;
        }

        @Override
        public void enterUrlDeclaration(@Nonnull UrlDeclarationContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.url);
            if (CompilerWalkState.this.serviceGroup == null) {
                return;
            }
            CompilerWalkState.this.url = CompilerWalkState.this.serviceGroup.getUrlByContext(ctx);
        }

        @Override
        public void exitUrlDeclaration(@Nonnull UrlDeclarationContext ctx) {
            CompilerWalkState.this.url = null;
        }

        @Override
        public void enterServiceDeclaration(@Nonnull ServiceDeclarationContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.service);
            CompilerWalkState.assertNull(CompilerWalkState.this.orderByOwner);

            if (CompilerWalkState.this.url == null) {
                return;
            }
            CompilerWalkState.this.service = CompilerWalkState.this.url.getServiceByContext(ctx);
            if (CompilerWalkState.this.service == null) {
                return;
            }
            CompilerWalkState.this.orderByOwner = CompilerWalkState.this.service;
        }

        @Override
        public void exitServiceDeclaration(@Nonnull ServiceDeclarationContext ctx) {
            CompilerWalkState.this.service = null;
            CompilerWalkState.this.orderByOwner = null;
        }

        @Override
        public void enterInterfaceMember(InterfaceMemberContext ctx) {
            CompilerWalkState.this.numClassifierMembers++;
        }

        @Override
        public void enterClassMember(ClassMemberContext ctx) {
            CompilerWalkState.this.numClassifierMembers++;
        }

        @Override
        public void enterParameterizedProperty(@Nonnull ParameterizedPropertyContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.parameterizedProperty);
            CompilerWalkState.assertNull(CompilerWalkState.this.orderByOwner);
            CompilerWalkState.assertNull(CompilerWalkState.this.parameterOwner);

            if (CompilerWalkState.this.klass == null) {
                return;
            }
            CompilerWalkState.this.parameterizedProperty =
                CompilerWalkState.this.klass.getParameterizedPropertyByContext(ctx);
            CompilerWalkState.this.orderByOwner = CompilerWalkState.this.parameterizedProperty;
            CompilerWalkState.this.parameterOwner = CompilerWalkState.this.parameterizedProperty;
        }

        @Override
        public void exitParameterizedProperty(@Nonnull ParameterizedPropertyContext ctx) {
            CompilerWalkState.this.parameterizedProperty = null;
            CompilerWalkState.this.orderByOwner = null;
            CompilerWalkState.this.parameterOwner = null;
        }

        @Override
        public void enterParameterDeclaration(@Nonnull ParameterDeclarationContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.parameter);

            if (CompilerWalkState.this.parameterOwner == null) {
                return;
            }

            CompilerWalkState.this.parameter = CompilerWalkState.this.parameterOwner.getParameterByContext(ctx);
        }

        @Override
        public void exitParameterDeclaration(@Nonnull ParameterDeclarationContext ctx) {
            CompilerWalkState.this.parameter = null;
        }

        @Override
        public void enterClassifierModifier(@Nonnull ClassifierModifierContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.classifierModifier);

            if (CompilerWalkState.this.classifier == null) {
                return;
            }

            CompilerWalkState.this.classifierModifier = CompilerWalkState.this.classifier.getDeclaredModifierByContext(
                ctx
            );
        }

        @Override
        public void exitClassifierModifier(@Nonnull ClassifierModifierContext ctx) {
            CompilerWalkState.this.classifierModifier = null;
        }

        @Override
        public void enterOrderByDeclaration(OrderByDeclarationContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.orderBy);

            if (CompilerWalkState.this.orderByOwner == null) {
                return;
            }

            CompilerWalkState.this.orderByOwner.getOrderBy().ifPresent((antlrOrderBy) ->
                CompilerWalkState.this.orderBy = antlrOrderBy
            );
        }

        @Override
        public void exitOrderByDeclaration(OrderByDeclarationContext ctx) {
            CompilerWalkState.this.orderBy = null;
        }

        @Override
        public void enterOrderByMemberReferencePath(OrderByMemberReferencePathContext ctx) {
            CompilerWalkState.assertNull(CompilerWalkState.this.orderByMemberReferencePath);

            if (CompilerWalkState.this.orderBy == null) {
                return;
            }

            CompilerWalkState.this.orderByMemberReferencePath = Objects.requireNonNull(
                CompilerWalkState.this.orderBy.getOrderByMemberReferencePath(ctx)
            );
        }

        @Override
        public void exitOrderByMemberReferencePath(OrderByMemberReferencePathContext ctx) {
            CompilerWalkState.this.orderByMemberReferencePath = null;
        }
    }
}
