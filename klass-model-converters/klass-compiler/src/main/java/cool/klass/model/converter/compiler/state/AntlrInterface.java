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

package cool.klass.model.converter.compiler.state;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEndSignature;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import cool.klass.model.converter.compiler.state.property.AntlrProperty;
import cool.klass.model.converter.compiler.state.property.AntlrReferenceProperty;
import cool.klass.model.meta.domain.InterfaceImpl.InterfaceBuilder;
import cool.klass.model.meta.domain.property.AbstractDataTypeProperty.DataTypePropertyBuilder;
import cool.klass.model.meta.domain.property.AbstractProperty.PropertyBuilder;
import cool.klass.model.meta.domain.property.AssociationEndSignatureImpl.AssociationEndSignatureBuilder;
import cool.klass.model.meta.domain.property.ModifierImpl.ModifierBuilder;
import cool.klass.model.meta.domain.property.ReferencePropertyImpl.ReferencePropertyBuilder;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceBlockContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceReferenceContext;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;

public class AntlrInterface extends AntlrClassifier {

    // <editor-fold desc="AMBIGUOUS">
    public static final AntlrInterface AMBIGUOUS = new AntlrInterface(
        new InterfaceDeclarationContext(AMBIGUOUS_PARENT, -1),
        AntlrCompilationUnit.AMBIGUOUS,
        -1,
        AMBIGUOUS_IDENTIFIER_CONTEXT
    ) {
        @Override
        public void enterDataTypeProperty(@Nonnull AntlrDataTypeProperty<?> antlrDataTypeProperty) {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".enterDataTypeProperty() not implemented yet"
            );
        }
    };
    // </editor-fold>

    // <editor-fold desc="NOT_FOUND">
    public static final AntlrInterface NOT_FOUND = new AntlrInterface(
        new InterfaceDeclarationContext(NOT_FOUND_PARENT, -1),
        AntlrCompilationUnit.NOT_FOUND,
        -1,
        NOT_FOUND_IDENTIFIER_CONTEXT
    ) {
        @Override
        public void enterDataTypeProperty(@Nonnull AntlrDataTypeProperty<?> antlrDataTypeProperty) {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".enterDataTypeProperty() not implemented yet"
            );
        }
    };
    // </editor-fold>

    // TODO: Unified list of all members

    /*
    private final MutableList<AntlrAssociationEnd>               associationEnds  = Lists.mutable.empty();
    private final MutableOrderedMap<String, AntlrAssociationEnd> associationEndsByName =
            OrderedMapAdapter.adapt(new LinkedHashMap<>());

    private final MutableList<AntlrParameterizedProperty>                                     parameterizedProperties          = Lists.mutable.empty();
    private final MutableOrderedMap<String, AntlrParameterizedProperty>                       parameterizedPropertiesByName    =
            OrderedMapAdapter.adapt(new LinkedHashMap<>());
    private final MutableOrderedMap<ParameterizedPropertyContext, AntlrParameterizedProperty> parameterizedPropertiesByContext =
            OrderedMapAdapter.adapt(new LinkedHashMap<>());
    */

    private InterfaceBuilder interfaceBuilder;

    public AntlrInterface(
        @Nonnull InterfaceDeclarationContext elementContext,
        @Nonnull AntlrCompilationUnit compilationUnitState,
        int ordinal,
        @Nonnull IdentifierContext nameContext
    ) {
        super(elementContext, compilationUnitState, ordinal, nameContext);
    }

    public InterfaceBuilder build1() {
        if (this.interfaceBuilder != null) {
            throw new IllegalStateException();
        }

        this.interfaceBuilder = new InterfaceBuilder(
            (InterfaceDeclarationContext) this.elementContext,
            this.getMacroElementBuilder(),
            this.getSourceCodeBuilder(),
            this.ordinal,
            this.getNameContext(),
            this.getPackageName()
        );

        ImmutableList<ModifierBuilder> declaredModifiers =
            this.declaredModifiers.collect(AntlrModifier::build).toImmutable();
        this.interfaceBuilder.setDeclaredModifiers(declaredModifiers);

        ImmutableList<DataTypePropertyBuilder<?, ?, ?>> declaredDataTypeProperties =
            this.declaredDataTypeProperties.<DataTypePropertyBuilder<?, ?, ?>>collect(
                    AntlrDataTypeProperty::build
                ).toImmutable();

        this.interfaceBuilder.setDeclaredDataTypeProperties(declaredDataTypeProperties);
        return this.interfaceBuilder;
    }

    @Nonnull
    @Override
    public InterfaceBuilder getElementBuilder() {
        return Objects.requireNonNull(this.interfaceBuilder);
    }

    @Override
    public AntlrReferenceProperty<?> getReferencePropertyByName(@Nonnull String name) {
        AntlrReferenceProperty<?> declaredProperty = this.declaredReferencePropertiesByName.get(name);
        if (declaredProperty != null) {
            return declaredProperty;
        }

        return this.declaredInterfaces.asLazy()
            .collectWith(AntlrInterface::getReferencePropertyByName, name)
            .detectIfNone(Objects::nonNull, () -> AntlrReferenceProperty.NOT_FOUND);
    }

    public void build2() {
        if (this.interfaceBuilder == null) {
            throw new IllegalStateException();
        }

        this.declaredDataTypeProperties.each(AntlrDataTypeProperty::build2);

        ImmutableList<AssociationEndSignatureBuilder> declaredAssociationEndSignatures =
            this.declaredAssociationEndSignatures.collect(AntlrAssociationEndSignature::build).toImmutable();
        this.interfaceBuilder.setDeclaredAssociationEndSignatures(declaredAssociationEndSignatures);

        ImmutableList<ReferencePropertyBuilder<?, ?, ?>> declaredReferenceProperties =
            this.declaredReferenceProperties.<ReferencePropertyBuilder<?, ?, ?>>collect(
                    AntlrReferenceProperty::getElementBuilder
                ).toImmutable();
        this.interfaceBuilder.setDeclaredReferenceProperties(declaredReferenceProperties);

        ImmutableList<PropertyBuilder<?, ?, ?>> declaredProperties =
            this.declaredProperties.<PropertyBuilder<?, ?, ?>>collect(AntlrProperty::getElementBuilder).toImmutable();
        this.interfaceBuilder.setDeclaredProperties(declaredProperties);

        ImmutableList<InterfaceBuilder> declaredInterfaces =
            this.declaredInterfaces.collect(AntlrInterface::getElementBuilder).toImmutable();
        this.interfaceBuilder.setDeclaredInterfaces(declaredInterfaces);
    }

    // <editor-fold desc="Report Compiler Errors">
    @Override
    public void reportNameErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        super.reportNameErrors(compilerAnnotationHolder);
        this.reportKeywordCollision(compilerAnnotationHolder);

        if (RELADOMO_TYPES.contains(this.getName())) {
            String message = String.format("'%s' is a Reladomo type.", this.getName());
            compilerAnnotationHolder.add("ERR_REL_NME", message, this);
        }

        this.declaredDataTypeProperties.forEachWith(AntlrNamedElement::reportNameErrors, compilerAnnotationHolder);
    }

    @Override
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        super.reportErrors(compilerAnnotationHolder);

        this.reportTransientModifier(compilerAnnotationHolder);
        // this.reportDuplicateParameterizedPropertyNames(compilerAnnotationHolder);
        // this.reportDuplicateAssociationEndNames(compilerAnnotationHolder);
    }

    /*
    private void reportDuplicateParameterizedPropertyNames(@Nonnull CompilerErrorState compilerAnnotationHolder)
    {
        ImmutableBag<String> duplicateMemberNames = this.getDuplicateMemberNames();

        for (AntlrParameterizedProperty parameterizedProperty : this.parameterizedProperties)
        {
            if (duplicateMemberNames.contains(parameterizedProperty.getName()))
            {
                parameterizedProperty.reportDuplicateMemberName(compilerAnnotationHolder);
            }
            parameterizedProperty.reportErrors(compilerAnnotationHolder);
        }
    }

    private void reportDuplicateAssociationEndNames(@Nonnull CompilerErrorState compilerAnnotationHolder)
    {
        ImmutableBag<String> duplicateMemberNames = this.getDuplicateMemberNames();

        for (AntlrAssociationEnd associationEnd : this.associationEnds)
        {
            if (duplicateMemberNames.contains(associationEnd.getName()))
            {
                associationEnd.reportDuplicateMemberName(compilerAnnotationHolder);
            }
            associationEnd.reportErrors(compilerAnnotationHolder);
        }
    }
    */

    private void reportTransientModifier(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        // Only need to check declared modifiers
        Optional<AntlrModifier> maybeTransientModifier =
            this.declaredModifiers.detectOptional(AntlrModifier::isTransient);

        if (maybeTransientModifier.isEmpty()) {
            return;
        }

        AntlrModifier transientModifier = maybeTransientModifier.get();
        String message = String.format("'%s' keyword not applicable to interfaces.", transientModifier.getKeyword());
        compilerAnnotationHolder.add("ERR_INT_TRN", message, transientModifier);
    }

    @Override
    protected void reportCircularInheritance(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        boolean noCircularInheritance = true;
        for (int i = 0; i < this.declaredInterfaces.size(); i++) {
            AntlrInterface iface = this.declaredInterfaces.get(i);
            if (!iface.extendsInterface(this, Sets.mutable.empty())) {
                continue;
            }
            InterfaceReferenceContext offendingToken = this.getOffendingInterfaceReference(i);
            String message = String.format("Circular inheritance '%s'.", offendingToken.getText());
            compilerAnnotationHolder.add("ERR_IMP_SLF", message, this, offendingToken);
            noCircularInheritance = false;
        }

        if (noCircularInheritance) {
            this.reportForwardReference(compilerAnnotationHolder);
        }
    }

    // </editor-fold>

    private boolean extendsInterface(AntlrInterface iface, @Nonnull MutableSet<AntlrInterface> visitedInterfaces) {
        if (this.declaredInterfaces.contains(iface)) {
            return true;
        }

        if (visitedInterfaces.contains(this)) {
            return false;
        }

        visitedInterfaces.add(this);
        return this.declaredInterfaces.anySatisfy(
                eachSuperInterface -> eachSuperInterface.extendsInterface(iface, visitedInterfaces)
            );
    }

    @Override
    protected InterfaceReferenceContext getOffendingInterfaceReference(int index) {
        return this.getElementContext().interfaceHeader().implementsDeclaration().interfaceReference().get(index);
    }

    @Override
    protected boolean isInterfaceRedundant(int index, @Nonnull AntlrInterface iface) {
        return this.interfaceNotAtIndexImplements(index, iface);
    }

    @Override
    public ImmutableBag<String> getDuplicateMemberNames() {
        return this.getDeclaredMemberNames().toBag().selectByOccurrences(occurrences -> occurrences > 1).toImmutable();
    }

    private ImmutableList<String> getDeclaredMemberNames() {
        MutableList<String> topLevelNames = Lists.mutable.empty();
        this.declaredDataTypeProperties.collect(AntlrProperty::getName, topLevelNames);
        this.declaredAssociationEndSignatures.collect(AntlrProperty::getName, topLevelNames);
        return topLevelNames.toImmutable();
    }

    @Nonnull
    @Override
    public InterfaceDeclarationContext getElementContext() {
        return (InterfaceDeclarationContext) super.getElementContext();
    }

    @Override
    public InterfaceBlockContext getBlockContext() {
        return this.getElementContext().interfaceBlock();
    }

    @Nonnull
    @Override
    public InterfaceBuilder getTypeGetter() {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".getTypeBuilder() not implemented yet"
        );
    }

    @Override
    public AntlrDataTypeProperty<?> getDataTypePropertyByName(String name) {
        return this.declaredDataTypePropertiesByName.containsKey(name)
            ? this.declaredDataTypePropertiesByName.get(name)
            : this.getInterfaceDataTypePropertyByName(name);
    }

    public AntlrModifier getModifierByName(String name) {
        return this.declaredModifiersByName.containsKey(name)
            ? this.declaredModifiersByName.get(name)
            : this.getInterfaceClassifierModifierByName(name);
    }
}
