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

package cool.klass.model.converter.compiler.state.property;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.AnnotationSeverity;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrAssociation;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.order.AntlrOrderBy;
import cool.klass.model.meta.domain.order.OrderByImpl.OrderByBuilder;
import cool.klass.model.meta.domain.property.AssociationEndImpl.AssociationEndBuilder;
import cool.klass.model.meta.domain.property.ModifierImpl.ModifierBuilder;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;

public class AntlrAssociationEnd extends AntlrClassReferenceProperty {

    // <editor-fold desc="AMBIGUOUS">
    public static final AntlrAssociationEnd AMBIGUOUS = new AntlrAssociationEnd(
        new AssociationEndContext(AMBIGUOUS_PARENT, -1),
        Optional.empty(),
        -1,
        AMBIGUOUS_IDENTIFIER_CONTEXT,
        AntlrAssociation.AMBIGUOUS
    ) {
        @Nonnull
        @Override
        public AntlrClass getType() {
            return AntlrClass.AMBIGUOUS;
        }

        @Override
        public String toString() {
            return AntlrAssociationEnd.class.getSimpleName() + ".AMBIGUOUS";
        }
    };
    // </editor-fold>

    // <editor-fold desc="NOT_FOUND">
    public static final AntlrAssociationEnd NOT_FOUND = new AntlrAssociationEnd(
        new AssociationEndContext(NOT_FOUND_PARENT, -1),
        Optional.empty(),
        -1,
        NOT_FOUND_IDENTIFIER_CONTEXT,
        AntlrAssociation.NOT_FOUND
    ) {
        @Nonnull
        @Override
        public AntlrClass getType() {
            return AntlrClass.NOT_FOUND;
        }

        @Override
        public String toString() {
            return AntlrAssociationEnd.class.getSimpleName() + ".NOT_FOUND";
        }
    };

    // </editor-fold>

    @Nonnull
    private final AntlrAssociation owningAssociation;

    private AntlrClass owningClass;
    private AntlrAssociationEnd opposite;

    private AssociationEndBuilder associationEnd;

    private final MutableOrderedMap<AntlrDataTypeProperty<?>, AntlrDataTypeProperty<?>> foreignKeys =
        OrderedMapAdapter.adapt(new LinkedHashMap<>());

    public AntlrAssociationEnd(
        @Nonnull AssociationEndContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        int ordinal,
        @Nonnull IdentifierContext nameContext,
        @Nonnull AntlrAssociation owningAssociation
    ) {
        super(elementContext, compilationUnit, ordinal, nameContext);
        this.owningAssociation = Objects.requireNonNull(owningAssociation);
    }

    @Nonnull
    @Override
    public Optional<IAntlrElement> getSurroundingElement() {
        return Optional.of(this.owningAssociation);
    }

    @Nonnull
    public AntlrAssociation getOwningAssociation() {
        return this.owningAssociation;
    }

    @Nonnull
    @Override
    public AssociationEndBuilder build() {
        if (this.associationEnd != null) {
            throw new IllegalStateException();
        }

        // TODO: 🔗 Set association end's opposite
        this.associationEnd = new AssociationEndBuilder(
            this.getElementContext(),
            this.getMacroElementBuilder(),
            this.getSourceCodeBuilder(),
            this.ordinal,
            this.getNameContext(),
            this.getType().getElementBuilder(),
            this.owningClass.getElementBuilder(),
            this.owningAssociation.getElementBuilder(),
            this.multiplicity.getMultiplicity()
        );

        ImmutableList<ModifierBuilder> modifiers = this.getModifiers().collect(AntlrModifier::build).toImmutable();

        this.associationEnd.setModifiers(modifiers);

        Optional<OrderByBuilder> orderBy = this.orderBy.map(AntlrOrderBy::build);
        this.associationEnd.setOrderBy(orderBy);

        return this.associationEnd;
    }

    // <editor-fold desc="Report Compiler Errors">
    @Override
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        super.reportErrors(compilerAnnotationHolder);

        this.orderBy.ifPresent((o) -> o.reportErrors(compilerAnnotationHolder));

        this.reportInvalidMultiplicity(compilerAnnotationHolder);
        this.reportVersionEndUnowned(compilerAnnotationHolder);
        this.reportNonVersionEnd(compilerAnnotationHolder);
        this.reportNonUserAuditEnd(compilerAnnotationHolder);
        this.reportPluralName(compilerAnnotationHolder);
        this.reportDeclarationOrderTypes(compilerAnnotationHolder);
        this.reportForwardReference(compilerAnnotationHolder);
    }

    private void reportVersionEndUnowned(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (this.isVersion() && !this.isOwned()) {
            String message = String.format(
                "Expected version association end '%s.%s' to be owned.",
                this.getOwningClassifier().getName(),
                this.getName()
            );
            compilerAnnotationHolder.add("ERR_VER_OWN", message, this, this.nameContext);
        }
    }

    private void reportNonVersionEnd(CompilerAnnotationHolder compilerAnnotationHolder) {
        if (this.isVersion() && !this.getType().isVersion()) {
            String message = String.format(
                "Expected version association end '%s.%s' to have version type, but %s has no version property.",
                this.getOwningClassifier().getName(),
                this.getName(),
                this.getType().getName()
            );
            compilerAnnotationHolder.add("ERR_VER_END", message, this, this.nameContext);
        }
        if (
            !this.getOwningClassifier().isUser()
            && !this.isVersion()
            && this.getType().isVersion()
            && !this.opposite.isCreatedBy()
            && !this.opposite.isLastUpdatedBy()
        ) {
            String message = String.format(
                "Association end '%s.%s' has version type %s, but is missing the version modifier.",
                this.getOwningClassifier().getName(),
                this.getName(),
                this.getType().getName()
            );
            compilerAnnotationHolder.add(
                "ERR_VER_TYP",
                message,
                this,
                Lists.immutable.with(this.nameContext, this.getElementContext().classReference())
            );
        }
    }

    private void reportNonUserAuditEnd(CompilerAnnotationHolder compilerAnnotationHolder) {
        if (this.isCreatedBy() && !this.getType().isUser()) {
            AntlrModifier modifier = this.getModifiers().detect(AntlrModifier::isCreatedBy);
            String message = String.format(
                "Expected createdBy association end '%s.%s' to have user type, but was %s.",
                this.getOwningClassifier().getName(),
                this.getName(),
                this.getType().getName()
            );
            compilerAnnotationHolder.add(
                "ERR_AUD_END",
                message,
                modifier,
                modifier.getSurroundingElements(),
                Lists.immutable.with(this.getElementContext().classReference(), modifier.getElementContext())
            );
        }
        if (this.isLastUpdatedBy() && !this.getType().isUser()) {
            AntlrModifier modifier = this.getModifiers().detect(AntlrModifier::isLastUpdatedBy);
            String message = String.format(
                "Expected lastUpdatedBy association end '%s.%s' to have user type, but was %s.",
                this.getOwningClassifier().getName(),
                this.getName(),
                this.getType().getName()
            );

            compilerAnnotationHolder.add(
                "ERR_AUD_END",
                message,
                modifier,
                modifier.getSurroundingElements(),
                Lists.immutable.with(this.getElementContext().classReference(), modifier.getElementContext())
            );
        }
    }

    public void reportDuplicateOppositeWithModifier(
        @Nonnull CompilerAnnotationHolder compilerAnnotationHolder,
        @Nonnull AntlrClassifier classifier,
        @Nonnull String modifierString,
        @Nonnull AnnotationSeverity severity
    ) {
        AntlrModifier modifier = this.getModifiers().detectWith(AntlrModifier::is, modifierString);
        String message = String.format(
            "Multiple %s association ends point at '%s'.",
            modifierString,
            classifier.getName()
        );
        compilerAnnotationHolder.add("ERR_DUP_END", message, modifier, severity);
    }

    private void reportPluralName(CompilerAnnotationHolder compilerAnnotationHolder) {
        if (this.multiplicity.isToMany()) {
            if (
                this.classReference.getElementContext()
                    .identifier()
                    .getText()
                    .toLowerCase(Locale.ROOT)
                    .endsWith(this.getName().toLowerCase(Locale.ROOT))
            ) {
                String message =
                    "Expected to-many association end '%s.%s' to have a plural name, but name exactly matched type association end type '%s'.".formatted(
                        this.getOwningClassifier().getName(),
                        this.getName(),
                        this.classReference.getElementContext().identifier().getText()
                    );
                compilerAnnotationHolder.add("ERR_ASS_PLU", message, this, this.nameContext);
            }
        }
    }

    private void reportDeclarationOrderTypes(CompilerAnnotationHolder compilerAnnotationHolder) {
        if (!this.isToOneRequired()) {
            return;
        }

        if (this.foreignKeys.isEmpty()) {
            return;
        }

        if (this.isVersion()) {
            // We reach here in the case of a version association end that is not owned
            // This will trigger a different error: ERR_VER_OWN
            return;
        }

        if (this.opposite.getType().isForwardReference(this.getType())) {
            String message = String.format(
                "Association '%s' establishes that type '%s' requires type '%s', so it ought to be declared later in the source file. '%s' is declared on line %d and '%s' is declared on line %d in source file '%s'.",
                this.owningAssociation.getName(),
                this.opposite.getType().getName(),
                this.getType().getName(),
                this.opposite.getType().getName(),
                this.opposite.getType().getElementContext().getStart().getLine(),
                this.getType().getName(),
                this.getType().getElementContext().getStart().getLine(),
                this.getCompilationUnit().get().getSourceName()
            );
            compilerAnnotationHolder.add("ERR_ASO_ORD", message, this, this.getElementContext().classReference());
        }
    }

    private void reportForwardReference(CompilerAnnotationHolder compilerAnnotationHolder) {
        if (!this.owningAssociation.isForwardReference(this.getType())) {
            return;
        }
        String message = String.format(
            "Association end '%s.%s' is declared on line %d and has a forward reference to type '%s' which is declared later in the source file '%s' on line %d.",
            this.getOwningClassifier().getName(),
            this.getName(),
            this.getElementContext().getStart().getLine(),
            this.getType().getName(),
            this.getCompilationUnit().get().getSourceName(),
            this.getType().getElementContext().getStart().getLine()
        );
        compilerAnnotationHolder.add("ERR_ASO_ORD", message, this, this.getElementContext().classReference());
    }

    // </editor-fold>

    @Nonnull
    @Override
    public AntlrClass getOwningClassifier() {
        return Objects.requireNonNull(this.owningClass);
    }

    public void setOwningClass(@Nonnull AntlrClass owningClass) {
        if (this.owningClass != null) {
            throw new IllegalStateException();
        }
        this.owningClass = Objects.requireNonNull(owningClass);
    }

    public boolean isVersioned() {
        return this.opposite.isVersion();
    }

    public void setOpposite(@Nonnull AntlrAssociationEnd opposite) {
        this.opposite = Objects.requireNonNull(opposite);
    }

    @Override
    @Nonnull
    public AssociationEndBuilder getElementBuilder() {
        return Objects.requireNonNull(this.associationEnd);
    }

    @Nonnull
    @Override
    public AssociationEndContext getElementContext() {
        return (AssociationEndContext) super.getElementContext();
    }

    public void addForeignKeyPropertyMatchingProperty(
        @Nonnull AntlrDataTypeProperty<?> foreignKeyProperty,
        @Nonnull AntlrDataTypeProperty<?> keyProperty
    ) {
        this.foreignKeys.put(foreignKeyProperty, keyProperty);
        foreignKeyProperty.setKeyMatchingThisForeignKey(this, keyProperty);
        keyProperty.setForeignKeyMatchingThisKey(this, foreignKeyProperty);
    }

    @Override
    protected IdentifierContext getTypeIdentifier() {
        return this.getElementContext().classReference().identifier();
    }

    public boolean isSourceEnd() {
        return this == this.owningAssociation.getSourceEnd();
    }

    public boolean isTargetEnd() {
        return this == this.owningAssociation.getTargetEnd();
    }

    public AntlrAssociationEnd getOpposite() {
        return this.opposite;
    }
}
