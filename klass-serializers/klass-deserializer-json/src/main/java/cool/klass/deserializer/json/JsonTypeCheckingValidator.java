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

package cool.klass.deserializer.json;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cool.klass.model.meta.domain.api.Enumeration;
import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.NamedElement;
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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.factory.Stacks;

public final class JsonTypeCheckingValidator
{
    @Nonnull
    private final ObjectNode objectNode;
    @Nonnull
    private final Klass      klass;

    @Nonnull
    private final MutableStack<String> contextStack = Stacks.mutable.empty();
    @Nonnull
    private final MutableList<String>  errors;

    public JsonTypeCheckingValidator(ObjectNode objectNode, Klass klass, MutableList<String> errors)
    {
        this.objectNode = Objects.requireNonNull(objectNode);
        this.klass = Objects.requireNonNull(klass);
        this.errors = Objects.requireNonNull(errors);
    }

    public static void validate(ObjectNode objectNode, Klass klass, MutableList<String> errors)
    {
        JsonTypeCheckingValidator incomingDataValidator = new JsonTypeCheckingValidator(
                objectNode,
                klass,
                errors);
        incomingDataValidator.validateIncomingData();
    }

    public void validateIncomingData()
    {
        this.contextStack.push(this.klass.toString());
        this.validateIncomingData(this.objectNode, this.klass);
    }

    private void validateIncomingData(@Nonnull ObjectNode objectNode, @Nonnull Klass klass)
    {
        objectNode.fields().forEachRemaining(entry ->
        {
            String             fieldName        = entry.getKey();
            JsonNode           jsonNode         = entry.getValue();
            Optional<Property> optionalProperty = klass.getPropertyByName(fieldName);

            if (optionalProperty.isEmpty())
            {
                this.handleMissingProperty(klass, fieldName, jsonNode);
                return;
            }

            if (jsonNode.isNull())
            {
                return;
            }

            Property property = optionalProperty.get();
            PropertyVisitor visitor = new JsonTypeCheckingPropertyVisitor(
                    fieldName,
                    jsonNode);
            property.visit(visitor);
        });
    }

    public void handleMissingProperty(@Nonnull Klass klass, String fieldName, JsonNode jsonNode)
    {
        String error = String.format(
                "Error at %s. No such property '%s.%s' but got %s. Expected properties: %s.",
                this.getContextString(),
                klass,
                fieldName,
                jsonNode,
                klass.getProperties().reject(Property::isPrivate).collect(NamedElement::getName).makeString());
        this.errors.add(error);
    }

    private String getContextString()
    {
        return this.contextStack
                .toList()
                .asReversed()
                .makeString(".");
    }

    private final class JsonTypeCheckingPropertyVisitor implements PropertyVisitor
    {
        private final String   fieldName;
        private final JsonNode jsonNode;

        private JsonTypeCheckingPropertyVisitor(
                String fieldName,
                JsonNode jsonNode)
        {
            this.fieldName = fieldName;
            this.jsonNode = jsonNode;
        }

        private void visitPropertyWithContext(@Nonnull Runnable runnable)
        {
            JsonTypeCheckingValidator.this.contextStack.push(this.fieldName);

            try
            {
                runnable.run();
            }
            finally
            {
                JsonTypeCheckingValidator.this.contextStack.pop();
            }
        }

        @Override
        public void visitPrimitiveProperty(@Nonnull PrimitiveProperty primitiveProperty)
        {
            this.visitPropertyWithContext(() -> this.handlePrimitiveProperty(primitiveProperty));
        }

        public void handlePrimitiveProperty(@Nonnull PrimitiveProperty primitiveProperty)
        {
            PrimitiveType primitiveType = primitiveProperty.getType();
            PrimitiveTypeVisitor visitor = new JsonTypeCheckingPrimitiveTypeVisitor(
                    primitiveProperty,
                    this.jsonNode,
                    JsonTypeCheckingValidator.this.contextStack,
                    JsonTypeCheckingValidator.this.errors);
            primitiveType.visit(visitor);
        }

        @Override
        public void visitEnumerationProperty(@Nonnull EnumerationProperty enumerationProperty)
        {
            this.visitPropertyWithContext(() -> this.handleEnumerationProperty(enumerationProperty));
        }

        public void handleEnumerationProperty(@Nonnull EnumerationProperty enumerationProperty)
        {
            if (!this.jsonNode.isTextual())
            {
                String error = String.format(
                        "Error at %s. Expected enumerated property with type '%s.%s: %s%s' but got %s with type '%s'.",
                        JsonTypeCheckingValidator.this.getContextString(),
                        enumerationProperty.getOwningClassifier().getName(),
                        enumerationProperty.getName(),
                        enumerationProperty.getType().getName(),
                        enumerationProperty.isOptional() ? "?" : "",
                        this.jsonNode,
                        this.jsonNode.getNodeType().toString().toLowerCase());
                JsonTypeCheckingValidator.this.errors.add(error);
            }

            String textValue = this.jsonNode.textValue();

            Enumeration                       enumeration         = enumerationProperty.getType();
            ImmutableList<EnumerationLiteral> enumerationLiterals = enumeration.getEnumerationLiterals();
            if (!enumerationLiterals.asLazy()
                    .collect(EnumerationLiteral::getPrettyName)
                    .contains(textValue))
            {
                ImmutableList<String> quotedPrettyNames = enumerationLiterals
                        .collect(EnumerationLiteral::getPrettyName)
                        .collect(each -> '"' + each + '"');

                String error = String.format(
                        "Error at %s. Expected enumerated property with type '%s.%s: %s%s' but got %s with type '%s'. Expected one of %s.",
                        JsonTypeCheckingValidator.this.getContextString(),
                        enumerationProperty.getOwningClassifier().getName(),
                        enumerationProperty.getName(),
                        enumerationProperty.getType().getName(),
                        enumerationProperty.isOptional() ? "?" : "",
                        this.jsonNode,
                        this.jsonNode.getNodeType().toString().toLowerCase(),
                        quotedPrettyNames.makeString());
                JsonTypeCheckingValidator.this.errors.add(error);
            }
        }

        @Override
        public void visitAssociationEndSignature(AssociationEndSignature associationEndSignature)
        {
            throw new UnsupportedOperationException(this.getClass().getSimpleName()
                    + ".visitAssociationEndSignature() not implemented yet");
        }

        @Override
        public void visitAssociationEnd(@Nonnull AssociationEnd associationEnd)
        {
            Multiplicity multiplicity = associationEnd.getMultiplicity();
            if (multiplicity.isToOne())
            {
                this.visitPropertyWithContext(() -> this.handleToOneAssociationEnd(associationEnd));
                return;
            }

            if (!this.jsonNode.isArray())
            {
                JsonTypeCheckingValidator.this.contextStack.push(this.fieldName);

                try
                {
                    String error = String.format(
                            "Error at %s. Expected json array but value was %s: %s.",
                            JsonTypeCheckingValidator.this.getContextString(),
                            this.jsonNode.getNodeType().toString().toLowerCase(),
                            this.jsonNode);
                    JsonTypeCheckingValidator.this.errors.add(error);
                }
                finally
                {
                    JsonTypeCheckingValidator.this.contextStack.pop();
                }
                return;
            }

            for (int index = 0; index < this.jsonNode.size(); index++)
            {
                String contextString = String.format("%s[%d]", this.fieldName, index);
                JsonTypeCheckingValidator.this.contextStack.push(contextString);

                try
                {
                    JsonNode jsonNode = this.jsonNode.path(index);
                    if (jsonNode instanceof ObjectNode objectNode)
                    {
                        JsonTypeCheckingValidator.this.validateIncomingData(
                                objectNode,
                                associationEnd.getType());
                    }
                    else
                    {
                        String error = String.format(
                                "Error at %s. Expected json object but value was %s: %s.",
                                JsonTypeCheckingValidator.this.getContextString(),
                                jsonNode.getNodeType().toString().toLowerCase(),
                                jsonNode);
                        JsonTypeCheckingValidator.this.errors.add(error);
                    }
                }
                finally
                {
                    JsonTypeCheckingValidator.this.contextStack.pop();
                }
            }
        }

        public void handleToOneAssociationEnd(@Nonnull AssociationEnd associationEnd)
        {
            if (this.jsonNode.isObject())
            {
                JsonTypeCheckingValidator.this.validateIncomingData(
                        (ObjectNode) this.jsonNode,
                        associationEnd.getType());
            }
            else
            {
                String error = String.format(
                        "Error at %s. Expected json object but value was %s: %s.",
                        JsonTypeCheckingValidator.this.getContextString(),
                        this.jsonNode.getNodeType().toString().toLowerCase(),
                        this.jsonNode);
                JsonTypeCheckingValidator.this.errors.add(error);
            }
        }

        @Override
        public void visitParameterizedProperty(ParameterizedProperty parameterizedProperty)
        {
            throw new UnsupportedOperationException(this.getClass().getSimpleName()
                    + ".visitParameterizedProperty() not implemented yet");
        }
    }
}
