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

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.criteria.AntlrCriteriaVisitor;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEnd;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import cool.klass.model.meta.domain.AssociationImpl.AssociationBuilder;
import cool.klass.model.meta.domain.criteria.AbstractCriteria.AbstractCriteriaBuilder;
import cool.klass.model.meta.domain.property.AssociationEndImpl.AssociationEndBuilder;
import cool.klass.model.meta.grammar.KlassParser.AssociationBlockContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;

public class AntlrAssociation extends AntlrPackageableElement implements AntlrTopLevelElement {

    // <editor-fold desc="AMBIGUOUS">
    public static final AntlrAssociation AMBIGUOUS = new AntlrAssociation(
        new AssociationDeclarationContext(AMBIGUOUS_PARENT, -1),
        AntlrCompilationUnit.AMBIGUOUS,
        -1,
        AMBIGUOUS_IDENTIFIER_CONTEXT
    ) {
        @Override
        public void enterAssociationEnd(@Nonnull AntlrAssociationEnd associationEnd) {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".enterAssociationEnd() not implemented yet"
            );
        }
    };
    // </editor-fold>

    // <editor-fold desc="NOT_FOUND">
    public static final AntlrAssociation NOT_FOUND = new AntlrAssociation(
        new AssociationDeclarationContext(NOT_FOUND_PARENT, -1),
        AntlrCompilationUnit.NOT_FOUND,
        -1,
        NOT_FOUND_IDENTIFIER_CONTEXT
    ) {
        @Override
        public void enterAssociationEnd(@Nonnull AntlrAssociationEnd associationEnd) {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".enterAssociationEnd() not implemented yet"
            );
        }
    };
    // </editor-fold>

    private final MutableList<AntlrAssociationEnd> associationEnds = Lists.mutable.empty();
    private final MutableOrderedMap<AssociationEndContext, AntlrAssociationEnd> associationEndsByContext =
        OrderedMapAdapter.adapt(new LinkedHashMap<>());

    private AntlrRelationship relationship;

    private AssociationBuilder associationBuilder;

    public AntlrAssociation(
        @Nonnull AssociationDeclarationContext elementContext,
        @Nonnull AntlrCompilationUnit compilationUnitState,
        int ordinal,
        @Nonnull IdentifierContext nameContext
    ) {
        super(elementContext, compilationUnitState, ordinal, nameContext);
    }

    public void visitCriteria(AntlrCriteriaVisitor criteriaVisitor) {
        if (this.relationship == null) {
            return;
        }
        this.relationship.getCriteria().visit(criteriaVisitor);
    }

    @Nonnull
    @Override
    public AssociationDeclarationContext getElementContext() {
        return (AssociationDeclarationContext) this.elementContext;
    }

    @Override
    public AssociationBlockContext getBlockContext() {
        return this.getElementContext().associationBlock();
    }

    public MutableList<AntlrAssociationEnd> getAssociationEnds() {
        return this.associationEnds.asUnmodifiable();
    }

    public int getNumAssociationEnds() {
        return this.associationEnds.size();
    }

    public AntlrAssociationEnd getAssociationEndByContext(AssociationEndContext ctx) {
        return this.associationEndsByContext.get(ctx);
    }

    public void enterAssociationEnd(@Nonnull AntlrAssociationEnd associationEnd) {
        AntlrAssociationEnd duplicate = this.associationEndsByContext.put(
            associationEnd.getElementContext(),
            associationEnd
        );
        if (duplicate != null) {
            throw new AssertionError();
        }

        this.associationEnds.add(associationEnd);
    }

    public void exitAssociationDeclaration() {
        int numAssociationEnds = this.associationEnds.size();
        if (numAssociationEnds != 2) {
            throw new AssertionError(numAssociationEnds);
        }

        AntlrClass sourceType = this.getSourceEnd().getType();
        AntlrClass targetType = this.getTargetEnd().getType();

        this.getSourceEnd().setOpposite(this.getTargetEnd());
        this.getTargetEnd().setOpposite(this.getSourceEnd());

        this.getSourceEnd().setOwningClass(targetType);
        this.getTargetEnd().setOwningClass(sourceType);

        if (sourceType != AntlrClass.NOT_FOUND && sourceType != AntlrClass.AMBIGUOUS) {
            sourceType.enterAssociationEnd(this.getTargetEnd());
        }

        if (targetType != AntlrClass.NOT_FOUND && targetType != AntlrClass.AMBIGUOUS) {
            targetType.enterAssociationEnd(this.getSourceEnd());
        }
    }

    public AssociationBuilder build() {
        if (this.associationBuilder != null) {
            throw new IllegalStateException();
        }

        int numAssociationEnds = this.associationEnds.size();
        if (numAssociationEnds != 2) {
            throw new AssertionError(numAssociationEnds);
        }

        this.associationBuilder = new AssociationBuilder(
            (AssociationDeclarationContext) this.elementContext,
            this.getMacroElementBuilder(),
            this.getSourceCodeBuilder(),
            this.ordinal,
            this.getNameContext(),
            this.getPackageName()
        );

        ImmutableList<AssociationEndBuilder> associationEndBuilders = this.associationEnds.collect(
            AntlrAssociationEnd::build
        ).toImmutable();

        this.associationBuilder.setAssociationEndBuilders(associationEndBuilders);

        AbstractCriteriaBuilder<?> criteriaBuilder = this.relationship.getCriteria().build();
        this.associationBuilder.setCriteriaBuilder(criteriaBuilder);

        return this.associationBuilder;
    }

    @Nonnull
    @Override
    public AssociationBuilder getElementBuilder() {
        return this.associationBuilder;
    }

    @Override
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        int numAssociationEnds = this.associationEnds.size();
        if (numAssociationEnds != 2) {
            String message = String.format(
                "Association '%s' should have 2 ends. Found %d",
                this.getName(),
                numAssociationEnds
            );
            compilerAnnotationHolder.add("ERR_ASO_END", message, this);
            return;
        }

        if (this.getSourceEnd().isOwned() && this.getTargetEnd().isOwned()) {
            String message = String.format(
                "Both association ends are owned in '%s'. At most one end may be owned.",
                this.getName()
            );
            AntlrModifier sourceOwnedModifier = this.getSourceEnd().getModifiers().detect(AntlrModifier::isOwned);
            AntlrModifier targetOwnedModifier = this.getTargetEnd().getModifiers().detect(AntlrModifier::isOwned);

            compilerAnnotationHolder.add(
                "ERR_ASO_OWN",
                message,
                this,
                Lists.immutable
                    .<IAntlrElement>with(this.getTargetEnd())
                    .newWithAll(this.getSourceEnd().getSurroundingElements())
                    .distinct(),
                Lists.immutable.with(sourceOwnedModifier.getElementContext(), targetOwnedModifier.getElementContext())
            );
        } else if (this.getSourceEnd().isToMany() && this.getTargetEnd().isToOne() && this.getTargetEnd().isOwned()) {
            String message = String.format(
                "Association end '%s.%s' is owned, but is on the to-one end of a many-to-one association.",
                this.getTargetEnd().getOwningClassifier().getName(),
                this.getTargetEnd().getName()
            );
            AntlrModifier ownedModifier = this.getTargetEnd().getModifiers().detect(AntlrModifier::isOwned);
            compilerAnnotationHolder.add(
                "ERR_OWN_ONE",
                message,
                ownedModifier,
                Lists.immutable.with(
                    ownedModifier.getElementContext(),
                    this.getElementContext().associationBlock().associationBody().associationEnd(1).multiplicity()
                )
            );
        } else if (this.getSourceEnd().isToOne() && this.getTargetEnd().isToMany() && this.getSourceEnd().isOwned()) {
            String message = String.format(
                "Association end '%s.%s' is owned, but is on the to-one end of a one-to-many association.",
                this.getSourceEnd().getOwningClassifier().getName(),
                this.getSourceEnd().getName()
            );
            AntlrModifier ownedModifier = this.getSourceEnd().getModifiers().detect(AntlrModifier::isOwned);
            compilerAnnotationHolder.add(
                "ERR_OWN_ONE",
                message,
                ownedModifier,
                Lists.immutable.with(
                    ownedModifier.getElementContext(),
                    this.getElementContext().associationBlock().associationBody().associationEnd(0).multiplicity()
                )
            );
        } else if (
            this.getSourceEnd().isToOne()
            && this.getTargetEnd().isToOne()
            && this.getSourceEnd().isToOneRequired() == this.getTargetEnd().isToOneRequired()
            && !this.getSourceEnd().isOwned()
            && !this.getTargetEnd().isOwned()
        ) {
            String message = String.format(
                "Association '%s' is perfectly symmetrical, so foreign keys cannot be inferred. To break the symmetry, make one end owned, or make one end required and the other end optional.",
                this.getName()
            );
            compilerAnnotationHolder.add(
                "ERR_ASO_SYM",
                message,
                this,
                Lists.immutable
                    .<IAntlrElement>with(this.getTargetEnd())
                    .newWithAll(this.getSourceEnd().getSurroundingElements())
                    .distinct(),
                Lists.immutable.with(
                    this.getSourceEnd().getMultiplicity().getElementContext(),
                    this.getTargetEnd().getMultiplicity().getElementContext()
                )
            );
        }

        if (
            this.getSourceEnd().getType() == AntlrClass.NOT_FOUND
            || this.getTargetEnd().getType() == AntlrClass.NOT_FOUND
        ) {
            this.getSourceEnd().reportTypeNotFound(compilerAnnotationHolder);
            this.getTargetEnd().reportTypeNotFound(compilerAnnotationHolder);

            return;
        }

        if (
            this.getSourceEnd().getType() == AntlrClass.AMBIGUOUS
            || this.getTargetEnd().getType() == AntlrClass.AMBIGUOUS
        ) {
            return;
        }

        if (this.relationship == null) {
            String message = String.format("Association '%s' has no relationship", this.getName());
            compilerAnnotationHolder.add("ERR_ASO_REL", message, this);
        } else {
            this.relationship.reportErrors(compilerAnnotationHolder);
        }
    }

    public AntlrRelationship getRelationship() {
        return Objects.requireNonNull(this.relationship);
    }

    public void setRelationship(@Nonnull AntlrRelationship relationship) {
        if (this.relationship != null) {
            throw new IllegalStateException();
        }
        this.relationship = Objects.requireNonNull(relationship);
    }

    public AntlrAssociationEnd getSourceEnd() {
        return this.associationEnds.get(0);
    }

    public AntlrAssociationEnd getTargetEnd() {
        return this.associationEnds.get(1);
    }

    public boolean isManyToMany() {
        return this.getSourceEnd().isToMany() && this.getTargetEnd().isToMany();
    }
}
