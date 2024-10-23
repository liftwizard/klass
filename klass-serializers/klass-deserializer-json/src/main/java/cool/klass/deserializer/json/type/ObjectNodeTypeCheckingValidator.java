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

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cool.klass.deserializer.json.context.ContextNode;
import cool.klass.deserializer.json.context.ContextStack;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.NamedElement;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.property.PropertyVisitor;
import org.eclipse.collections.api.list.MutableList;

public final class ObjectNodeTypeCheckingValidator
{
    @Nonnull
    private final ContextStack contextStack;

    @Nonnull
    private final Klass klass;
    @Nonnull
    private final JsonNode jsonNode;

    public ObjectNodeTypeCheckingValidator(
            @Nonnull ContextStack contextStack,
            @Nonnull Klass klass,
            @Nonnull JsonNode jsonNode)
    {
        this.contextStack = Objects.requireNonNull(contextStack);
        this.klass = Objects.requireNonNull(klass);
        this.jsonNode = Objects.requireNonNull(jsonNode);
    }

    public static void validate(@Nonnull MutableList<String> errors, @Nonnull JsonNode jsonNode, @Nonnull Klass klass)
    {
        ContextStack contextStack = new ContextStack(errors, null);
        var contextNode = new ContextNode(klass);
        contextStack.runWithContext(contextNode, () ->
        {
            var validator = new ObjectNodeTypeCheckingValidator(contextStack, klass, jsonNode);
            validator.validateIncomingData();
        });
    }

    public void validateIncomingData()
    {
        if (this.jsonNode instanceof ObjectNode objectNode)
        {
            this.validateObjectNode(objectNode);
        }
        else
        {
            String error = String.format(
                    "Expected json object but value was %s: %s.",
                    this.jsonNode.getNodeType().toString().toLowerCase(),
                    this.jsonNode);
            this.contextStack.addError(error);
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
                    this.contextStack,
                    property,
                    childJsonNode);
            property.visit(visitor);
        });
    }

    private void handleMissingProperty(String childFieldName, JsonNode childJsonNode)
    {
        String error = String.format(
                "No such property '%s.%s' but got %s. Expected properties: %s.",
                this.klass,
                childFieldName,
                childJsonNode,
                this.klass.getProperties().reject(Property::isPrivate).collect(NamedElement::getName).makeString());
        this.contextStack.addError(error);
    }
}
