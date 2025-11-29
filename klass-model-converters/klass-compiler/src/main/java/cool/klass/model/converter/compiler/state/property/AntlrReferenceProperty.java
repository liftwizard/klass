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

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.AntlrMultiplicity;
import cool.klass.model.converter.compiler.state.AntlrMultiplicityOwner;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.order.AntlrOrderBy;
import cool.klass.model.converter.compiler.state.order.AntlrOrderByOwner;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.property.ReferencePropertyImpl.ReferencePropertyBuilder;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;

public abstract class AntlrReferenceProperty<Type extends AntlrClassifier>
    extends AntlrProperty
    implements AntlrOrderByOwner, AntlrMultiplicityOwner {

    // <editor-fold desc="AMBIGUOUS">
    public static final AntlrReferenceProperty AMBIGUOUS = new AntlrReferenceProperty(
        new ParserRuleContext(AMBIGUOUS_PARENT, -1),
        Optional.empty(),
        -1,
        AMBIGUOUS_IDENTIFIER_CONTEXT
    ) {
        @Nonnull
        @Override
        public AntlrClassifier getType() {
            return AntlrClassifier.AMBIGUOUS;
        }

        @Override
        public String toString() {
            return AntlrReferenceProperty.class.getSimpleName() + ".AMBIGUOUS";
        }

        @Override
        public AntlrMultiplicity getMultiplicity() {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".getMultiplicity() not implemented yet"
            );
        }

        @Nonnull
        @Override
        public ReferencePropertyBuilder<?, ?, ?> build() {
            throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".build() not implemented yet");
        }

        @Nonnull
        @Override
        public ReferencePropertyBuilder<?, ?, ?> getElementBuilder() {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".getElementBuilder() not implemented yet"
            );
        }

        @Override
        protected IdentifierContext getTypeIdentifier() {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".getTypeIdentifier() not implemented yet"
            );
        }

        @Nonnull
        @Override
        public AntlrClassifier getOwningClassifier() {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".getOwningClassifier() not implemented yet"
            );
        }

        @Nonnull
        @Override
        public Optional<IAntlrElement> getSurroundingElement() {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".getSurroundingElement() not implemented yet"
            );
        }
    };
    // </editor-fold>

    // <editor-fold desc="NOT_FOUND">
    public static final AntlrReferenceProperty NOT_FOUND = new AntlrReferenceProperty(
        new ParserRuleContext(NOT_FOUND_PARENT, -1),
        Optional.empty(),
        -1,
        NOT_FOUND_IDENTIFIER_CONTEXT
    ) {
        @Nonnull
        @Override
        public AntlrClassifier getType() {
            return AntlrClassifier.NOT_FOUND;
        }

        @Override
        public String toString() {
            return AntlrReferenceProperty.class.getSimpleName() + ".NOT_FOUND";
        }

        @Nonnull
        @Override
        public Optional<IAntlrElement> getSurroundingElement() {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".getSurroundingElement() not implemented yet"
            );
        }

        @Override
        public AntlrMultiplicity getMultiplicity() {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".getMultiplicity() not implemented yet"
            );
        }

        @Nonnull
        @Override
        public ReferencePropertyBuilder<?, ?, ?> build() {
            throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".build() not implemented yet");
        }

        @Nonnull
        @Override
        public ReferencePropertyBuilder<?, ?, ?> getElementBuilder() {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".getElementBuilder() not implemented yet"
            );
        }

        @Nonnull
        @Override
        public AntlrClassifier getOwningClassifier() {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".getOwningClassifier() not implemented yet"
            );
        }

        @Override
        protected IdentifierContext getTypeIdentifier() {
            throw new UnsupportedOperationException(
                this.getClass().getSimpleName() + ".getTypeIdentifier() not implemented yet"
            );
        }
    };

    // </editor-fold>

    @Nonnull
    protected Optional<AntlrOrderBy> orderBy = Optional.empty();

    protected AntlrMultiplicity multiplicity;

    protected AntlrReferenceProperty(
        @Nonnull ParserRuleContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        int ordinal,
        @Nonnull IdentifierContext nameContext
    ) {
        super(elementContext, compilationUnit, ordinal, nameContext);
    }

    @Override
    @Nonnull
    public abstract Type getType();

    @Nonnull
    @Override
    public abstract ReferencePropertyBuilder<?, ?, ?> build();

    public AntlrMultiplicity getMultiplicity() {
        return this.multiplicity;
    }

    public boolean isToOne() {
        return this.multiplicity != null && this.multiplicity.isToOne();
    }

    public boolean isToMany() {
        return this.multiplicity != null && this.multiplicity.isToMany();
    }

    public boolean isToOneOptional() {
        return this.multiplicity != null && this.multiplicity.getMultiplicity() == Multiplicity.ZERO_TO_ONE;
    }

    public boolean isToOneRequired() {
        return this.multiplicity != null && this.multiplicity.getMultiplicity() == Multiplicity.ONE_TO_ONE;
    }

    public boolean isOwned() {
        return this.getModifiers().anySatisfy(AntlrModifier::isOwned);
    }

    protected void reportInvalidMultiplicity(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (this.multiplicity.getMultiplicity() == null) {
            String multiplicityChoices = ArrayAdapter.adapt(Multiplicity.values())
                .collect(Multiplicity::getPrettyName)
                .collect((each) -> '[' + each + ']')
                .makeString();

            String message = String.format(
                "Reference property '%s: %s[%s..%s]' has invalid multiplicity. Expected one of %s.",
                this.getName(),
                this.getOwningClassifier().getName(),
                this.multiplicity.getLowerBoundText(),
                this.multiplicity.getUpperBoundText(),
                multiplicityChoices
            );

            compilerAnnotationHolder.add("ERR_ASO_MUL", message, this.multiplicity);
        }
    }

    @Nonnull
    @Override
    public abstract ReferencePropertyBuilder<?, ?, ?> getElementBuilder();

    @Override
    public void enterMultiplicity(@Nonnull AntlrMultiplicity multiplicity) {
        if (this.multiplicity != null) {
            throw new AssertionError();
        }

        this.multiplicity = Objects.requireNonNull(multiplicity);
    }

    @Override
    public void enterOrderByDeclaration(@Nonnull AntlrOrderBy orderBy) {
        if (this.orderBy.isPresent()) {
            throw new IllegalStateException();
        }
        this.orderBy = Optional.of(orderBy);
    }

    @Override
    @Nonnull
    public Optional<AntlrOrderBy> getOrderBy() {
        return this.orderBy;
    }

    // <editor-fold desc="Report Compiler Errors">
    @Override
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        super.reportErrors(compilerAnnotationHolder);

        this.reportToOneOrderBy(compilerAnnotationHolder);
    }

    public void reportTypeNotFound(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (this.getType() != AntlrClass.NOT_FOUND) {
            return;
        }

        IdentifierContext offendingToken = this.getTypeIdentifier();
        String message = String.format("Cannot find class '%s'.", offendingToken.getText());
        compilerAnnotationHolder.add("ERR_REF_TYP", message, this, offendingToken);
    }

    public void reportToOneOrderBy(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (!this.isToOne()) {
            return;
        }

        if (this.orderBy.isEmpty()) {
            return;
        }

        String message = String.format(
            "Reference property '%s.%s' is to-one but has an order-by clause. Order by clauses are only valid for "
            + "to-many properties.",
            this.getOwningClassifier().getName(),
            this.getName()
        );
        compilerAnnotationHolder.add("ERR_REF_ORD", message, this.orderBy.get());
    }

    // </editor-fold>

    protected abstract IdentifierContext getTypeIdentifier();

    @Override
    public String getTypeName() {
        return this.getTypeIdentifier().getText();
    }
}
