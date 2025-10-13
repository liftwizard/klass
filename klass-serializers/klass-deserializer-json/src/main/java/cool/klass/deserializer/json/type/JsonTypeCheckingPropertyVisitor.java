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

package cool.klass.deserializer.json.type;

import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import cool.klass.deserializer.json.context.ContextNode;
import cool.klass.deserializer.json.context.ContextStack;
import cool.klass.model.meta.domain.api.Enumeration;
import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.AssociationEndSignature;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.ParameterizedProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.property.PropertyVisitor;
import cool.klass.model.meta.domain.api.visitor.PrimitiveTypeVisitor;
import org.eclipse.collections.api.list.ImmutableList;

public record JsonTypeCheckingPropertyVisitor(
    @Nonnull ContextStack contextStack,
    Property property,
    JsonNode childJsonNode
) implements PropertyVisitor {
    public JsonTypeCheckingPropertyVisitor(
        @Nonnull ContextStack contextStack,
        @Nonnull Property property,
        @Nonnull JsonNode childJsonNode
    ) {
        this.contextStack = Objects.requireNonNull(contextStack);

        this.property = Objects.requireNonNull(property);
        this.childJsonNode = Objects.requireNonNull(childJsonNode);
    }

    private void visitPropertyWithContext(@Nonnull Runnable runnable) {
        this.contextStack.push(new ContextNode(this.property));

        try {
            runnable.run();
        } finally {
            this.contextStack.pop();
        }
    }

    @Override
    public void visitPrimitiveProperty(@Nonnull PrimitiveProperty primitiveProperty) {
        this.visitPropertyWithContext(() -> this.handlePrimitiveProperty(primitiveProperty));
    }

    public void handlePrimitiveProperty(@Nonnull PrimitiveProperty primitiveProperty) {
        PrimitiveType primitiveType = primitiveProperty.getType();
        PrimitiveTypeVisitor visitor = new JsonTypeCheckingPrimitiveTypeVisitor(
            this.contextStack,
            primitiveProperty,
            this.childJsonNode
        );
        primitiveType.visit(visitor);
    }

    @Override
    public void visitEnumerationProperty(@Nonnull EnumerationProperty enumerationProperty) {
        this.visitPropertyWithContext(() -> this.handleEnumerationProperty(enumerationProperty));
    }

    public void handleEnumerationProperty(@Nonnull EnumerationProperty enumerationProperty) {
        if (!this.childJsonNode.isTextual()) {
            String error = String.format(
                "Expected enumerated property with type '%s.%s: %s%s' but got %s with type '%s'.",
                enumerationProperty.getOwningClassifier().getName(),
                enumerationProperty.getName(),
                enumerationProperty.getType().getName(),
                enumerationProperty.isOptional() ? "?" : "",
                this.childJsonNode,
                this.childJsonNode.getNodeType().toString().toLowerCase(Locale.ROOT)
            );
            this.contextStack.addError(error);
        }

        String textValue = this.childJsonNode.textValue();

        Enumeration enumeration = enumerationProperty.getType();
        ImmutableList<EnumerationLiteral> enumerationLiterals = enumeration.getEnumerationLiterals();
        if (!enumerationLiterals.asLazy().collect(EnumerationLiteral::getPrettyName).contains(textValue)) {
            ImmutableList<String> quotedPrettyNames = enumerationLiterals
                .collect(EnumerationLiteral::getPrettyName)
                .collect(each -> '"' + each + '"');

            String error = String.format(
                "Expected enumerated property with type '%s.%s: %s%s' but got %s with type '%s'. Expected one of %s.",
                enumerationProperty.getOwningClassifier().getName(),
                enumerationProperty.getName(),
                enumerationProperty.getType().getName(),
                enumerationProperty.isOptional() ? "?" : "",
                this.childJsonNode,
                this.childJsonNode.getNodeType().toString().toLowerCase(Locale.ROOT),
                quotedPrettyNames.makeString()
            );
            this.contextStack.addError(error);
        }
    }

    @Override
    public void visitAssociationEndSignature(AssociationEndSignature associationEndSignature) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitAssociationEndSignature() not implemented yet"
        );
    }

    @Override
    public void visitAssociationEnd(@Nonnull AssociationEnd associationEnd) {
        Multiplicity multiplicity = associationEnd.getMultiplicity();
        if (multiplicity.isToOne()) {
            var contextNode = new ContextNode(associationEnd);
            this.contextStack.runWithContext(contextNode, () -> {
                    var validator = new ObjectNodeTypeCheckingValidator(
                        this.contextStack,
                        associationEnd.getType(),
                        this.childJsonNode
                    );
                    validator.validateIncomingData();
                });
        } else if (multiplicity.isToMany()) {
            ArrayNodeTypeCheckingValidator jsonTypeCheckingValidator = new ArrayNodeTypeCheckingValidator(
                this.contextStack,
                associationEnd,
                associationEnd.getType(),
                this.childJsonNode
            );
            jsonTypeCheckingValidator.validateIncomingData();
        } else {
            throw new AssertionError(multiplicity);
        }
    }

    @Override
    public void visitParameterizedProperty(ParameterizedProperty parameterizedProperty) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitParameterizedProperty() not implemented yet"
        );
    }
}
