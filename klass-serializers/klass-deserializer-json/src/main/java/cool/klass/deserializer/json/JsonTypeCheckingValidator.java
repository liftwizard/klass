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
import com.fasterxml.jackson.databind.node.ArrayNode;
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
    private final JsonNode jsonNode;
    @Nonnull
    private final Klass klass;
    @Nonnull
    private final Multiplicity expectedMultiplicity;
    @Nonnull
    private final Optional<NamedElement> context;

    @Nonnull
    private final MutableStack<String> contextStack;
    @Nonnull
    private final MutableList<String> errors;

    public JsonTypeCheckingValidator(
            @Nonnull JsonNode jsonNode,
            @Nonnull Optional<NamedElement> context,
            @Nonnull Klass klass,
            @Nonnull Multiplicity expectedMultiplicity,
            @Nonnull MutableStack<String> contextStack,
            @Nonnull MutableList<String> errors)
    {
        this.jsonNode = Objects.requireNonNull(jsonNode);
        this.context = Objects.requireNonNull(context);
        this.klass = Objects.requireNonNull(klass);
        this.expectedMultiplicity = Objects.requireNonNull(expectedMultiplicity);
        this.contextStack = Objects.requireNonNull(contextStack);
        this.errors = Objects.requireNonNull(errors);
    }

    public static void validate(
            @Nonnull JsonNode jsonNode,
            @Nonnull Klass klass,
            @Nonnull Multiplicity expectedMultiplicity,
            @Nonnull MutableList<String> errors)
    {
        JsonTypeCheckingValidator incomingDataValidator = new JsonTypeCheckingValidator(
                jsonNode,
                Optional.of(klass),
                klass,
                expectedMultiplicity,
                Stacks.mutable.empty(),
                errors);
        incomingDataValidator.validateIncomingData();
    }

    public void validateIncomingData()
    {
        if (this.expectedMultiplicity.isToOne())
        {
            this.context.ifPresent(presentContext -> this.contextStack.push(presentContext.getName()));

            try
            {
                if (this.jsonNode instanceof ObjectNode objectNode)
                {
                    this.validateObjectNode(objectNode);
                }
                else
                {
                    String error = String.format(
                            "Error at %s. Expected json object but value was %s: %s.",
                            this.getContextString(),
                            this.jsonNode.getNodeType().toString().toLowerCase(),
                            this.jsonNode);
                    this.errors.add(error);
                }
            }
            finally
            {
                this.context.ifPresent(presentContext -> this.contextStack.pop());
            }
        }
        else if (this.expectedMultiplicity.isToMany())
        {
            if (this.jsonNode instanceof ArrayNode arrayNode)
            {
                this.validateArrayNode(arrayNode);
            }
            else
            {
                String error = String.format(
                        "Error at %s. Expected json array but value was %s: %s.",
                        this.getContextString(),
                        this.jsonNode.getNodeType().toString().toLowerCase(),
                        this.jsonNode);
                this.errors.add(error);
            }
        }
        else
        {
            throw new AssertionError(this.expectedMultiplicity);
        }
    }

    private void validateObjectNode(@Nonnull ObjectNode objectNode)
    {
        objectNode.fields().forEachRemaining(entry ->
        {
            String childFieldName = entry.getKey();
            JsonNode childJsonNode = entry.getValue();
            Optional<Property> optionalProperty = this.klass.getPropertyByName(childFieldName);

            if (optionalProperty.isEmpty())
            {
                this.handleMissingProperty(childFieldName, childJsonNode);
                return;
            }

            if (childJsonNode.isNull())
            {
                return;
            }

            Property property = optionalProperty.get();
            PropertyVisitor visitor = new JsonTypeCheckingPropertyVisitor(
                    childFieldName,
                    childJsonNode);
            property.visit(visitor);
        });
    }

    private void handleMissingProperty(String childFieldName, JsonNode childJsonNode)
    {
        this.contextStack.push(childFieldName);

        try
        {
            String error = String.format(
                    "Error at %s. No such property '%s.%s' but got %s. Expected properties: %s.",
                    this.getContextString(),
                    this.klass,
                    childFieldName,
                    childJsonNode,
                    this.klass.getProperties().collect(NamedElement::getName).makeString());
            this.errors.add(error);
        }
        finally
        {
            this.contextStack.pop();
        }
    }

    private void validateArrayNode(@Nonnull ArrayNode arrayNode)
    {
        for (int index = 0; index < arrayNode.size(); index++)
        {
            String contextString = String.format("%s[%d]", this.context.get().getName(), index);
            this.contextStack.push(contextString);

            try
            {
                JsonNode childJsonNode = arrayNode.path(index);
                var jsonTypeCheckingValidator = new JsonTypeCheckingValidator(
                        childJsonNode,
                        Optional.empty(),
                        this.klass,
                        Multiplicity.ONE_TO_ONE,
                        this.contextStack,
                        this.errors);
                jsonTypeCheckingValidator.validateIncomingData();
            }
            finally
            {
                this.contextStack.pop();
            }
        }
    }

    private String getContextString()
    {
        return this.contextStack
                .toList()
                .asReversed()
                .makeString(".");
    }

    private final class JsonTypeCheckingPropertyVisitor
            implements PropertyVisitor
    {
        private final String childFieldName;
        private final JsonNode childJsonNode;

        private JsonTypeCheckingPropertyVisitor(
                @Nonnull String childFieldName,
                @Nonnull JsonNode childJsonNode)
        {
            this.childFieldName = Objects.requireNonNull(childFieldName);
            this.childJsonNode = Objects.requireNonNull(childJsonNode);
        }

        private void visitPropertyWithContext(@Nonnull Runnable runnable)
        {
            JsonTypeCheckingValidator.this.contextStack.push(this.childFieldName);

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
                    this.childJsonNode,
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
            if (!this.childJsonNode.isTextual())
            {
                String error = String.format(
                        "Error at %s. Expected enumerated property with type '%s.%s: %s%s' but got %s with type '%s'.",
                        JsonTypeCheckingValidator.this.getContextString(),
                        enumerationProperty.getOwningClassifier().getName(),
                        enumerationProperty.getName(),
                        enumerationProperty.getType().getName(),
                        enumerationProperty.isOptional() ? "?" : "",
                        this.childJsonNode,
                        this.childJsonNode.getNodeType().toString().toLowerCase());
                JsonTypeCheckingValidator.this.errors.add(error);
            }

            String textValue = this.childJsonNode.textValue();

            Enumeration enumeration = enumerationProperty.getType();
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
                        this.childJsonNode,
                        this.childJsonNode.getNodeType().toString().toLowerCase(),
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
            var jsonTypeCheckingValidator = new JsonTypeCheckingValidator(
                    this.childJsonNode,
                    Optional.of(associationEnd),
                    associationEnd.getType(),
                    multiplicity,
                    JsonTypeCheckingValidator.this.contextStack,
                    JsonTypeCheckingValidator.this.errors);
            jsonTypeCheckingValidator.validateIncomingData();
        }

        @Override
        public void visitParameterizedProperty(ParameterizedProperty parameterizedProperty)
        {
            throw new UnsupportedOperationException(this.getClass().getSimpleName()
                    + ".visitParameterizedProperty() not implemented yet");
        }
    }
}
