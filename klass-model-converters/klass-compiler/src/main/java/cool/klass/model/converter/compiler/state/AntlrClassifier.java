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
import javax.annotation.OverridingMethodsMustInvokeSuper;

import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEndSignature;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrEnumerationProperty;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import cool.klass.model.converter.compiler.state.property.AntlrProperty;
import cool.klass.model.converter.compiler.state.property.AntlrReferenceProperty;
import cool.klass.model.meta.domain.AbstractClassifier.ClassifierBuilder;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndSignatureContext;
import cool.klass.model.meta.grammar.KlassParser.ClassDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceReferenceContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.list.Interval;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;

public abstract class AntlrClassifier extends AntlrPackageableElement implements AntlrType, AntlrTopLevelElement {

    // <editor-fold desc="AMBIGUOUS">
    public static final AntlrClassifier AMBIGUOUS = new AntlrClassifier(
        new ClassDeclarationContext(AMBIGUOUS_PARENT, -1),
        AntlrCompilationUnit.AMBIGUOUS,
        -1,
        AMBIGUOUS_IDENTIFIER_CONTEXT
    ) {
        @Override
        public AntlrReferenceProperty<?> getReferencePropertyByName(@Nonnull String name) {
            return AntlrReferenceProperty.AMBIGUOUS;
        }

        @Override
        public AntlrDataTypeProperty<?> getDataTypePropertyByName(String name) {
            return AntlrDataTypeProperty.AMBIGUOUS;
        }

        @Override
        public String toString() {
            return AntlrClassifier.class.getSimpleName() + ".AMBIGUOUS";
        }
    };
    // </editor-fold>

    // <editor-fold desc="NOT_FOUND">
    public static final AntlrClassifier NOT_FOUND = new AntlrClassifier(
        new ClassDeclarationContext(NOT_FOUND_PARENT, -1),
        AntlrCompilationUnit.AMBIGUOUS,
        -1,
        NOT_FOUND_IDENTIFIER_CONTEXT
    ) {
        @Override
        public AntlrReferenceProperty<?> getReferencePropertyByName(@Nonnull String name) {
            return AntlrReferenceProperty.NOT_FOUND;
        }

        @Override
        public AntlrDataTypeProperty<?> getDataTypePropertyByName(String name) {
            return AntlrDataTypeProperty.NOT_FOUND;
        }

        @Override
        public String toString() {
            return AntlrClassifier.class.getSimpleName() + ".NOT_FOUND";
        }
    };
    // </editor-fold>

    protected final MutableList<AntlrModifier> declaredModifiers = Lists.mutable.empty();
    protected final MutableList<AntlrProperty> declaredProperties = Lists.mutable.empty();
    // Members are properties where the ordinal comes from declaration order. Association ends are properties, but are not members.
    protected final MutableList<AntlrProperty> declaredMembers = Lists.mutable.empty();
    protected final MutableList<AntlrDataTypeProperty<?>> declaredDataTypeProperties = Lists.mutable.empty();
    protected final MutableList<AntlrReferenceProperty<?>> declaredReferenceProperties = Lists.mutable.empty();
    protected final MutableList<AntlrAssociationEndSignature> declaredAssociationEndSignatures = Lists.mutable.empty();
    protected final MutableList<AntlrInterface> declaredInterfaces = Lists.mutable.empty();

    protected final MutableOrderedMap<String, AntlrModifier> declaredModifiersByName = OrderedMapAdapter.adapt(
        new LinkedHashMap<>()
    );
    protected final MutableOrderedMap<ParserRuleContext, AntlrModifier> declaredModifiersByContext =
        OrderedMapAdapter.adapt(new LinkedHashMap<>());
    protected final MutableOrderedMap<String, AntlrDataTypeProperty<?>> declaredDataTypePropertiesByName =
        OrderedMapAdapter.adapt(new LinkedHashMap<>());
    protected final MutableOrderedMap<String, AntlrReferenceProperty<?>> declaredReferencePropertiesByName =
        OrderedMapAdapter.adapt(new LinkedHashMap<>());
    protected final MutableOrderedMap<
        ParserRuleContext,
        AntlrReferenceProperty<?>
    > declaredReferencePropertiesByContext = OrderedMapAdapter.adapt(new LinkedHashMap<>());
    protected final MutableOrderedMap<String, AntlrAssociationEndSignature> declaredAssociationEndSignaturesByName =
        OrderedMapAdapter.adapt(new LinkedHashMap<>());

    protected final MutableOrderedMap<
        AssociationEndSignatureContext,
        AntlrAssociationEndSignature
    > declaredAssociationEndSignaturesByContext = OrderedMapAdapter.adapt(new LinkedHashMap<>());

    protected AntlrClassifier(
        @Nonnull ParserRuleContext elementContext,
        @Nonnull AntlrCompilationUnit compilationUnitState,
        int ordinal,
        @Nonnull IdentifierContext nameContext
    ) {
        super(elementContext, compilationUnitState, ordinal, nameContext);
    }

    public abstract AntlrReferenceProperty<?> getReferencePropertyByName(@Nonnull String name);

    public abstract AntlrDataTypeProperty<?> getDataTypePropertyByName(String name);

    @Nonnull
    @Override
    public ClassifierBuilder<?> getElementBuilder() {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".getElementBuilder() not implemented yet"
        );
    }

    @Nonnull
    @Override
    public ClassifierBuilder<?> getTypeGetter() {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".getTypeGetter() not implemented yet"
        );
    }

    public final ImmutableList<AntlrProperty> getAllProperties() {
        return this.getAllProperties(Lists.mutable.empty());
    }

    protected ImmutableList<AntlrProperty> getAllProperties(@Nonnull MutableList<AntlrClassifier> visited) {
        if (visited.contains(this)) {
            return Lists.immutable.empty();
        }
        visited.add(this);

        MutableSet<String> propertyNames = this.declaredProperties.collect(AntlrNamedElement::getName).toSet();

        ImmutableList<AntlrProperty> inheritedProperties = this.getInheritedProperties(visited).reject(
            (inheritedProperty) -> propertyNames.contains(inheritedProperty.getName())
        );

        return this.declaredProperties.toImmutable().newWithAll(inheritedProperties);
    }

    protected ImmutableList<AntlrProperty> getInheritedProperties(@Nonnull MutableList<AntlrClassifier> visited) {
        return this.declaredInterfaces.flatCollectWith(AntlrClassifier::getAllProperties, visited)
            .distinctBy(AntlrNamedElement::getName)
            .toImmutable();
    }

    public final ImmutableList<AntlrDataTypeProperty<?>> getAllDataTypeProperties() {
        return this.getAllDataTypeProperties(Lists.mutable.empty());
    }

    protected ImmutableList<AntlrDataTypeProperty<?>> getAllDataTypeProperties(
        @Nonnull MutableList<AntlrClassifier> visited
    ) {
        if (visited.contains(this)) {
            return Lists.immutable.empty();
        }
        visited.add(this);

        MutableSet<String> propertyNames = this.declaredDataTypeProperties.collect(AntlrNamedElement::getName).toSet();

        ImmutableList<AntlrDataTypeProperty<?>> inheritedProperties = this.getInheritedDataTypeProperties(
            visited
        ).reject((inheritedProperty) -> propertyNames.contains(inheritedProperty.getName()));

        return this.declaredDataTypeProperties.toImmutable().newWithAll(inheritedProperties);
    }

    protected ImmutableList<AntlrDataTypeProperty<?>> getInheritedDataTypeProperties(
        @Nonnull MutableList<AntlrClassifier> visited
    ) {
        return this.declaredInterfaces.flatCollectWith(AntlrClassifier::getAllDataTypeProperties, visited)
            .distinctBy(AntlrNamedElement::getName)
            .toImmutable();
    }

    private ImmutableList<AntlrModifier> getAllModifiers() {
        return this.getAllModifiers(Lists.mutable.empty());
    }

    protected ImmutableList<AntlrModifier> getAllModifiers(@Nonnull MutableList<AntlrClassifier> visited) {
        if (visited.contains(this)) {
            return Lists.immutable.empty();
        }
        visited.add(this);

        MutableSet<String> modifierNames = this.declaredModifiers.collect(AntlrModifier::getKeyword).toSet();

        ImmutableList<AntlrModifier> inheritedModifiers = this.getInheritedModifiers(visited).reject(
            (inheritedProperty) -> modifierNames.contains(inheritedProperty.getKeyword())
        );

        return this.declaredModifiers.toImmutable().newWithAll(inheritedModifiers);
    }

    protected ImmutableList<AntlrModifier> getInheritedModifiers(@Nonnull MutableList<AntlrClassifier> visited) {
        return this.declaredInterfaces.flatCollectWith(AntlrClassifier::getAllModifiers, visited)
            .distinctBy(AntlrModifier::getKeyword)
            .toImmutable();
    }

    public boolean isTransient() {
        return this.getAllModifiers().anySatisfy(AntlrModifier::isTransient);
    }

    public void enterDataTypeProperty(@Nonnull AntlrDataTypeProperty<?> antlrDataTypeProperty) {
        Objects.requireNonNull(antlrDataTypeProperty);
        this.declaredProperties.add(antlrDataTypeProperty);
        this.declaredMembers.add(antlrDataTypeProperty);
        this.declaredDataTypeProperties.add(antlrDataTypeProperty);
        this.declaredDataTypePropertiesByName.compute(antlrDataTypeProperty.getName(), (name, builder) ->
            builder == null ? antlrDataTypeProperty : AntlrDataTypeProperty.AMBIGUOUS
        );
    }

    public AntlrAssociationEndSignature getDeclaredAssociationEndSignatureByContext(
        @Nonnull AssociationEndSignatureContext ctx
    ) {
        Objects.requireNonNull(ctx);
        return this.declaredAssociationEndSignaturesByContext.get(ctx);
    }

    public AntlrReferenceProperty<?> getDeclaredReferencePropertyByContext(@Nonnull ParserRuleContext ctx) {
        Objects.requireNonNull(ctx);
        return this.declaredReferencePropertiesByContext.get(ctx);
    }

    public void enterAssociationEndSignature(@Nonnull AntlrAssociationEndSignature associationEndSignature) {
        Objects.requireNonNull(associationEndSignature);
        this.declaredProperties.add(associationEndSignature);
        this.declaredMembers.add(associationEndSignature);
        this.declaredAssociationEndSignatures.add(associationEndSignature);
        this.declaredAssociationEndSignaturesByName.compute(associationEndSignature.getName(), (name, builder) ->
            builder == null ? associationEndSignature : AntlrAssociationEndSignature.AMBIGUOUS
        );
        AntlrAssociationEndSignature duplicate1 = this.declaredAssociationEndSignaturesByContext.put(
            associationEndSignature.getElementContext(),
            associationEndSignature
        );
        if (duplicate1 != null) {
            throw new AssertionError();
        }

        this.declaredReferenceProperties.add(associationEndSignature);
        this.declaredReferencePropertiesByName.compute(associationEndSignature.getName(), (name, builder) ->
            builder == null ? associationEndSignature : AntlrAssociationEndSignature.AMBIGUOUS
        );
        AntlrReferenceProperty<?> duplicate2 = this.declaredReferencePropertiesByContext.put(
            associationEndSignature.getElementContext(),
            associationEndSignature
        );
        if (duplicate2 != null) {
            throw new AssertionError();
        }
    }

    public void enterModifier(@Nonnull AntlrModifier modifier) {
        Objects.requireNonNull(modifier);
        this.declaredModifiers.add(modifier);
        this.declaredModifiersByName.compute(modifier.getKeyword(), (name, builder) ->
            builder == null ? modifier : AntlrModifier.AMBIGUOUS
        );

        AntlrModifier duplicate = this.declaredModifiersByContext.put(modifier.getElementContext(), modifier);
        if (duplicate != null) {
            throw new AssertionError();
        }
    }

    public MutableList<AntlrModifier> getDeclaredModifiers() {
        return this.declaredModifiers.asUnmodifiable();
    }

    public AntlrModifier getDeclaredModifierByContext(@Nonnull ParserRuleContext modifierContext) {
        Objects.requireNonNull(modifierContext);
        return this.declaredModifiersByContext.get(modifierContext);
    }

    public int getNumClassifierModifiers() {
        return this.declaredModifiers.size();
    }

    public void enterImplementsDeclaration(@Nonnull AntlrInterface iface) {
        Objects.requireNonNull(iface);
        this.declaredInterfaces.add(iface);
    }

    // <editor-fold desc="Report Compiler Errors">
    @Override
    @OverridingMethodsMustInvokeSuper
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        this.reportDuplicatePropertyNames(compilerAnnotationHolder);
        this.reportMultiplePropertiesWithModifiers(compilerAnnotationHolder, this.declaredDataTypeProperties, "id");
        this.reportMultiplePropertiesWithModifiers(
            compilerAnnotationHolder,
            this.declaredDataTypeProperties,
            "version"
        );
        this.reportMultiplePropertiesWithModifiers(
            compilerAnnotationHolder,
            this.declaredDataTypeProperties,
            "createdBy"
        );
        this.reportMultiplePropertiesWithModifiers(
            compilerAnnotationHolder,
            this.declaredDataTypeProperties,
            "lastUpdatedBy"
        );
        this.reportMultiplePropertiesWithModifiers(
            compilerAnnotationHolder,
            this.declaredReferenceProperties,
            "version"
        );
        this.reportMultiplePropertiesWithModifiers(
            compilerAnnotationHolder,
            this.declaredReferenceProperties,
            "createdBy"
        );
        this.reportMultiplePropertiesWithModifiers(
            compilerAnnotationHolder,
            this.declaredReferenceProperties,
            "lastUpdatedBy"
        );
        this.reportIdAndKeyProperties(compilerAnnotationHolder);
        this.reportInterfaceNotFound(compilerAnnotationHolder);
        this.reportRedundantInterface(compilerAnnotationHolder);
        this.reportCircularInheritance(compilerAnnotationHolder);
        this.reportPropertyDeclarationOrder(compilerAnnotationHolder);
        this.reportDuplicateAssociationEndSignatureNames(compilerAnnotationHolder);
        this.reportRedundantTemporalModifiers(compilerAnnotationHolder);
    }

    private void reportDuplicatePropertyNames(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        ImmutableBag<String> duplicateMemberNames = this.getDuplicateMemberNames();
        for (AntlrProperty property : this.declaredProperties) {
            if (duplicateMemberNames.contains(property.getName())) {
                property.reportDuplicateMemberName(compilerAnnotationHolder);
            }
            property.reportErrors(compilerAnnotationHolder);
        }
    }

    protected <T extends AntlrProperty> void reportMultiplePropertiesWithModifiers(
        @Nonnull CompilerAnnotationHolder compilerAnnotationHolder,
        MutableList<T> properties,
        String... modifiersArray
    ) {
        ImmutableList<String> modifiers = Lists.immutable.with(modifiersArray);
        MutableList<T> duplicatePropertyWithModifiers = properties.select((property) ->
            modifiers.allSatisfy((modifier) -> property.getModifiers().anySatisfyWith(AntlrModifier::is, modifier))
        );

        if (duplicatePropertyWithModifiers.size() <= 1) {
            return;
        }

        for (AntlrProperty property : duplicatePropertyWithModifiers) {
            property.reportDuplicatePropertyWithModifiers(compilerAnnotationHolder, modifiers);
        }
    }

    private void reportIdAndKeyProperties(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        MutableList<AntlrDataTypeProperty<?>> idProperties = this.declaredDataTypeProperties.select(
            AntlrDataTypeProperty::isId
        );
        if (idProperties.isEmpty()) {
            return;
        }

        ImmutableList<AntlrDataTypeProperty<?>> nonIdKeyProperties = this.getAllKeyProperties().reject(
            AntlrDataTypeProperty::isId
        );
        if (nonIdKeyProperties.isEmpty()) {
            return;
        }

        for (AntlrDataTypeProperty<?> idProperty : idProperties) {
            idProperty.reportIdPropertyWithKeyProperties(compilerAnnotationHolder);
        }

        for (AntlrDataTypeProperty<?> nonIdKeyProperty : nonIdKeyProperties) {
            nonIdKeyProperty.reportKeyPropertyWithIdProperties(compilerAnnotationHolder);
        }
    }

    private void reportInterfaceNotFound(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        for (int i = 0; i < this.declaredInterfaces.size(); i++) {
            AntlrInterface iface = this.declaredInterfaces.get(i);
            if (iface == AntlrInterface.NOT_FOUND) {
                InterfaceReferenceContext offendingToken = this.getOffendingInterfaceReference(i);
                String message = String.format("Cannot find interface '%s'.", offendingToken.getText());
                compilerAnnotationHolder.add("ERR_IMP_INT", message, this, offendingToken);
            }
        }
    }

    private void reportRedundantInterface(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        MutableSet<AntlrInterface> visitedInterfaces = Sets.mutable.empty();

        for (int i = 0; i < this.declaredInterfaces.size(); i++) {
            AntlrInterface iface = this.declaredInterfaces.get(i);
            if (iface == AntlrInterface.NOT_FOUND) {
                continue;
            }

            if (visitedInterfaces.contains(iface)) {
                InterfaceReferenceContext offendingToken = this.getOffendingInterfaceReference(i);
                String message = String.format("Duplicate interface '%s'.", offendingToken.getText());
                compilerAnnotationHolder.add("ERR_DUP_INT", message, this, offendingToken);
            }

            if (this.isInterfaceRedundant(i, iface)) {
                InterfaceReferenceContext offendingToken = this.getOffendingInterfaceReference(i);
                String message = String.format("Redundant interface '%s'.", offendingToken.getText());
                compilerAnnotationHolder.add("ERR_RED_INT", message, this, offendingToken);
            }

            visitedInterfaces.add(iface);
        }
    }

    private void reportDuplicateAssociationEndSignatureNames(
        @Nonnull CompilerAnnotationHolder compilerAnnotationHolder
    ) {
        ImmutableBag<String> duplicateMemberNames = this.getDuplicateMemberNames();

        for (AntlrAssociationEndSignature associationEndSignature : this.declaredAssociationEndSignatures) {
            if (duplicateMemberNames.contains(associationEndSignature.getName())) {
                associationEndSignature.reportDuplicateMemberName(compilerAnnotationHolder);
            }
            associationEndSignature.reportErrors(compilerAnnotationHolder);
        }
    }

    private void reportRedundantTemporalModifiers(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        boolean hasSystemTemporal = this.declaredModifiers.anySatisfy(AntlrModifier::isSystemTemporal);
        boolean hasValidTemporal = this.declaredModifiers.anySatisfy(AntlrModifier::isValidTemporal);
        boolean hasBitemporal = this.declaredModifiers.anySatisfy(AntlrModifier::isBitemporal);

        if (!hasBitemporal || (!hasSystemTemporal && !hasValidTemporal)) {
            return;
        }

        MutableList<AntlrModifier> temporalModifiers = this.declaredModifiers.select(AntlrModifier::isTemporal);

        String message = String.format(
            "Class '%s' has redundant temporal modifiers. The 'bitemporal' modifier is redundant with 'systemTemporal' and 'validTemporal'.",
            this.getName()
        );

        compilerAnnotationHolder.add(
            "ERR_RED_TMP",
            message,
            this,
            temporalModifiers.collect(AntlrModifier::getElementContext).toImmutable()
        );
    }

    protected void reportCircularInheritance(CompilerAnnotationHolder compilerAnnotationHolder) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".reportCircularInheritance() not implemented yet"
        );
    }

    protected void reportPropertyDeclarationOrder(CompilerAnnotationHolder compilerAnnotationHolder) {
        ImmutableList<AntlrDataTypeProperty<?>> dataTypeProperties = this.declaredDataTypeProperties.reject(
            AntlrElement::hasMacro
        ).toImmutable();

        MutableList<AntlrDataTypeProperty<?>> orderedDataTypeProperties = Lists.mutable.empty();

        ImmutableList<AntlrDataTypeProperty<?>> foreignKeys = dataTypeProperties.select(
            AntlrDataTypeProperty::isForeignKey
        );

        ImmutableList<AntlrDataTypeProperty<?>> keysAndForeignKeys = foreignKeys
            .select(AntlrDataTypeProperty::isKey)
            .reject(orderedDataTypeProperties::contains);
        orderedDataTypeProperties.addAllIterable(keysAndForeignKeys);
        ImmutableList<AntlrDataTypeProperty<?>> keys = dataTypeProperties
            .select(AntlrDataTypeProperty::isKey)
            .reject(AntlrDataTypeProperty::isForeignKey)
            .reject(orderedDataTypeProperties::contains);
        orderedDataTypeProperties.addAllIterable(keys);
        ImmutableList<AntlrDataTypeProperty<?>> nonKeyForeignKeys = foreignKeys
            .reject(AntlrDataTypeProperty::isKey)
            .reject(AntlrDataTypeProperty::isCreatedBy)
            .reject(AntlrDataTypeProperty::isLastUpdatedBy)
            .reject(orderedDataTypeProperties::contains);
        orderedDataTypeProperties.addAllIterable(nonKeyForeignKeys);
        ImmutableList<AntlrDataTypeProperty<?>> system = dataTypeProperties
            .select(AntlrDataTypeProperty::isSystemRange)
            .reject(orderedDataTypeProperties::contains);
        orderedDataTypeProperties.addAllIterable(system);
        ImmutableList<AntlrDataTypeProperty<?>> systemFrom = dataTypeProperties
            .select(AntlrDataTypeProperty::isSystemFrom)
            .reject(orderedDataTypeProperties::contains);
        orderedDataTypeProperties.addAllIterable(systemFrom);
        ImmutableList<AntlrDataTypeProperty<?>> systemTo = dataTypeProperties
            .select(AntlrDataTypeProperty::isSystemTo)
            .reject(orderedDataTypeProperties::contains);
        orderedDataTypeProperties.addAllIterable(systemTo);
        ImmutableList<AntlrDataTypeProperty<?>> valid = dataTypeProperties
            .select(AntlrDataTypeProperty::isValidRange)
            .reject(orderedDataTypeProperties::contains);
        orderedDataTypeProperties.addAllIterable(valid);
        ImmutableList<AntlrDataTypeProperty<?>> validFrom = dataTypeProperties
            .select(AntlrDataTypeProperty::isValidFrom)
            .reject(orderedDataTypeProperties::contains);
        orderedDataTypeProperties.addAllIterable(validFrom);
        ImmutableList<AntlrDataTypeProperty<?>> validTo = dataTypeProperties
            .select(AntlrDataTypeProperty::isValidTo)
            .reject(orderedDataTypeProperties::contains);
        orderedDataTypeProperties.addAllIterable(validTo);
        ImmutableList<AntlrDataTypeProperty<?>> createdBy = dataTypeProperties
            .select(AntlrDataTypeProperty::isCreatedBy)
            .reject(AntlrDataTypeProperty::isKey)
            .reject(orderedDataTypeProperties::contains);
        orderedDataTypeProperties.addAllIterable(createdBy);
        ImmutableList<AntlrDataTypeProperty<?>> createdOn = dataTypeProperties
            .select(AntlrDataTypeProperty::isCreatedOn)
            .reject(orderedDataTypeProperties::contains);
        orderedDataTypeProperties.addAllIterable(createdOn);
        ImmutableList<AntlrDataTypeProperty<?>> lastUpdatedBy = dataTypeProperties
            .select(AntlrDataTypeProperty::isLastUpdatedBy)
            .reject(AntlrDataTypeProperty::isKey)
            .reject(orderedDataTypeProperties::contains);
        orderedDataTypeProperties.addAllIterable(lastUpdatedBy);
        ImmutableList<AntlrDataTypeProperty<?>> otherDataTypeProperties = dataTypeProperties.reject(
            orderedDataTypeProperties::contains
        );
        orderedDataTypeProperties.addAllIterable(otherDataTypeProperties);

        if (!orderedDataTypeProperties.equals(orderedDataTypeProperties.distinct())) {
            throw new AssertionError(orderedDataTypeProperties);
        }

        if (!dataTypeProperties.equals(orderedDataTypeProperties)) {
            String allAdditionalContext =
                ""
                + this.getContext("keys and foreign keys:     ", keysAndForeignKeys)
                + this.getContext("keys:                      ", keys)
                + this.getContext("foreign keys but not keys: ", nonKeyForeignKeys)
                + this.getContext("system range:              ", system)
                + this.getContext("system from:               ", systemFrom)
                + this.getContext("system to:                 ", systemTo)
                + this.getContext("valid range:               ", valid)
                + this.getContext("valid from:                ", validFrom)
                + this.getContext("valid to:                  ", validTo)
                + this.getContext("created by:                ", createdBy)
                + this.getContext("created on:                ", createdOn)
                + this.getContext("last updated by:           ", lastUpdatedBy)
                + this.getContext("Other:                     ", otherDataTypeProperties);

            String message = String.format(
                "The properties of class '%s' are not declared in the correct order. Expected '%s' but found '%s'.%n%s",
                this.getName(),
                orderedDataTypeProperties.collect(AntlrNamedElement::getName),
                dataTypeProperties.collect(AntlrNamedElement::getName),
                allAdditionalContext
            );
            compilerAnnotationHolder.add("ERR_DTP_ORD", message, this);
        }
    }

    @Nonnull
    private String getContext(String description, ImmutableList<AntlrDataTypeProperty<?>> properties) {
        return properties.isEmpty() ? "" : description + properties.collect(AntlrNamedElement::getName) + ".\n";
    }

    @OverridingMethodsMustInvokeSuper
    public void reportAuditErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        this.reportAuditErrors(compilerAnnotationHolder, this.declaredModifiers, this);
        this.declaredDataTypeProperties.each((each) -> each.reportAuditErrors(compilerAnnotationHolder));
    }

    protected void reportForwardReference(CompilerAnnotationHolder compilerAnnotationHolder) {
        for (int i = 0; i < this.declaredInterfaces.size(); i++) {
            AntlrInterface iface = this.declaredInterfaces.get(i);
            if (this.isForwardReference(iface)) {
                String message = String.format(
                    "Class '%s' is declared on line %d and has a forward reference to implemented interface '%s' which is declared later in the source file '%s' on line %d.",
                    this.getName(),
                    this.getElementContext().getStart().getLine(),
                    iface.getName(),
                    this.getCompilationUnit().get().getSourceName(),
                    iface.getElementContext().getStart().getLine()
                );
                compilerAnnotationHolder.add("ERR_FWD_REF", message, this, this.getOffendingInterfaceReference(i));
            }
        }
    }

    // </editor-fold>

    protected boolean isInterfaceRedundant(int index, @Nonnull AntlrInterface iface) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".isInterfaceRedundant() not implemented yet"
        );
    }

    protected boolean interfaceNotAtIndexImplements(int index, @Nonnull AntlrInterface iface) {
        return Interval.zeroTo(this.declaredInterfaces.size() - 1)
            .asLazy()
            .reject((i) -> i == index)
            .collect(this.declaredInterfaces::get)
            .anySatisfyWith(AntlrClassifier::implementsInterface, iface);
    }

    protected InterfaceReferenceContext getOffendingInterfaceReference(int index) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".getOffendingInterfaceReference() not implemented yet"
        );
    }

    protected ImmutableBag<String> getDuplicateMemberNames() {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".getDuplicateMemberNames() not implemented yet"
        );
    }

    protected AntlrDataTypeProperty<?> getInterfaceDataTypePropertyByName(String name) {
        return this.declaredInterfaces.asLazy()
            .<String, AntlrDataTypeProperty<?>>collectWith(AntlrInterface::getDataTypePropertyByName, name)
            .detectOptional((interfaceProperty) -> interfaceProperty != AntlrEnumerationProperty.NOT_FOUND)
            .orElse(AntlrEnumerationProperty.NOT_FOUND);
    }

    protected AntlrModifier getInterfaceClassifierModifierByName(String name) {
        return this.declaredInterfaces.asLazy()
            .collectWith(AntlrInterface::getModifierByName, name)
            .detectOptional((interfaceModifier) -> interfaceModifier != AntlrModifier.NOT_FOUND)
            .orElse(AntlrModifier.NOT_FOUND);
    }

    public boolean isSubTypeOf(AntlrClassifier classifier) {
        if (classifier instanceof AntlrInterface iface) {
            return this.implementsInterface(iface);
        }

        if (classifier instanceof AntlrClass klass) {
            if (this instanceof AntlrClass thisClass) {
                return thisClass.isSubClassOf(klass);
            }
            return false;
        }

        throw new AssertionError(
            "Expected AntlrInterface or AntlrClass but found " + classifier.getClass().getSimpleName()
        );
    }

    @OverridingMethodsMustInvokeSuper
    public boolean implementsInterface(AntlrInterface iface) {
        return (
            this.declaredInterfaces.contains(iface)
            || this.declaredInterfaces.anySatisfyWith(AntlrClassifier::implementsInterface, iface)
        );
    }

    public ImmutableList<AntlrDataTypeProperty<?>> getAllKeyProperties() {
        return this.getAllDataTypeProperties().select(AntlrDataTypeProperty::isKey);
    }

    public ImmutableList<AntlrDataTypeProperty<?>> getOverriddenDataTypeProperties(String name) {
        MutableList<AntlrDataTypeProperty<?>> overriddenProperties = Lists.mutable.empty();
        MutableSet<AntlrClassifier> visited = Sets.mutable.empty();
        this.getOverriddenDataTypeProperties(name, overriddenProperties, visited);
        return overriddenProperties.toImmutable();
    }

    protected void getOverriddenDataTypeProperties(
        String name,
        MutableList<AntlrDataTypeProperty<?>> overriddenProperties,
        MutableSet<AntlrClassifier> visited
    ) {
        if (visited.contains(this)) {
            return;
        }
        visited.add(this);

        AntlrDataTypeProperty<?> antlrDataTypeProperty = this.declaredDataTypePropertiesByName.get(name);
        if (antlrDataTypeProperty != null) {
            overriddenProperties.add(antlrDataTypeProperty);
        }

        this.getSuperClass().ifPresent((antlrClass) ->
            antlrClass.getOverriddenDataTypeProperties(name, overriddenProperties, visited)
        );

        for (AntlrInterface iface : this.declaredInterfaces) {
            iface.getOverriddenDataTypeProperties(name, overriddenProperties, visited);
        }
    }

    public Optional<AntlrClass> getSuperClass() {
        return Optional.empty();
    }
}
