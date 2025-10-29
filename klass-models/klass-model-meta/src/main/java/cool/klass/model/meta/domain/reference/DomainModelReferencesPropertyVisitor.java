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

package cool.klass.model.meta.domain.reference;

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.order.OrderBy;
import cool.klass.model.meta.domain.api.order.OrderByMemberReferencePath;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.AssociationEndSignature;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.ParameterizedProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.PropertyVisitor;
import cool.klass.model.meta.domain.api.source.ClassifierWithSourceCode;
import cool.klass.model.meta.domain.api.source.EnumerationWithSourceCode;
import cool.klass.model.meta.domain.api.source.KlassWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.AssociationEndSignatureWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.AssociationEndWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.EnumerationPropertyWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.ParameterizedPropertyWithSourceCode;
import cool.klass.model.meta.domain.api.value.ThisMemberReferencePath;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndSignatureContext;
import cool.klass.model.meta.grammar.KlassParser.ClassReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ClassifierReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationPropertyContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterizedPropertyContext;
import org.eclipse.collections.api.list.ImmutableList;

public class DomainModelReferencesPropertyVisitor implements PropertyVisitor {

    @Nonnull
    private final DomainModelReferences domainModelReferences;

    public DomainModelReferencesPropertyVisitor(@Nonnull DomainModelReferences domainModelReferences) {
        this.domainModelReferences = Objects.requireNonNull(domainModelReferences);
    }

    @Override
    public void visitPrimitiveProperty(PrimitiveProperty primitiveProperty) {
        // Deliberately empty
    }

    @Override
    public void visitEnumerationProperty(EnumerationProperty enumerationProperty) {
        EnumerationPropertyWithSourceCode elementWithSourceCode =
            (EnumerationPropertyWithSourceCode) enumerationProperty;
        EnumerationPropertyContext elementContext = elementWithSourceCode.getElementContext();
        EnumerationReferenceContext reference = elementContext.enumerationReference();
        EnumerationWithSourceCode enumeration = elementWithSourceCode.getType();

        this.domainModelReferences.addEnumerationReference(reference, enumeration);
    }

    @Override
    public void visitAssociationEnd(AssociationEnd associationEnd) {
        AssociationEndWithSourceCode elementWithSourceCode = (AssociationEndWithSourceCode) associationEnd;
        AssociationEndContext elementContext = elementWithSourceCode.getElementContext();
        ClassReferenceContext reference = elementContext.classReference();
        KlassWithSourceCode klass = elementWithSourceCode.getType();

        this.domainModelReferences.addClassReference(reference, klass);

        associationEnd.getOrderBy().ifPresent(this::visitOrderBy);
    }

    @Override
    public void visitAssociationEndSignature(AssociationEndSignature associationEndSignature) {
        AssociationEndSignatureWithSourceCode elementWithSourceCode =
            (AssociationEndSignatureWithSourceCode) associationEndSignature;
        AssociationEndSignatureContext elementContext = elementWithSourceCode.getElementContext();
        ClassifierReferenceContext reference = elementContext.classifierReference();
        ClassifierWithSourceCode classifier = elementWithSourceCode.getType();

        this.domainModelReferences.addClassifierReference(reference, classifier);

        associationEndSignature.getOrderBy().ifPresent(this::visitOrderBy);
    }

    @Override
    public void visitParameterizedProperty(ParameterizedProperty parameterizedProperty) {
        ParameterizedPropertyWithSourceCode elementWithSourceCode =
            (ParameterizedPropertyWithSourceCode) parameterizedProperty;
        ParameterizedPropertyContext elementContext = elementWithSourceCode.getElementContext();
        ClassReferenceContext reference = elementContext.classReference();
        KlassWithSourceCode klass = elementWithSourceCode.getType();

        this.domainModelReferences.addClassReference(reference, klass);

        parameterizedProperty.getOrderBy().ifPresent(this::visitOrderBy);
    }

    public void visitOrderBy(OrderBy orderBy) {
        ImmutableList<OrderByMemberReferencePath> orderByMemberReferencePaths =
            orderBy.getOrderByMemberReferencePaths();
        for (OrderByMemberReferencePath orderByMemberReferencePath : orderByMemberReferencePaths) {
            ThisMemberReferencePath thisMemberReferencePath = orderByMemberReferencePath.getThisMemberReferencePath();
            thisMemberReferencePath.visit(new DomainModelReferencesExpressionValueVisitor(this.domainModelReferences));
        }
    }
}
