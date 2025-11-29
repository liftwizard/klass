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

package cool.klass.model.converter.compiler.state;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.annotation.AnnotationSeverity;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEnd;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEndSignature;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrEnumerationProperty;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import cool.klass.model.converter.compiler.state.property.AntlrParameterizedProperty;
import cool.klass.model.converter.compiler.state.property.AntlrProperty;
import cool.klass.model.converter.compiler.state.property.AntlrReferenceProperty;
import cool.klass.model.meta.domain.InterfaceImpl.InterfaceBuilder;
import cool.klass.model.meta.domain.KlassImpl.KlassBuilder;
import cool.klass.model.meta.domain.property.AbstractDataTypeProperty.DataTypePropertyBuilder;
import cool.klass.model.meta.domain.property.AbstractProperty.PropertyBuilder;
import cool.klass.model.meta.domain.property.AssociationEndImpl.AssociationEndBuilder;
import cool.klass.model.meta.domain.property.AssociationEndSignatureImpl.AssociationEndSignatureBuilder;
import cool.klass.model.meta.domain.property.ModifierImpl.ModifierBuilder;
import cool.klass.model.meta.domain.property.ReferencePropertyImpl.ReferencePropertyBuilder;
import cool.klass.model.meta.grammar.KlassParser.ClassBlockContext;
import cool.klass.model.meta.grammar.KlassParser.ClassDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ClassReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterizedPropertyContext;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;

public class AntlrClass extends AntlrClassifier {

    // <editor-fold desc="AMBIGUOUS">
    public static final AntlrClass AMBIGUOUS = new AntlrClass(
        new ClassDeclarationContext(AMBIGUOUS_PARENT, -1),
        AntlrCompilationUnit.AMBIGUOUS,
        -1,
        AMBIGUOUS_IDENTIFIER_CONTEXT,
        false
    ) {
        @Override
        public String toString() {
            return AntlrClass.class.getSimpleName() + ".AMBIGUOUS";
        }

        @Override
        public void enterDataTypeProperty(@Nonnull AntlrDataTypeProperty<?> antlrDataTypeProperty) {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".enterDataTypeProperty() not implemented yet"
            );
        }

        @Override
        public void enterAssociationEnd(@Nonnull AntlrAssociationEnd antlrAssociationEnd) {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".enterAssociationEnd() not implemented yet"
            );
        }

        @Override
        public void enterParameterizedProperty(@Nonnull AntlrParameterizedProperty parameterizedProperty) {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".enterParameterizedProperty() not implemented yet"
            );
        }
    };
    // </editor-fold>

    // <editor-fold desc="NOT_FOUND">
    public static final AntlrClass NOT_FOUND = new AntlrClass(
        new ClassDeclarationContext(NOT_FOUND_PARENT, -1),
        AntlrCompilationUnit.NOT_FOUND,
        -1,
        NOT_FOUND_IDENTIFIER_CONTEXT,
        false
    ) {
        @Override
        public String toString() {
            return AntlrClass.class.getSimpleName() + ".NOT_FOUND";
        }

        @Override
        public void enterDataTypeProperty(@Nonnull AntlrDataTypeProperty<?> antlrDataTypeProperty) {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".enterDataTypeProperty() not implemented yet"
            );
        }

        @Override
        public void enterAssociationEnd(@Nonnull AntlrAssociationEnd antlrAssociationEnd) {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".enterAssociationEnd() not implemented yet"
            );
        }

        @Override
        public void enterParameterizedProperty(@Nonnull AntlrParameterizedProperty parameterizedProperty) {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".enterParameterizedProperty() not implemented yet"
            );
        }
    };
    // </editor-fold>

    // TODO: Unified list of dataType and parameterized properties

    private final MutableList<AntlrAssociationEnd> declaredAssociationEnds = Lists.mutable.empty();
    private final MutableOrderedMap<String, AntlrAssociationEnd> declaredAssociationEndsByName =
        OrderedMapAdapter.adapt(new LinkedHashMap<>());

    private final MutableList<AntlrParameterizedProperty> declaredParameterizedProperties = Lists.mutable.empty();

    private final MutableOrderedMap<String, AntlrParameterizedProperty> declaredParameterizedPropertiesByName =
        OrderedMapAdapter.adapt(new LinkedHashMap<>());

    private final MutableOrderedMap<
        ParameterizedPropertyContext,
        AntlrParameterizedProperty
    > declaredParameterizedPropertiesByContext = OrderedMapAdapter.adapt(new LinkedHashMap<>());

    private final boolean isUser;

    private boolean isAbstract;

    private KlassBuilder klassBuilder;

    @Nonnull
    private Optional<AntlrClass> superClass = Optional.empty();

    @Nonnull
    private final MutableList<AntlrClass> subClasses = Lists.mutable.empty();

    public AntlrClass(
        @Nonnull ClassDeclarationContext elementContext,
        @Nonnull AntlrCompilationUnit compilationUnitState,
        int ordinal,
        @Nonnull IdentifierContext nameContext,
        boolean isUser
    ) {
        super(elementContext, compilationUnitState, ordinal, nameContext);
        this.isUser = isUser;
    }

    public boolean isUser() {
        return this.isUser;
    }

    public ListIterable<AntlrAssociationEnd> getDeclaredAssociationEnds() {
        return this.declaredAssociationEnds.asUnmodifiable();
    }

    @Override
    protected ImmutableList<AntlrProperty> getInheritedProperties(@Nonnull MutableList<AntlrClassifier> visited) {
        ImmutableList<AntlrProperty> superClassProperties = this.superClass.map((antlrClass) ->
            antlrClass.getAllProperties(visited)
        ).orElseGet(Lists.immutable::empty);

        ImmutableList<AntlrProperty> interfaceProperties = this.declaredInterfaces.flatCollectWith(
            AntlrClassifier::getAllProperties,
            visited
        ).toImmutable();

        return superClassProperties.newWithAll(interfaceProperties).distinctBy(AntlrNamedElement::getName);
    }

    @Override
    protected ImmutableList<AntlrDataTypeProperty<?>> getInheritedDataTypeProperties(
        @Nonnull MutableList<AntlrClassifier> visited
    ) {
        ImmutableList<AntlrDataTypeProperty<?>> superClassProperties = this.superClass.map((antlrClass) ->
            antlrClass.getAllDataTypeProperties(visited)
        ).orElseGet(Lists.immutable::empty);

        ImmutableList<AntlrDataTypeProperty<?>> interfaceProperties = this.declaredInterfaces.flatCollectWith(
            AntlrClassifier::getAllDataTypeProperties,
            visited
        ).toImmutable();

        return superClassProperties.newWithAll(interfaceProperties).distinctBy(AntlrNamedElement::getName);
    }

    @Override
    protected ImmutableList<AntlrModifier> getInheritedModifiers(@Nonnull MutableList<AntlrClassifier> visited) {
        ImmutableList<AntlrModifier> superClassModifiers = this.superClass.map((antlrClass) ->
            antlrClass.getAllModifiers(visited)
        ).orElseGet(Lists.immutable::empty);

        ImmutableList<AntlrModifier> interfaceModifiers = this.declaredInterfaces.flatCollectWith(
            AntlrClassifier::getAllModifiers,
            visited
        ).toImmutable();

        return superClassModifiers.newWithAll(interfaceModifiers).distinctBy(AntlrModifier::getKeyword);
    }

    @Override
    public AntlrReferenceProperty<?> getReferencePropertyByName(@Nonnull String name) {
        AntlrReferenceProperty<?> declaredProperty = this.declaredReferencePropertiesByName.get(name);
        if (declaredProperty != null) {
            return declaredProperty;
        }

        Optional<AntlrReferenceProperty<?>> superClassProperty = this.superClass.map((superClass) ->
            superClass.getReferencePropertyByName(name)
        );
        if (superClassProperty.isPresent()) {
            return superClassProperty.get();
        }

        return this.declaredInterfaces.asLazy()
            .collectWith(AntlrInterface::getReferencePropertyByName, name)
            .detectIfNone(Objects::nonNull, () -> AntlrReferenceProperty.NOT_FOUND);
    }

    public void enterAssociationEnd(@Nonnull AntlrAssociationEnd antlrAssociationEnd) {
        this.declaredProperties.add(antlrAssociationEnd);
        this.declaredAssociationEnds.add(antlrAssociationEnd);
        this.declaredAssociationEndsByName.compute(antlrAssociationEnd.getName(), (name, builder) ->
            builder == null ? antlrAssociationEnd : AntlrAssociationEnd.AMBIGUOUS
        );

        this.declaredReferenceProperties.add(antlrAssociationEnd);
        this.declaredReferencePropertiesByName.compute(antlrAssociationEnd.getName(), (name, builder) ->
            builder == null ? antlrAssociationEnd : AntlrAssociationEnd.AMBIGUOUS
        );
        AntlrReferenceProperty<?> duplicate2 = this.declaredReferencePropertiesByContext.put(
            antlrAssociationEnd.getElementContext(),
            antlrAssociationEnd
        );
        if (duplicate2 != null) {
            throw new AssertionError();
        }
    }

    public void enterParameterizedProperty(@Nonnull AntlrParameterizedProperty parameterizedProperty) {
        // this.properties.add(parameterizedProperty);
        this.declaredParameterizedProperties.add(parameterizedProperty);
        this.declaredParameterizedPropertiesByName.compute(parameterizedProperty.getName(), (name, builder) ->
            builder == null ? parameterizedProperty : AntlrParameterizedProperty.AMBIGUOUS
        );

        AntlrParameterizedProperty duplicate1 = this.declaredParameterizedPropertiesByContext.put(
            parameterizedProperty.getElementContext(),
            parameterizedProperty
        );
        if (duplicate1 != null) {
            throw new AssertionError();
        }
        /*
        this.referenceProperties.add(parameterizedProperty);
        this.referencePropertiesByName.compute(
                parameterizedProperty.getName(),
                (name, builder) -> builder == null
                        ? parameterizedProperty
                        : AntlrParameterizedProperty.AMBIGUOUS);
        AntlrReferenceProperty<?> duplicate2 = this.referencePropertiesByContext.put(
                parameterizedProperty.getElementContext(),
                parameterizedProperty);
        if (duplicate2 != null)
        {
            throw new AssertionError();
        }
        */
    }

    public AntlrParameterizedProperty getParameterizedPropertyByContext(ParameterizedPropertyContext ctx) {
        return this.declaredParameterizedPropertiesByContext.get(ctx);
    }

    public boolean isAbstract() {
        return this.isAbstract;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public void enterExtendsDeclaration(@Nonnull AntlrClass superClass) {
        if (this.superClass.isPresent()) {
            throw new AssertionError();
        }
        this.superClass = Optional.of(superClass);
        superClass.subClasses.add(this);
    }

    @Nonnull
    @Override
    public KlassBuilder getElementBuilder() {
        return Objects.requireNonNull(this.klassBuilder);
    }

    @Override
    public boolean implementsInterface(AntlrInterface iface) {
        if (super.implementsInterface(iface)) {
            return true;
        }

        return this.superClass.map((klass) -> klass.implementsInterface(iface)).orElse(false);
    }

    public KlassBuilder build1() {
        if (this.klassBuilder != null) {
            throw new IllegalStateException();
        }

        this.klassBuilder = new KlassBuilder(
            (ClassDeclarationContext) this.elementContext,
            this.getMacroElementBuilder(),
            this.getSourceCodeBuilder(),
            this.ordinal,
            this.getNameContext(),
            this.getPackageName(),
            this.isAbstract,
            this.isUser,
            this.isTransient()
        );

        ImmutableList<ModifierBuilder> declaredModifiers = this.declaredModifiers.collect(
            AntlrModifier::build
        ).toImmutable();
        this.klassBuilder.setDeclaredModifiers(declaredModifiers);

        ImmutableList<DataTypePropertyBuilder<?, ?, ?>> declaredDataTypeProperties = this.declaredDataTypeProperties.<
            DataTypePropertyBuilder<?, ?, ?>
        >collect(AntlrDataTypeProperty::build).toImmutable();

        this.klassBuilder.setDeclaredDataTypeProperties(declaredDataTypeProperties);
        return this.klassBuilder;
    }

    public void build2() {
        if (this.klassBuilder == null) {
            throw new IllegalStateException();
        }

        ImmutableList<AssociationEndBuilder> declaredAssociationEnds = this.declaredAssociationEnds.collect(
            AntlrAssociationEnd::getElementBuilder
        ).toImmutable();
        this.klassBuilder.setDeclaredAssociationEnds(declaredAssociationEnds);

        ImmutableList<AssociationEndSignatureBuilder> declaredAssociationEndSignatures =
            this.declaredAssociationEndSignatures.collect(AntlrAssociationEndSignature::build).toImmutable();
        this.klassBuilder.setDeclaredAssociationEndSignatures(declaredAssociationEndSignatures);

        ImmutableList<ReferencePropertyBuilder<?, ?, ?>> declaredReferenceProperties =
            this.declaredReferenceProperties.<ReferencePropertyBuilder<?, ?, ?>>collect(
                AntlrReferenceProperty::getElementBuilder
            ).toImmutable();
        this.klassBuilder.setDeclaredReferenceProperties(declaredReferenceProperties);

        ImmutableList<PropertyBuilder<?, ?, ?>> declaredProperties = this.declaredProperties.<
            PropertyBuilder<?, ?, ?>
        >collect(AntlrProperty::getElementBuilder).toImmutable();
        this.klassBuilder.setDeclaredProperties(declaredProperties);

        this.declaredDataTypeProperties.each(AntlrDataTypeProperty::build2);

        ImmutableList<InterfaceBuilder> declaredInterfaces = this.declaredInterfaces.collect(
            AntlrInterface::getElementBuilder
        ).toImmutable();
        this.klassBuilder.setDeclaredInterfaces(declaredInterfaces);

        Optional<KlassBuilder> superClass = this.superClass.map(AntlrClass::getElementBuilder);
        this.klassBuilder.setSuperClass(superClass);

        ImmutableList<KlassBuilder> subClasses = this.subClasses.collect(AntlrClass::getElementBuilder).toImmutable();
        this.klassBuilder.setSubClassBuilders(subClasses);
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
        this.declaredParameterizedProperties.forEachWith(AntlrNamedElement::reportNameErrors, compilerAnnotationHolder);
        this.declaredAssociationEnds.forEachWith(AntlrNamedElement::reportNameErrors, compilerAnnotationHolder);
        this.declaredAssociationEndSignatures.forEachWith(
            AntlrNamedElement::reportNameErrors,
            compilerAnnotationHolder
        );
    }

    @Override
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        super.reportErrors(compilerAnnotationHolder);

        if (this.isUser) {
            this.reportMultiplePropertiesWithModifiers(
                compilerAnnotationHolder,
                this.declaredDataTypeProperties,
                "key",
                "userId"
            );
        }
        this.reportMissingKeyProperty(compilerAnnotationHolder);
        this.reportSuperClassNotFound(compilerAnnotationHolder);
        this.reportExtendsConcrete(compilerAnnotationHolder);
        this.reportMultipleOppositesWithModifier(compilerAnnotationHolder, "version", AnnotationSeverity.ERROR);
        this.reportMultipleOppositesWithModifier(compilerAnnotationHolder, "owned", AnnotationSeverity.WARNING);
        this.reportVersionErrors(compilerAnnotationHolder);
        this.reportTransientInheritance(compilerAnnotationHolder);
        this.reportTransientIdProperties(compilerAnnotationHolder);
        // TODO: parameterized properties
    }

    private void reportMultipleOppositesWithModifier(
        @Nonnull CompilerAnnotationHolder compilerAnnotationHolder,
        @Nonnull String modifier,
        @Nonnull AnnotationSeverity severity
    ) {
        MutableList<AntlrAssociationEnd> associationEnds = this.declaredAssociationEnds.select((associationEnd) ->
            associationEnd.getOpposite().getModifiers().anySatisfyWith(AntlrModifier::is, modifier)
        );
        if (associationEnds.size() <= 1) {
            return;
        }

        for (AntlrAssociationEnd associationEnd : associationEnds) {
            associationEnd
                .getOpposite()
                .reportDuplicateOppositeWithModifier(compilerAnnotationHolder, this, modifier, severity);
        }
    }

    private void reportVersionErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (
            this.declaredReferenceProperties.anySatisfy(AntlrReferenceProperty::isVersion)
            && this.declaredAssociationEnds.anySatisfy(AntlrAssociationEnd::isVersioned)
        ) {
            String message = String.format("Class '%s' is a version and has a version.", this.getName());
            compilerAnnotationHolder.add("ERR_VER_VER", message, this);
        }
    }

    private void reportMissingKeyProperty(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (!this.hasKeyProperty() && !this.hasIDProperty()) {
            String message = String.format("Class '%s' must have at least one key property.", this.getName());
            compilerAnnotationHolder.add("ERR_CLS_KEY", message, this);
        }
    }

    public void reportDuplicateUserClass(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        String message = String.format("Only one 'user' class is allowed. Found '%s'.", this.getName());
        compilerAnnotationHolder.add("ERR_DUP_USR", message, this, this.nameContext);
    }

    private void reportSuperClassNotFound(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (this.superClass.equals(Optional.of(AntlrClass.NOT_FOUND))) {
            ClassReferenceContext offendingToken = this.getElementContext()
                .classHeader()
                .extendsDeclaration()
                .classReference();
            String message = String.format("Cannot find class '%s'.", offendingToken.getText());
            compilerAnnotationHolder.add("ERR_EXT_CLS", message, this, offendingToken);
        }
    }

    private void reportExtendsConcrete(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (
            this.superClass.isEmpty()
            || this.superClass.equals(Optional.of(NOT_FOUND))
            || this.superClass.equals(Optional.of(AMBIGUOUS))
        ) {
            return;
        }

        if (!this.superClass.get().isAbstract) {
            ClassReferenceContext offendingToken = this.getElementContext()
                .classHeader()
                .extendsDeclaration()
                .classReference();
            String message = String.format("Superclass must be abstract '%s'.", offendingToken.getText());
            compilerAnnotationHolder.add("ERR_EXT_CCT", message, this, offendingToken);
        }
    }

    private void reportTransientInheritance(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (this.isTransient() || this.superClass.isEmpty() || !this.superClass.get().isTransient()) {
            return;
        }

        ClassReferenceContext offendingToken = this.getElementContext()
            .classHeader()
            .extendsDeclaration()
            .classReference();
        String message = String.format(
            "Must be transient to inherit from transient superclass '%s'.",
            offendingToken.getText()
        );
        compilerAnnotationHolder.add("ERR_EXT_TNS", message, this, offendingToken);
    }

    private void reportTransientIdProperties(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (!this.isTransient()) {
            return;
        }

        this.declaredDataTypeProperties.select(AntlrDataTypeProperty::isId).forEachWith(
            AntlrDataTypeProperty::reportTransientIdProperties,
            compilerAnnotationHolder
        );
    }

    @Override
    protected void reportCircularInheritance(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (this.hasCircularInheritance()) {
            ClassReferenceContext offendingToken = this.getElementContext()
                .classHeader()
                .extendsDeclaration()
                .classReference();
            String message = String.format("Circular inheritance '%s'.", offendingToken.getText());
            compilerAnnotationHolder.add("ERR_EXT_SLF", message, this, offendingToken);
        } else {
            this.reportForwardReference(compilerAnnotationHolder);
        }
    }

    @Override
    protected void reportForwardReference(CompilerAnnotationHolder compilerAnnotationHolder) {
        super.reportForwardReference(compilerAnnotationHolder);
        if (this.superClass.isEmpty()) {
            return;
        }

        AntlrClass klass = this.superClass.get();
        if (this.isForwardReference(klass)) {
            String message = String.format(
                "Class '%s' is declared on line %d and has a forward reference to super class '%s' which is declared later in the source file '%s' on line %d.",
                this.getName(),
                this.getElementContext().getStart().getLine(),
                klass.getName(),
                this.getCompilationUnit().get().getSourceName(),
                klass.getElementContext().getStart().getLine()
            );
            compilerAnnotationHolder.add(
                "ERR_FWD_REF",
                message,
                this,
                this.getElementContext().classHeader().extendsDeclaration().classReference()
            );
        }
    }

    // </editor-fold>

    private boolean hasIDProperty() {
        return this.getAllDataTypeProperties().anySatisfy(AntlrDataTypeProperty::isId);
    }

    private boolean hasKeyProperty() {
        return this.getAllDataTypeProperties().anySatisfy(AntlrDataTypeProperty::isKey);
    }

    private boolean hasCircularInheritance() {
        return this.superClass.isPresent() && this.superClass.get().extendsClass(this, Sets.mutable.empty());
    }

    private boolean extendsClass(@Nonnull AntlrClass antlrClass, @Nonnull MutableSet<AntlrClass> visitedClasses) {
        if (this.superClass.isEmpty()) {
            return false;
        }

        if (this.superClass.equals(Optional.of(antlrClass))) {
            return true;
        }

        if (visitedClasses.contains(this)) {
            return false;
        }

        visitedClasses.add(this);
        return this.superClass.get().extendsClass(antlrClass, visitedClasses);
    }

    @Override
    protected boolean isInterfaceRedundant(int index, @Nonnull AntlrInterface iface) {
        return (
            (this.superClass.isPresent() && this.superClass.get().implementsInterface(iface))
            || this.interfaceNotAtIndexImplements(index, iface)
        );
    }

    @Override
    protected InterfaceReferenceContext getOffendingInterfaceReference(int index) {
        return this.getElementContext().classHeader().implementsDeclaration().interfaceReference().get(index);
    }

    @Override
    public ImmutableBag<String> getDuplicateMemberNames() {
        return this.getDeclaredMemberNames()
            .toBag()
            .selectByOccurrences((occurrences) -> occurrences > 1)
            .toImmutable();
    }

    private ImmutableList<String> getDeclaredMemberNames() {
        MutableList<String> topLevelNames = Lists.mutable.empty();
        this.getAllDataTypeProperties().collect(AntlrProperty::getName, topLevelNames);
        this.declaredParameterizedProperties.collect(AntlrNamedElement::getName, topLevelNames);
        this.declaredAssociationEnds.collect(AntlrProperty::getName, topLevelNames);
        this.declaredAssociationEndSignatures.collect(AntlrProperty::getName, topLevelNames);
        return topLevelNames.toImmutable();
    }

    public boolean isSubClassOf(AntlrClass klass) {
        if (this == klass) {
            return false;
        }

        if (this.superClass.isEmpty()) {
            return false;
        }

        AntlrClass superClass = this.superClass.get();
        if (superClass == klass) {
            return true;
        }

        return superClass.isSubClassOf(klass);
    }

    @Override
    public Optional<AntlrClass> getSuperClass() {
        return this.superClass;
    }

    @Nonnull
    @Override
    public ClassDeclarationContext getElementContext() {
        return (ClassDeclarationContext) super.getElementContext();
    }

    @Override
    public ClassBlockContext getBlockContext() {
        return this.getElementContext().classBlock();
    }

    @Override
    public AntlrDataTypeProperty<?> getDataTypePropertyByName(String name) {
        if (this.declaredDataTypePropertiesByName.containsKey(name)) {
            return this.declaredDataTypePropertiesByName.get(name);
        }

        if (this.superClass.isPresent()) {
            AntlrDataTypeProperty<?> superClassProperty = this.superClass.get().getDataTypePropertyByName(name);
            if (superClassProperty != AntlrEnumerationProperty.NOT_FOUND) {
                return superClassProperty;
            }
        }

        return this.getInterfaceDataTypePropertyByName(name);
    }

    public AntlrAssociationEnd getAssociationEndByName(String name) {
        if (this.declaredAssociationEndsByName.containsKey(name)) {
            return this.declaredAssociationEndsByName.get(name);
        }

        return this.superClass.map((superClass) -> superClass.getAssociationEndByName(name)).orElse(
            AntlrAssociationEnd.NOT_FOUND
        );
    }

    public AntlrModifier getModifierByName(String name) {
        if (this.declaredModifiersByName.containsKey(name)) {
            return this.declaredModifiersByName.get(name);
        }

        if (this.superClass.isPresent()) {
            AntlrModifier superClassProperty = this.superClass.get().getModifierByName(name);
            if (superClassProperty != AntlrModifier.NOT_FOUND) {
                return superClassProperty;
            }
        }

        return this.getInterfaceClassifierModifierByName(name);
    }

    @Nonnull
    @Override
    public KlassBuilder getTypeGetter() {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".getTypeBuilder() not implemented yet"
        );
    }

    public boolean isVersion() {
        return this.declaredDataTypeProperties.anySatisfy(AntlrDataTypeProperty::isVersion);
    }
}
