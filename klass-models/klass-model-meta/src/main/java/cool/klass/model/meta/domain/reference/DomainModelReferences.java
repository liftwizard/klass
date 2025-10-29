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

import cool.klass.model.meta.domain.api.source.ClassifierWithSourceCode;
import cool.klass.model.meta.domain.api.source.ElementWithSourceCode;
import cool.klass.model.meta.domain.api.source.EnumerationWithSourceCode;
import cool.klass.model.meta.domain.api.source.KlassWithSourceCode;
import cool.klass.model.meta.domain.api.source.projection.ProjectionWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.AssociationEndWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.DataTypePropertyWithSourceCode;
import cool.klass.model.meta.domain.api.source.property.ReferencePropertyWithSourceCode;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ClassReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ClassifierReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionReferenceContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.map.MutableMapIterable;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;

public class DomainModelReferences {

    private final MutableMapIterable<Token, ElementWithSourceCode> elementsByReference = OrderedMapAdapter.adapt(
        new LinkedHashMap<>()
    );

    public Optional<ElementWithSourceCode> getElementByReference(@Nonnull Token token) {
        Objects.requireNonNull(token);
        return Optional.ofNullable(this.elementsByReference.get(token));
    }

    public void addEnumerationReference(
        @Nonnull EnumerationReferenceContext reference,
        @Nonnull EnumerationWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = reference.identifier().getStart();
        this.elementsByReference.put(token, element);
    }

    public void addClassReference(@Nonnull ClassReferenceContext reference, @Nonnull KlassWithSourceCode element) {
        Objects.requireNonNull(element);
        Token token = reference.identifier().getStart();
        this.elementsByReference.put(token, element);
    }

    public void addClassifierReference(
        @Nonnull ClassifierReferenceContext reference,
        @Nonnull ClassifierWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = reference.identifier().getStart();
        this.elementsByReference.put(token, element);
    }

    public void addReferencePropertyReference(
        @Nonnull IdentifierContext reference,
        @Nonnull ReferencePropertyWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = reference.getStart();
        this.elementsByReference.put(token, element);
    }

    public void addProjectionReference(
        @Nonnull ProjectionReferenceContext reference,
        @Nonnull ProjectionWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = reference.getStart();
        this.elementsByReference.put(token, element);
    }

    public void addDataTypePropertyReference(
        @Nonnull IdentifierContext reference,
        @Nonnull DataTypePropertyWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = reference.getStart();
        this.elementsByReference.put(token, element);
    }

    public void addAssociationEndReference(
        @Nonnull AssociationEndReferenceContext reference,
        @Nonnull AssociationEndWithSourceCode element
    ) {
        Objects.requireNonNull(element);
        Token token = reference.getStart();
        this.elementsByReference.put(token, element);
    }

    public void addUserReference(ParserRuleContext reference, KlassWithSourceCode element) {
        Objects.requireNonNull(element);
        Token token = reference.getStart();
        this.elementsByReference.put(token, element);
    }
}
