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
import cool.klass.deserializer.json.context.ContextNode;
import cool.klass.deserializer.json.context.ContextStack;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import org.eclipse.collections.api.list.ImmutableList;

// TODO 2024-10-20: Rename "*OutsideProjection()" methods to "*OutsideComposite"
// TODO 2024-10-20: Rename isInProjection to isInComposite
public class ObjectNodeRequiredDataTypePropertiesValidator
{
    @Nonnull
    private final ContextStack contextStack;

    @Nonnull
    private final Klass                    klass;
    @Nonnull
    private final ObjectNode               objectNode;
    @Nonnull
    private final OperationMode            operationMode;
    @Nonnull
    private final Optional<AssociationEnd> pathHere;
    private final boolean                  isRoot;
    private final boolean                  isInProjection;

    public ObjectNodeRequiredDataTypePropertiesValidator(
            @Nonnull ContextStack contextStack,
            @Nonnull Klass klass,
            @Nonnull ObjectNode objectNode,
            @Nonnull OperationMode operationMode,
            @Nonnull Optional<AssociationEnd> pathHere,
            boolean isRoot,
            boolean isInProjection)
    {
        this.contextStack  = Objects.requireNonNull(contextStack);
        this.klass         = Objects.requireNonNull(klass);
        this.objectNode    = Objects.requireNonNull(objectNode);
        this.operationMode = Objects.requireNonNull(operationMode);
        this.pathHere      = Objects.requireNonNull(pathHere);
        this.isRoot        = isRoot;
        this.isInProjection = isInProjection;
    }

    public void handleDataTypeProperties()
    {
        ImmutableList<DataTypeProperty> dataTypeProperties = this.klass.getDataTypeProperties();
        for (DataTypeProperty dataTypeProperty : dataTypeProperties)
        {
            var contextNode = new ContextNode(dataTypeProperty);
            this.contextStack.runWithContext(contextNode, () -> this.handleDataTypeProperty(dataTypeProperty));
        }
    }

    private void handleDataTypeProperty(@Nonnull DataTypeProperty dataTypeProperty)
    {
        if (dataTypeProperty.isID())
        {
            this.handleIdProperty(dataTypeProperty);
        }
        else if (dataTypeProperty.isKey())
        {
            this.handleKeyProperty(dataTypeProperty);
        }
        else if (dataTypeProperty.isCreatedBy() || dataTypeProperty.isLastUpdatedBy())
        {
            Severity severity = dataTypeProperty.isPrivate() ? Severity.ERROR : Severity.WARNING;
            this.handleIfPresent(dataTypeProperty, "audit", severity);
        }
        else if (dataTypeProperty.isCreatedOn())
        {
            this.handleCreatedOnProperty(dataTypeProperty);
        }
        else if (dataTypeProperty.isForeignKey())
        {
            Severity severity = dataTypeProperty.isPrivate() ? Severity.ERROR : Severity.WARNING;
            this.handleIfPresent(dataTypeProperty, "foreign key", severity);
        }
        else if (dataTypeProperty.isDerived())
        {
            this.handleIfPresent(dataTypeProperty, "derived", Severity.WARNING);
        }
        else if (dataTypeProperty.getType() == PrimitiveType.TEMPORAL_RANGE)
        {
            this.handleIfPresent(dataTypeProperty, "temporal range", Severity.ERROR);
        }
        else if (dataTypeProperty.getType() == PrimitiveType.TEMPORAL_INSTANT && this.isInProjection)
        {
            if (this.operationMode == OperationMode.CREATE && this.isInProjection)
            {
                this.handleIfPresent(dataTypeProperty, "temporal", Severity.WARNING);
            }
        }
        else if (dataTypeProperty.isPrivate())
        {
            this.handleIfPresent(dataTypeProperty, "private", Severity.ERROR);
        }
        else if (dataTypeProperty.isDerived())
        {
            this.handleIfPresent(dataTypeProperty, "derived", Severity.WARNING);
        }
        else if (dataTypeProperty.isVersion())
        {
            this.handleVersionProperty(dataTypeProperty);
        }
        else if (dataTypeProperty.isPrivate())
        {
            this.handleIfPresent(dataTypeProperty, "private", Severity.ERROR);
        }
        else
        {
            this.handlePlainProperty(dataTypeProperty);
        }
    }

    private void handleIdProperty(@Nonnull DataTypeProperty dataTypeProperty)
    {
        if (this.operationMode == OperationMode.CREATE)
        {
            return;
        }

        if (this.operationMode == OperationMode.REPLACE && this.isRoot)
        {
            return;
        }

        if (this.pathHere.isPresent() && this.pathHere.get().getMultiplicity() == Multiplicity.ONE_TO_ONE)
        {
            JsonNode jsonNode = this.objectNode.path(dataTypeProperty.getName());
            if (jsonNode.isMissingNode() || jsonNode.isNull())
            {
                String error = String.format(
                        "Expected value for required id property '%s.%s: %s%s' but value was %s.",
                        dataTypeProperty.getOwningClassifier().getName(),
                        dataTypeProperty.getName(),
                        dataTypeProperty.getType(),
                        dataTypeProperty.isOptional() ? "?" : "",
                        jsonNode.getNodeType().toString().toLowerCase());
                this.contextStack.addError(error);
            }
        }
    }

    private void handleKeyProperty(@Nonnull DataTypeProperty dataTypeProperty)
    {
        // TODO: Handle foreign key properties that are also key properties at the root
        if (this.isForeignKeyMatchingKeyOnPath(dataTypeProperty))
        {
            this.handleIfPresent(dataTypeProperty, "foreign key matching key on path", Severity.WARNING);
            return;
        }

        // TODO: Exclude path here
        if (this.isForeignKeyMatchingRequiredNested(dataTypeProperty))
        {
            this.handleIfPresent(dataTypeProperty, "foreign key matching key of required nested object", Severity.WARNING);
            return;
        }

        if (this.isRoot)
        {
            this.handleIfPresent(dataTypeProperty, "root key", Severity.WARNING);
            return;
        }

        if (dataTypeProperty.isForeignKeyWithOpposite())
        {
            this.handleIfPresent(dataTypeProperty, "foreign key", Severity.WARNING);
            return;
        }

        if (this.pathHere.isPresent() && dataTypeProperty.isForeignKeyMatchingKeyOnPath(this.pathHere.get()))
        {
            this.handleIfPresent(this.pathHere.get(), dataTypeProperty.getName());
            return;
        }

        if (this.operationMode == OperationMode.PATCH)
        {
            return;
        }

        JsonNode jsonNode = this.objectNode.path(dataTypeProperty.getName());
        if (jsonNode.isMissingNode() || jsonNode.isNull())
        {
            String error = String.format(
                    "Expected value for key property '%s.%s: %s%s' but value was %s.",
                    dataTypeProperty.getOwningClassifier().getName(),
                    dataTypeProperty.getName(),
                    dataTypeProperty.getType(),
                    dataTypeProperty.isOptional() ? "?" : "",
                    jsonNode.getNodeType().toString().toLowerCase());
            this.contextStack.addError(error);
        }
    }

    // TODO 2024-10-20: Extract to super-class
    private void handleIfPresent(@Nonnull AssociationEnd property, String propertyKind)
    {
        JsonNode jsonNode = this.objectNode.path(property.getName());
        if (jsonNode.isMissingNode())
        {
            return;
        }

        String jsonNodeString = jsonNode.isNull() ? "" : ": " + jsonNode;
        String warning = String.format(
                "Didn't expect to receive value for %s association end '%s.%s: %s[%s]' but value was %s%s.",
                propertyKind,
                property.getOwningClassifier().getName(),
                property.getName(),
                property.getType(),
                property.getMultiplicity().getPrettyName(),
                jsonNode.getNodeType().toString().toLowerCase(),
                jsonNodeString);
        this.contextStack.addWarning(warning);
    }

    private boolean isForeignKeyMatchingRequiredNested(DataTypeProperty dataTypeProperty)
    {
        // TODO: Exclude path here
        return dataTypeProperty.getKeysMatchingThisForeignKey().keysView().anySatisfy(this::isToOneRequired);
    }

    private void handleCreatedOnProperty(@Nonnull DataTypeProperty dataTypeProperty)
    {
        if (this.isInProjection && this.operationMode == OperationMode.CREATE)
        {
            this.handleIfPresent(dataTypeProperty, "audit", Severity.WARNING);
        }
        else if (this.isInProjection && (this.operationMode == OperationMode.REPLACE || this.operationMode == OperationMode.PATCH))
        {
            // Validate in Incoming(Create|Update)DataModelValidator
        }
        else if (!this.isInProjection)
        {
            // Validate in Incoming(Create|Update)DataModelValidator
        }
        else
        {
            throw new AssertionError();
        }
    }

    private boolean isToOneRequired(AssociationEnd associationEnd)
    {
        Multiplicity multiplicity = associationEnd.getMultiplicity();
        return multiplicity.isToOne() && multiplicity.isRequired();
    }

    private boolean isForeignKeyMatchingKeyOnPath(DataTypeProperty dataTypeProperty)
    {
        return this.pathHere.map(dataTypeProperty::isForeignKeyMatchingKeyOnPath).orElse(false);
    }

    private enum Severity
    {
        ERROR,
        WARNING,
    }

    private void handleIfPresent(
            @Nonnull DataTypeProperty property,
            String propertyKind,
            Severity severity)
    {
        JsonNode jsonNode = this.objectNode.path(property.getName());
        if (jsonNode.isMissingNode())
        {
            return;
        }

        String jsonNodeString = jsonNode.isNull() ? "" : ": " + jsonNode;
        String annotation = String.format(
                "Didn't expect to receive value for %s property '%s.%s: %s%s' but value was %s%s.",
                propertyKind,
                property.getOwningClassifier().getName(),
                property.getName(),
                property.getType(),
                property.isOptional() ? "?" : "",
                jsonNode.getNodeType().toString().toLowerCase(),
                jsonNodeString);
        switch (severity)
        {
            case ERROR -> this.contextStack.addError(annotation);
            case WARNING -> this.contextStack.addWarning(annotation);
        }
    }

    private void handlePlainProperty(@Nonnull DataTypeProperty property)
    {
        if (!property.isRequired())
        {
            return;
        }

        if (!this.isInProjection)
        {
            this.handleIfPresent(property, "outside projection", Severity.WARNING);
            return;
        }

        if (this.operationMode == OperationMode.PATCH)
        {
            return;
        }

        JsonNode jsonNode = this.objectNode.path(property.getName());
        if (jsonNode.isMissingNode() || jsonNode.isNull())
        {
            String error = String.format(
                    "Expected value for required property '%s.%s: %s%s' but value was %s.",
                    property.getOwningClassifier().getName(),
                    property.getName(),
                    property.getType(),
                    property.isOptional() ? "?" : "",
                    jsonNode.getNodeType().toString().toLowerCase());
            this.contextStack.addError(error);
        }
    }

    private void handleVersionProperty(DataTypeProperty property)
    {
        JsonNode jsonNode = this.objectNode.path(property.getName());
        if (jsonNode.isMissingNode() || jsonNode.isNull())
        {
            return;
        }

        if (!jsonNode.isIntegralNumber())
        {
            return;
        }

        if (jsonNode.asInt() != 1)
        {
            String error = String.format(
                    "Expected value for version property '%s.%s: %s%s' to be 1 during initial creation but value was %s.",
                    property.getOwningClassifier().getName(),
                    property.getName(),
                    property.getType(),
                    property.isOptional() ? "?" : "",
                    jsonNode.getNodeType().toString().toLowerCase());
            this.contextStack.addError(error);
        }
    }
}
