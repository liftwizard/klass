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

import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.AssociationEndSignature;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.ParameterizedProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.PropertyVisitor;
import cool.klass.model.meta.domain.api.source.property.AssociationEndSignatureWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.AssociationEndWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.EnumerationPropertyWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.ParameterizedPropertyWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.PrimitivePropertyWithSourceCode;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndSignatureContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationPropertyContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterizedPropertyContext;
import cool.klass.model.meta.grammar.KlassParser.PrimitivePropertyContext;

public class DomainModelDeclarationsPropertyVisitor implements PropertyVisitor {

    @Nonnull
    private final DomainModelDeclarations domainModelDeclarations;

    public DomainModelDeclarationsPropertyVisitor(@Nonnull DomainModelDeclarations domainModelDeclarations) {
        this.domainModelDeclarations = Objects.requireNonNull(domainModelDeclarations);
    }

    @Override
    public void visitPrimitiveProperty(PrimitiveProperty primitiveProperty) {
        PrimitivePropertyWithSourceCode element = (PrimitivePropertyWithSourceCode) primitiveProperty;
        PrimitivePropertyContext declaration = element.getElementContext();
        this.domainModelDeclarations.addPrimitivePropertyDeclaration(declaration, element);
    }

    @Override
    public void visitEnumerationProperty(EnumerationProperty enumerationProperty) {
        EnumerationPropertyWithSourceCode element = (EnumerationPropertyWithSourceCode) enumerationProperty;
        EnumerationPropertyContext declaration = element.getElementContext();
        this.domainModelDeclarations.addEnumerationPropertyDeclaration(declaration, element);
    }

    @Override
    public void visitAssociationEnd(AssociationEnd associationEnd) {
        AssociationEndWithSourceCode element = (AssociationEndWithSourceCode) associationEnd;
        AssociationEndContext declaration = element.getElementContext();
        this.domainModelDeclarations.addAssociationEndDeclaration(declaration, element);
    }

    @Override
    public void visitAssociationEndSignature(AssociationEndSignature associationEndSignature) {
        AssociationEndSignatureWithSourceCode element = (AssociationEndSignatureWithSourceCode) associationEndSignature;
        AssociationEndSignatureContext declaration = element.getElementContext();
        this.domainModelDeclarations.addAssociationEndSignatureDeclaration(declaration, element);
    }

    @Override
    public void visitParameterizedProperty(ParameterizedProperty parameterizedProperty) {
        ParameterizedPropertyWithSourceCode element = (ParameterizedPropertyWithSourceCode) parameterizedProperty;
        ParameterizedPropertyContext declaration = element.getElementContext();
        this.domainModelDeclarations.addParameterizedPropertyDeclaration(declaration, element);
        // TODO: Parameter declarations
    }
}
