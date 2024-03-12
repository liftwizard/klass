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

package cool.klass.reladomo.persistent.writer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cool.klass.data.store.DataStore;
import cool.klass.deserializer.json.AssertValuesMatchPropertyVisitor;
import cool.klass.deserializer.json.JsonDataTypeValueVisitor;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.PropertyVisitor;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.api.map.OrderedMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Stacks;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;
import org.eclipse.collections.impl.utility.ListIterate;

public class IncomingUpdateDataModelValidator
{
    @Nonnull
    protected final DataStore                dataStore;
    @Nonnull
    protected final Klass                    userKlass;
    @Nonnull
    protected final Klass                    klass;
    @Nonnull
    protected final MutationContext          mutationContext;
    @Nonnull
    protected final Object                   persistentInstance;
    @Nonnull
    protected final ObjectNode               objectNode;
    @Nonnull
    protected final MutableList<String>      errors;
    @Nonnull
    protected final MutableList<String>      warnings;
    @Nonnull
    protected final MutableStack<String>     contextStack;
    @Nonnull
    protected final Optional<AssociationEnd> pathHere;
    protected final boolean                  isRoot;
    protected final boolean                  isInProjection;

    public IncomingUpdateDataModelValidator(
            @Nonnull DataStore dataStore,
            @Nonnull Klass userKlass,
            @Nonnull Klass klass,
            @Nonnull MutationContext mutationContext,
            @Nonnull Object persistentInstance,
            @Nonnull ObjectNode objectNode,
            @Nonnull MutableList<String> errors,
            @Nonnull MutableList<String> warnings,
            @Nonnull MutableStack<String> contextStack,
            @Nonnull Optional<AssociationEnd> pathHere,
            boolean isRoot,
            boolean isInProjection)
    {
        this.dataStore          = Objects.requireNonNull(dataStore);
        this.userKlass          = Objects.requireNonNull(userKlass);
        this.klass              = Objects.requireNonNull(klass);
        this.mutationContext    = Objects.requireNonNull(mutationContext);
        this.persistentInstance = Objects.requireNonNull(persistentInstance);
        this.objectNode         = Objects.requireNonNull(objectNode);
        this.errors             = Objects.requireNonNull(errors);
        this.warnings           = Objects.requireNonNull(warnings);
        this.contextStack       = Objects.requireNonNull(contextStack);
        this.pathHere           = Objects.requireNonNull(pathHere);
        this.isRoot             = isRoot;
        this.isInProjection     = isInProjection;
    }

    public static void validate(
            @Nonnull DataStore dataStore,
            @Nonnull Klass userKlass,
            @Nonnull Klass klass,
            @Nonnull MutationContext mutationContext,
            @Nonnull Object persistentInstance,
            @Nonnull ObjectNode objectNode,
            @Nonnull MutableList<String> errors,
            @Nonnull MutableList<String> warnings)
    {
        IncomingUpdateDataModelValidator validator = new IncomingUpdateDataModelValidator(
                dataStore,
                userKlass,
                klass,
                mutationContext,
                persistentInstance,
                objectNode,
                errors,
                warnings,
                Stacks.mutable.empty(),
                Optional.empty(),
                true,
                true);
        validator.validate();
    }

    public void validate()
    {
        if (this.isRoot)
        {
            this.contextStack.push(this.klass.getName());
        }
        try
        {
            this.handleDataTypePropertiesInsideProjection();
            this.handleAssociationEnds();
        }
        finally
        {
            if (this.isRoot)
            {
                this.contextStack.pop();
            }
        }
    }

    // region DataTypeProperties
    private void handleDataTypePropertiesInsideProjection()
    {
        ImmutableList<DataTypeProperty> dataTypeProperties = this.klass.getDataTypeProperties();
        for (DataTypeProperty dataTypeProperty : dataTypeProperties)
        {
            this.handleDataTypePropertyInsideProjection(dataTypeProperty);
        }
    }

    private void handleDataTypePropertyInsideProjection(@Nonnull DataTypeProperty dataTypeProperty)
    {
        if (dataTypeProperty.isKey())
        {
            this.handleKeyProperty(dataTypeProperty);
        }
        else if (dataTypeProperty.isTemporalInstant())
        {
            this.checkPropertyMatchesIfPresent(dataTypeProperty, "temporal");
        }
        else if (dataTypeProperty.isCreatedBy() || dataTypeProperty.isLastUpdatedBy())
        {
            if (dataTypeProperty.isForeignKey())
            {
                return;
            }
            this.handleAuditProperty(dataTypeProperty);
        }
        else if (dataTypeProperty.isCreatedOn())
        {
            this.checkPropertyMatchesIfPresent(dataTypeProperty, "created on");
        }
        else if (dataTypeProperty.isVersion())
        {
            this.checkPropertyMatchesIfPresent(dataTypeProperty, "version");
        }
        else if (dataTypeProperty.isFinal())
        {
            this.checkPropertyMatchesIfPresent(dataTypeProperty, "final");
        }
    }

    private void handleKeyProperty(@Nonnull DataTypeProperty dataTypeProperty)
    {
        this.contextStack.push(dataTypeProperty.getName());

        try
        {
            JsonNode jsonDataTypeValue = this.objectNode.path(dataTypeProperty.getName());
            if (jsonDataTypeValue.isMissingNode())
            {
                return;
            }

            ImmutableMap<DataTypeProperty, Object> propertyDataFromUrl = this.mutationContext.getPropertyDataFromUrl();
            if (!propertyDataFromUrl.containsKey(dataTypeProperty))
            {
                return;
            }

            Object propertyData = propertyDataFromUrl.get(dataTypeProperty);

            PropertyVisitor visitor = new AssertValuesMatchPropertyVisitor(
                    jsonDataTypeValue,
                    propertyData,
                    "url parameter key",
                    this.contextStack,
                    "Error",
                    this.errors);
            dataTypeProperty.visit(visitor);
        }
        finally
        {
            this.contextStack.pop();
        }
    }

    private void handleAuditProperty(@Nonnull DataTypeProperty dataTypeProperty)
    {
        this.checkPropertyMatchesIfPresent(dataTypeProperty, "user id");
    }

    private void checkPropertyMatchesIfPresent(
            @Nonnull DataTypeProperty dataTypeProperty,
            @Nonnull String propertyKind)
    {
        this.contextStack.push(dataTypeProperty.getName());

        try
        {
            this.checkPropertyMatches(dataTypeProperty, propertyKind);
        }
        finally
        {
            this.contextStack.pop();
        }
    }

    private void checkPropertyMatches(@Nonnull DataTypeProperty property, @Nonnull String propertyKind)
    {
        JsonNode jsonDataTypeValue = this.objectNode.path(property.getName());
        if (jsonDataTypeValue.isMissingNode())
        {
            return;
        }

        Object persistentValue = this.dataStore.getDataTypeProperty(this.persistentInstance, property);
        PropertyVisitor visitor = new AssertValuesMatchPropertyVisitor(
                jsonDataTypeValue,
                persistentValue,
                propertyKind,
                this.contextStack,
                "Error",
                this.errors);
        property.visit(visitor);
    }

    public String getContextString()
    {
        return this.contextStack
                .toList()
                .asReversed()
                .makeString(".");
    }
    // endregion

    // region AssociationEnds
    private void handleAssociationEnds()
    {
        for (AssociationEnd associationEnd : this.klass.getAssociationEnds())
        {
            this.handleAssociationEnd(associationEnd);
        }
    }

    private void handleAssociationEnd(@Nonnull AssociationEnd associationEnd)
    {
        if (this.isBackward(associationEnd))
        {
            return;
        }

        if (associationEnd.isVersion())
        {
            this.handleVersionAssociationEnd(associationEnd);
            return;
        }
        if (associationEnd.isCreatedBy() || associationEnd.isLastUpdatedBy())
        {
            this.handleAuditByAssociationEnd(associationEnd);
            return;
        }

        if (associationEnd.isOwned())
        {
            this.handleOwnedAssociationEnd(associationEnd);
        }
        else
        {
            this.handleOutsideProjectionReferenceProperty(associationEnd);
        }
    }

    private void handleAssociationEnd(
            @Nonnull AssociationEnd associationEnd,
            @Nonnull ObjectNode objectNode,
            @Nonnull Object persistentInstance)
    {
        IncomingUpdateDataModelValidator validator = new IncomingUpdateDataModelValidator(
                this.dataStore,
                this.userKlass,
                associationEnd.getType(),
                this.mutationContext,
                persistentInstance,
                objectNode,
                this.errors,
                this.warnings,
                // Yeah?
                this.contextStack,
                Optional.of(associationEnd),
                false,
                this.isInProjection && associationEnd.isOwned());
        validator.validate();
    }

    private void handleAuditByAssociationEnd(@Nonnull AssociationEnd associationEnd)
    {
        JsonNode jsonNode = this.objectNode.path(associationEnd.getName());
        if (jsonNode.isMissingNode() || jsonNode.isNull())
        {
            return;
        }

        String associationEndName = associationEnd.getName();
        this.contextStack.push(associationEndName);

        Optional<String> userId = this.mutationContext.getUserId();
        if (userId.isEmpty())
        {
            return;
        }

        DataTypeProperty userIdProperty = this.userKlass.getKeyProperties().getOnly();
        ImmutableMap<DataTypeProperty, Object> userKeys = Maps.immutable.with(
                userIdProperty,
                userId.get());
        Object userPersistentInstance = this.dataStore.findByKey(this.userKlass, userKeys);

        if (userPersistentInstance == null)
        {
            String error = String.format(
                    "Error at %s. Couldn't find user with key %s.",
                    this.getContextString(),
                    userKeys);
            this.errors.add(error);
            return;
        }

        try
        {
            // TODO: Support a IncomingLastUpdatedByDataModelValidator which allows the current user to be substituted in for lastUpdatedBy.
            IncomingCreateDataModelValidator validator = new IncomingCreateDataModelValidator(
                    this.dataStore,
                    this.userKlass,
                    associationEnd.getType(),
                    this.mutationContext,
                    (ObjectNode) jsonNode,
                    this.errors,
                    this.warnings,
                    this.contextStack,
                    Optional.of(associationEnd),
                    false);
            validator.validate();
        }
        finally
        {
            this.contextStack.pop();
        }
    }

    private void handleOwnedAssociationEnd(@Nonnull AssociationEnd associationEnd)
    {
        Multiplicity multiplicity = associationEnd.getMultiplicity();

        if (multiplicity.isToOne())
        {
            this.handleOwnedToOne(associationEnd);
        }
        else
        {
            this.handleOwnedToMany(associationEnd);
        }
    }

    private void handleOutsideProjectionReferenceProperty(@Nonnull AssociationEnd associationEnd)
    {
        Multiplicity multiplicity  = associationEnd.getMultiplicity();
        JsonNode     childJsonNode = this.objectNode.path(associationEnd.getName());

        if (childJsonNode.isMissingNode() || childJsonNode.isNull())
        {
            return;
        }

        if (multiplicity.isToOne())
        {
            this.handleToOneOutsideProjection(associationEnd, childJsonNode);
        }
        else
        {
            this.handleToManyOutsideProjection(associationEnd, childJsonNode);
        }
    }

    private void handleVersionAssociationEnd(@Nonnull AssociationEnd associationEnd)
    {
        this.contextStack.push(associationEnd.getName());
        try
        {
            JsonNode jsonNode = this.objectNode.path(associationEnd.getName());

            if (this.persistentInstance == null)
            {
                this.handleWarnIfPresent(associationEnd, "version");
                return;
            }

            if (jsonNode.isMissingNode() || jsonNode.isNull())
            {
                if (this.isInProjection && this.klass.getKeyProperties().noneSatisfy(DataTypeProperty::isID))
                {
                    this.handleErrorIfAbsent(associationEnd, "version");
                }
                return;
            }

            Object childPersistentInstance = this.dataStore.getToOne(
                    this.persistentInstance,
                    associationEnd);
            if (!(jsonNode instanceof ObjectNode objectNode))
            {
                return;
            }

            this.handleAssociationEnd(associationEnd, objectNode, childPersistentInstance);
        }
        finally
        {
            this.contextStack.pop();
        }
    }

    private void handleErrorIfAbsent(@Nonnull AssociationEnd associationEnd, String propertyKind)
    {
        JsonNode jsonNode = this.objectNode.path(associationEnd.getName());
        if (!jsonNode.isMissingNode() && !jsonNode.isNull())
        {
            return;
        }

        String error = String.format(
                "Error at %s. Expected value for %s property '%s.%s: %s[%s]' but value was %s.",
                this.getContextString(),
                propertyKind,
                associationEnd.getOwningClassifier().getName(),
                associationEnd.getName(),
                associationEnd.getType(),
                associationEnd.getMultiplicity().getPrettyName(),
                jsonNode.getNodeType().toString().toLowerCase());
        this.errors.add(error);
    }

    private void handleWarnIfPresent(@Nonnull AssociationEnd property, String propertyKind)
    {
        JsonNode jsonNode = this.objectNode.path(property.getName());
        if (jsonNode.isMissingNode())
        {
            return;
        }

        String jsonNodeString = jsonNode.isNull() ? "" : ": " + jsonNode;
        String warning = String.format(
                "Warning at %s. Didn't expect to receive value for %s association end '%s.%s: %s[%s]' but value was %s%s.",
                this.getContextString(),
                propertyKind,
                property.getOwningClassifier().getName(),
                property.getName(),
                property.getType(),
                property.getMultiplicity().getPrettyName(),
                jsonNode.getNodeType().toString().toLowerCase(),
                jsonNodeString);
        this.warnings.add(warning);
    }

    private boolean isBackward(@Nonnull AssociationEnd associationEnd)
    {
        return this.pathHere.equals(Optional.of(associationEnd.getOpposite()));
    }

    public void handleOwnedToOne(@Nonnull AssociationEnd associationEnd)
    {
        JsonNode childJsonNode = this.objectNode.path(associationEnd.getName());

        String associationEndName = associationEnd.getName();
        this.contextStack.push(associationEndName);

        try
        {
            if (childJsonNode instanceof ObjectNode)
            {
                Object childPersistentInstance = this.dataStore.getToOne(
                        this.persistentInstance,
                        associationEnd);
                if (childPersistentInstance == null)
                {
                    // TODO: This is a workaround for a bug and should be revisited to see if it still applies in the happy path. The bug started with an association between Owner[1..1] and Details[1..1] owned. The database wound up corrupted with no row or Details. Here we're trying to validate the incoming Details json against the childPersistentInstance which is null. It's possible that this situation comes up with a nullable Details object as well.
                    return;
                }
                this.handleAssociationEnd(associationEnd, (ObjectNode) childJsonNode, childPersistentInstance);
            }
        }
        finally
        {
            this.contextStack.pop();
        }
    }

    public void handleOwnedToMany(@Nonnull AssociationEnd associationEnd)
    {
        JsonNode incomingChildInstances = this.objectNode.path(associationEnd.getName());

        // TODO: Figure out how to recurse without checking key
        /*
        ImmutableList<JsonNode> incomingInstancesForInsert = this.filterIncomingInstancesForInsert(
                incomingChildInstances,
                associationEnd);
        */

        ImmutableList<JsonNode> incomingInstancesForUpdate = this.filterIncomingInstancesForUpdate(
                incomingChildInstances,
                associationEnd);

        MapIterable<ImmutableList<Object>, JsonNode> incomingChildInstancesByKey = this.indexIncomingJsonInstances(
                incomingInstancesForUpdate,
                associationEnd);

        MapIterable<ImmutableList<Object>, Object> persistentChildInstancesByKey =
                this.getPersistentChildInstancesByKey(
                        incomingChildInstancesByKey,
                        associationEnd);

        for (int index = 0; index < incomingChildInstances.size(); index++)
        {
            String contextString = String.format(
                    "%s[%d]",
                    associationEnd.getName(),
                    index);

            this.contextStack.push(contextString);

            try
            {
                JsonNode childJsonNode = incomingChildInstances.path(index);
                if (this.jsonNodeNeedsIdInferredOnInsert(childJsonNode, associationEnd))
                {
                    continue;
                }

                ImmutableList<Object> keysFromJsonNode        = this.getKeysFromJsonNode(childJsonNode, associationEnd, this.objectNode);
                Object                childPersistentInstance = persistentChildInstancesByKey.get(keysFromJsonNode);
                if (childPersistentInstance == null)
                {
                    // recurse in create mode
                    IncomingCreateDataModelValidator validator = new IncomingCreateDataModelValidator(
                            this.dataStore,
                            this.userKlass,
                            associationEnd.getType(),
                            this.mutationContext,
                            (ObjectNode) childJsonNode,
                            this.errors,
                            this.warnings,
                            this.contextStack,
                            Optional.of(associationEnd),
                            false);
                    validator.validate();
                }
                else
                {
                    this.handleAssociationEnd(associationEnd, (ObjectNode) childJsonNode, childPersistentInstance);
                }
            }
            finally
            {
                this.contextStack.pop();
            }
        }
    }

    public void handleToOneOutsideProjection(
            @Nonnull AssociationEnd associationEnd,
            JsonNode jsonNode)
    {
        this.contextStack.push(associationEnd.getName());
        try
        {
            if (jsonNode instanceof ObjectNode objectNode)
            {
                Object childPersistentInstance = this.dataStore.getToOne(
                        this.persistentInstance,
                        associationEnd);
                if (childPersistentInstance == null)
                {
                    // TODO: This is a workaround for a bug and should be revisited to see if it still applies in the happy path. The bug started with an association between Owner[1..1] and Details[1..1] owned. The database wound up corrupted with no row or Details. Here we're trying to validate the incoming Details json against the childPersistentInstance which is null. It's possible that this situation comes up with a nullable Details object as well.
                    return;
                }
                this.handleAssociationEnd(associationEnd, objectNode, childPersistentInstance);
            }
        }
        finally
        {
            this.contextStack.pop();
        }
    }

    public void handleToManyOutsideProjection(
            @Nonnull AssociationEnd associationEnd,
            JsonNode incomingChildInstances)
    {
        if (!(incomingChildInstances instanceof ArrayNode))
        {
            this.emitNonArrayError(associationEnd, incomingChildInstances);
            return;
        }

        /*
        ImmutableList<JsonNode> incomingInstancesForInsert = this.filterIncomingInstancesForInsert(
                incomingChildInstances,
                associationEnd);
        */

        // TODO: Figure out how to recurse without checking key
        ImmutableList<JsonNode> incomingInstancesForUpdate = this.filterIncomingInstancesForUpdate(
                incomingChildInstances,
                associationEnd);

        MapIterable<ImmutableList<Object>, JsonNode> incomingChildInstancesByKey = this.indexIncomingJsonInstances(
                incomingInstancesForUpdate,
                associationEnd);

        MapIterable<ImmutableList<Object>, Object> persistentChildInstancesByKey =
                this.getPersistentChildInstancesByKey(
                        incomingChildInstancesByKey,
                        associationEnd);

        for (int index = 0; index < incomingChildInstances.size(); index++)
        {
            String contextString = String.format(
                    "%s[%d]",
                    associationEnd.getName(),
                    index);
            this.contextStack.push(contextString);

            try
            {
                JsonNode childJsonNode = incomingChildInstances.path(index);
                if (this.jsonNodeNeedsIdInferredOnInsert(childJsonNode, associationEnd))
                {
                    continue;
                }

                ImmutableList<Object> keysFromJsonNode        = this.getKeysFromJsonNode(childJsonNode, associationEnd, this.objectNode);
                Object                childPersistentInstance = persistentChildInstancesByKey.get(keysFromJsonNode);
                if (childPersistentInstance == null)
                {
                    // recurse in create mode
                    IncomingCreateDataModelValidator validator = new IncomingCreateDataModelValidator(
                            this.dataStore,
                            this.userKlass,
                            associationEnd.getType(),
                            this.mutationContext,
                            (ObjectNode) childJsonNode,
                            this.errors,
                            this.warnings,
                            this.contextStack,
                            Optional.of(associationEnd),
                            false);
                    validator.validate();
                }
                else
                {
                    this.handleAssociationEnd(associationEnd, (ObjectNode) childJsonNode, childPersistentInstance);
                }
            }
            finally
            {
                this.contextStack.pop();
            }
        }
    }

    @Nonnull
    private MapIterable<ImmutableList<Object>, Object> getPersistentChildInstancesByKey(
            @Nonnull MapIterable<ImmutableList<Object>, JsonNode> incomingChildInstancesByKey,
            @Nonnull AssociationEnd associationEnd)
    {
        List<Object> persistentChildInstances = this.dataStore.getToMany(this.persistentInstance, associationEnd);
        MutableList<Object> nonTerminatedPersistentChildInstances = ListAdapter.adapt(persistentChildInstances)
                .reject(persistentChildInstance -> this.needsTermination(
                        persistentChildInstance,
                        associationEnd.getType(),
                        incomingChildInstancesByKey));
        return this.indexPersistentInstances(
                nonTerminatedPersistentChildInstances,
                associationEnd.getType());
    }

    private ImmutableList<JsonNode> filterIncomingInstancesForUpdate(
            JsonNode incomingChildInstances, AssociationEnd associationEnd)
    {
        return Lists.immutable.withAll(incomingChildInstances)
                .rejectWith(
                        this::jsonNodeNeedsIdInferredOnInsert,
                        associationEnd);
    }

    private ImmutableList<JsonNode> filterIncomingInstancesForInsert(
            JsonNode incomingChildInstances,
            AssociationEnd associationEnd)
    {
        return Lists.immutable.withAll(incomingChildInstances)
                .selectWith(
                        this::jsonNodeNeedsIdInferredOnInsert,
                        associationEnd);
    }

    private boolean needsTermination(
            Object persistentChildInstance,
            @Nonnull Klass klass,
            @Nonnull MapIterable<ImmutableList<Object>, JsonNode> incomingChildInstancesByKey)
    {
        ImmutableList<Object> keys = this.getKeysFromPersistentInstance(persistentChildInstance, klass);
        return !incomingChildInstancesByKey.containsKey(keys);
    }

    protected ImmutableList<Object> getKeysFromPersistentInstance(Object persistentInstance, @Nonnull Klass klass)
    {
        return klass
                .getKeyProperties()
                .collect(keyProperty -> this.dataStore.getDataTypeProperty(persistentInstance, keyProperty));
    }

    private void emitNonArrayError(@Nonnull AssociationEnd associationEnd, @Nonnull JsonNode incomingChildInstances)
    {
        this.contextStack.push(associationEnd.getName());
        String error = String.format(
                "Error at %s. Expected json array but value was %s.",
                this.getContextString(),
                incomingChildInstances.getNodeType().toString().toLowerCase());
        this.errors.add(error);
        this.contextStack.pop();
    }

    @Nonnull
    private MapIterable<ImmutableList<Object>, Object> indexPersistentInstances(
            @Nonnull List<Object> persistentInstances,
            @Nonnull Klass klass)
    {
        MutableOrderedMap<ImmutableList<Object>, Object> result = ListIterate.groupByUniqueKey(
                persistentInstances,
                persistentInstance -> this.getKeysFromPersistentInstance(persistentInstance, klass),
                OrderedMapAdapter.adapt(new LinkedHashMap<>()));
        return result.asUnmodifiable();
    }

    @Nonnull
    private MapIterable<ImmutableList<Object>, JsonNode> indexIncomingJsonInstances(
            @Nonnull Iterable<JsonNode> incomingInstances,
            @Nonnull AssociationEnd associationEnd)
    {
        MutableOrderedMap<ImmutableList<Object>, JsonNode> result = OrderedMapAdapter.adapt(new LinkedHashMap<>());
        for (JsonNode incomingInstance : incomingInstances)
        {
            ImmutableList<Object> keys = this.getKeysFromJsonNode(
                    incomingInstance,
                    associationEnd,
                    this.objectNode);
            JsonNode duplicateJsonNode = result.put(keys, incomingInstance);
            if (duplicateJsonNode != null)
            {
                throw new AssertionError("TODO: Test an array of owned children with duplicates with the same key.");
            }
        }
        return result.asUnmodifiable();
    }

    private boolean jsonNodeNeedsIdInferredOnInsert(
            JsonNode jsonNode,
            @Nonnull AssociationEnd associationEnd)
    {
        return associationEnd
                .getType()
                .getKeyProperties()
                .allSatisfy(keyProperty -> this.jsonNodeNeedsIdInferredOnInsert(
                        keyProperty,
                        jsonNode,
                        associationEnd));
    }

    private boolean jsonNodeNeedsIdInferredOnInsert(
            @Nonnull DataTypeProperty keyProperty,
            JsonNode jsonNode,
            @Nonnull AssociationEnd associationEnd)
    {
        OrderedMap<AssociationEnd, DataTypeProperty> keyMatchingThisForeignKey =
                keyProperty.getKeysMatchingThisForeignKey();

        AssociationEnd opposite = associationEnd.getOpposite();

        if (keyMatchingThisForeignKey.containsKey(opposite))
        {
            return false;
        }

        if (keyMatchingThisForeignKey.notEmpty())
        {
            if (keyMatchingThisForeignKey.size() != 1)
            {
                throw new AssertionError();
            }

            return false;
        }

        return JsonDataTypeValueVisitor.dataTypePropertyIsNullInJson(
                keyProperty,
                (ObjectNode) jsonNode);
    }

    private ImmutableList<Object> getKeysFromJsonNode(
            @Nonnull JsonNode jsonNode,
            @Nonnull AssociationEnd associationEnd,
            @Nonnull JsonNode parentJsonNode)
    {
        Klass type = associationEnd.getType();
        ImmutableList<DataTypeProperty> keyProperties = type.getKeyProperties();
        return keyProperties
                .collect(keyProperty -> this.getKeyFromJsonNode(
                        keyProperty,
                        jsonNode,
                        associationEnd,
                        parentJsonNode));
    }

    private Object getKeyFromJsonNode(
            @Nonnull DataTypeProperty keyProperty,
            @Nonnull JsonNode jsonNode,
            @Nonnull AssociationEnd associationEnd,
            @Nonnull JsonNode parentJsonNode)
    {
        OrderedMap<AssociationEnd, DataTypeProperty> keysMatchingThisForeignKey =
                keyProperty.getKeysMatchingThisForeignKey();

        AssociationEnd opposite = associationEnd.getOpposite();

        DataTypeProperty oppositeForeignKeys = keysMatchingThisForeignKey.get(opposite);

        if (oppositeForeignKeys != null)
        {
            Object result = this.dataStore.getDataTypeProperty(
                    this.persistentInstance,
                    oppositeForeignKeys);
            return Objects.requireNonNull(result);
        }

        if (keysMatchingThisForeignKey.notEmpty())
        {
            if (keysMatchingThisForeignKey.size() != 1)
            {
                throw new AssertionError();
            }

            Pair<AssociationEnd, DataTypeProperty> pair = keysMatchingThisForeignKey.keyValuesView().getOnly();

            AssociationEnd foreignKeyAssociation     = pair.getOne();
            String         foreignKeyAssociationName = foreignKeyAssociation.getName();
            JsonNode childNode = jsonNode.path(foreignKeyAssociationName);
            if (childNode instanceof ObjectNode objectNode)
            {
                Object result = JsonDataTypeValueVisitor.extractDataTypePropertyFromJson(
                        pair.getTwo(),
                        objectNode);
                return Objects.requireNonNull(result);
            }
            if (childNode.isMissingNode())
            {
                Object result = JsonDataTypeValueVisitor.extractDataTypePropertyFromJson(
                        keyProperty,
                        (ObjectNode) jsonNode);
                return Objects.requireNonNull(result);
            }
            throw new AssertionError(childNode.getClass().getCanonicalName());
        }

        if (jsonNode instanceof ObjectNode objectNode)
        {
            Object result = JsonDataTypeValueVisitor.extractDataTypePropertyFromJson(
                    keyProperty,
                    objectNode);
            return Objects.requireNonNull(result);
        }

        throw new AssertionError();
    }
}
