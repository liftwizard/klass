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

package cool.klass.model.converter.compiler.state.value;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrEnumeration;
import cool.klass.model.converter.compiler.state.AntlrType;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEnd;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrEnumerationProperty;
import cool.klass.model.meta.domain.property.AssociationEndImpl.AssociationEndBuilder;
import cool.klass.model.meta.domain.value.TypeMemberReferencePathImpl.TypeMemberReferencePathBuilder;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ClassReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.TypeMemberReferencePathContext;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

public class AntlrTypeMemberReferencePath extends AntlrMemberReferencePath {

    private TypeMemberReferencePathBuilder elementBuilder;

    public AntlrTypeMemberReferencePath(
        @Nonnull TypeMemberReferencePathContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        @Nonnull AntlrClass klass,
        @Nonnull ImmutableList<AntlrAssociationEnd> associationEnds,
        @Nonnull AntlrDataTypeProperty<?> dataTypeProperty,
        @Nonnull IAntlrElement expressionValueOwner
    ) {
        super(elementContext, compilationUnit, klass, associationEnds, dataTypeProperty, expressionValueOwner);
    }

    @Nonnull
    @Override
    public TypeMemberReferencePathBuilder build() {
        if (this.elementBuilder != null) {
            throw new IllegalStateException();
        }
        ImmutableList<AssociationEndBuilder> associationEndBuilders =
            this.associationEnd.collect(AntlrAssociationEnd::getElementBuilder);

        this.elementBuilder = new TypeMemberReferencePathBuilder(
            (TypeMemberReferencePathContext) this.elementContext,
            this.getMacroElementBuilder(),
            this.getSourceCodeBuilder(),
            this.klass.getElementBuilder(),
            associationEndBuilders,
            this.dataTypeProperty.getElementBuilder()
        );
        return this.elementBuilder;
    }

    @Nonnull
    @Override
    public TypeMemberReferencePathBuilder getElementBuilder() {
        return Objects.requireNonNull(this.elementBuilder);
    }

    @Override
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (this.klass == AntlrClass.AMBIGUOUS) {
            // Covered by ERR_DUP_TOP
            return;
        }

        if (this.klass == AntlrClass.NOT_FOUND) {
            ClassReferenceContext offendingToken = this.getElementContext().classReference();

            String message = String.format("Cannot find class '%s'.", offendingToken.getText());

            compilerAnnotationHolder.add("ERR_MEM_TYP", message, this, offendingToken);
            return;
        }

        List<AssociationEndReferenceContext> associationEndReferenceContexts =
            this.getElementContext().associationEndReference();
        AntlrClass currentClass =
            this.reportErrorsAssociationEnds(compilerAnnotationHolder, associationEndReferenceContexts);
        if (currentClass == null) {
            return;
        }

        if (this.dataTypeProperty == AntlrEnumerationProperty.NOT_FOUND) {
            IdentifierContext identifier = this.getElementContext().memberReference().identifier();
            String message = String.format("Cannot find member '%s.%s'.", currentClass.getName(), identifier.getText());
            compilerAnnotationHolder.add("ERR_TYP_MEM", message, this, identifier);
        }
    }

    @Nonnull
    @Override
    public ImmutableList<AntlrType> getPossibleTypes() {
        AntlrType type = this.dataTypeProperty.getType();
        return type == AntlrEnumeration.NOT_FOUND || type == AntlrEnumeration.AMBIGUOUS
            ? Lists.immutable.empty()
            : Lists.immutable.with(type);
    }

    @Override
    public void visit(AntlrExpressionValueVisitor visitor) {
        visitor.visitTypeMember(this);
    }

    @Nonnull
    @Override
    public TypeMemberReferencePathContext getElementContext() {
        return (TypeMemberReferencePathContext) super.getElementContext();
    }
}
