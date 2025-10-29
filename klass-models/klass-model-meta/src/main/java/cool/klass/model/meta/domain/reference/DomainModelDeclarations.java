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

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.source.AssociationWithSourceCode;
import cool.klass.model.meta.domain.api.source.ElementWithSourceCode;
import cool.klass.model.meta.domain.api.source.EnumerationLiteralWithSourceCode;
import cool.klass.model.meta.domain.api.source.EnumerationWithSourceCode;
import cool.klass.model.meta.domain.api.source.InterfaceWithSourceCode;
import cool.klass.model.meta.domain.api.source.KlassWithSourceCode;
import cool.klass.model.meta.domain.api.source.projection.ProjectionWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.AssociationEndSignatureWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.AssociationEndWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.EnumerationPropertyWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.ParameterizedPropertyWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.PrimitivePropertyWithSourceCode;
import cool.klass.model.meta.domain.api.source.service.ServiceGroupWithSourceCode;
import cool.klass.model.meta.grammar.KlassParser.AssociationDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndSignatureContext;
import cool.klass.model.meta.grammar.KlassParser.ClassDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationPropertyContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterizedPropertyContext;
import cool.klass.model.meta.grammar.KlassParser.PrimitivePropertyContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceGroupDeclarationContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.map.MutableMapIterable;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;

public class DomainModelDeclarations {

    private final MutableMapIterable<Token, ElementWithSourceCode> elementsByDeclaration = OrderedMapAdapter.adapt(
        new LinkedHashMap<>()
    );

    public Optional<ElementWithSourceCode> getElementByDeclaration(@Nonnull Token token) {
        Objects.requireNonNull(token);
        return Optional.ofNullable(this.elementsByDeclaration.get(token));
    }

    public void addEnumerationDeclaration(
        @Nonnull EnumerationDeclarationContext declaration,
        @Nonnull EnumerationWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = declaration.identifier().getStart();
        this.elementsByDeclaration.put(token, element);
    }

    public void addEnumerationLiteralDeclaration(
        @Nonnull EnumerationLiteralContext declaration,
        @Nonnull EnumerationLiteralWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = declaration.identifier().getStart();
        this.elementsByDeclaration.put(token, element);
    }

    public void addInterfaceDeclaration(
        @Nonnull InterfaceDeclarationContext declaration,
        @Nonnull InterfaceWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = declaration.interfaceHeader().identifier().getStart();
        this.elementsByDeclaration.put(token, element);
    }

    public void addKlassDeclaration(
        @Nonnull ClassDeclarationContext declaration,
        @Nonnull KlassWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = declaration.classHeader().identifier().getStart();
        this.elementsByDeclaration.put(token, element);
    }

    public void addPrimitivePropertyDeclaration(
        @Nonnull PrimitivePropertyContext declaration,
        @Nonnull PrimitivePropertyWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = declaration.identifier().getStart();
        this.elementsByDeclaration.put(token, element);
    }

    public void addEnumerationPropertyDeclaration(
        @Nonnull EnumerationPropertyContext declaration,
        @Nonnull EnumerationPropertyWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = declaration.identifier().getStart();
        this.elementsByDeclaration.put(token, element);
    }

    public void addAssociationEndDeclaration(
        @Nonnull AssociationEndContext declaration,
        @Nonnull AssociationEndWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = declaration.identifier().getStart();
        this.elementsByDeclaration.put(token, element);
    }

    public void addAssociationEndSignatureDeclaration(
        @Nonnull AssociationEndSignatureContext declaration,
        @Nonnull AssociationEndSignatureWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = declaration.identifier().getStart();
        this.elementsByDeclaration.put(token, element);
    }

    public void addAssociationDeclaration(
        @Nonnull AssociationDeclarationContext declaration,
        @Nonnull AssociationWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = declaration.identifier().getStart();
        this.elementsByDeclaration.put(token, element);
    }

    public void addProjectionDeclaration(
        @Nonnull ProjectionDeclarationContext declaration,
        @Nonnull ProjectionWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = declaration.identifier().getStart();
        this.elementsByDeclaration.put(token, element);
    }

    public void addParameterizedPropertyDeclaration(
        @Nonnull ParameterizedPropertyContext declaration,
        @Nonnull ParameterizedPropertyWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = declaration.identifier().getStart();
        this.elementsByDeclaration.put(token, element);
    }

    public void addServiceGroupDeclaration(
        @Nonnull ServiceGroupDeclarationContext declaration,
        @Nonnull ServiceGroupWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = declaration.identifier().getStart();
        this.elementsByDeclaration.put(token, element);
    }
}
