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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cool.klass.data.store.DataStore;
import cool.klass.deserializer.json.OperationMode;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.MapAdapter;

public class PersistentCreator extends PersistentSynchronizer {

    public PersistentCreator(@Nonnull MutationContext mutationContext, @Nonnull DataStore dataStore) {
        this(mutationContext, dataStore, false);
    }

    public PersistentCreator(
        @Nonnull MutationContext mutationContext,
        @Nonnull DataStore dataStore,
        boolean inTransaction
    ) {
        super(mutationContext, dataStore, inTransaction);
    }

    @Override
    protected boolean shouldWriteKey() {
        return true;
    }

    @Override
    protected boolean shouldWriteId() {
        return false;
    }

    @Override
    protected void synchronizeUpdatedDataTypeProperties(
        @Nonnull Klass klass,
        Object persistentInstance,
        boolean propertyMutationOccurred
    ) {
        this.synchronizeUpdatedDataTypeProperties(klass, persistentInstance);
    }

    @Override
    protected void validateSetIdDataTypeProperties(Klass klass, Object persistentInstance) {
        ImmutableList<DataTypeProperty> idProperties = klass.getDataTypeProperties().select(DataTypeProperty::isID);
        for (DataTypeProperty idProperty : idProperties) {
            Object id = this.dataStore.getDataTypeProperty(persistentInstance, idProperty);
            if (id.equals(0L) || id.equals(0)) {
                throw new IllegalStateException();
            }
        }
    }

    @Override
    protected void synchronizeCreatedDataTypeProperties(@Nonnull Klass klass, Object persistentInstance) {
        Optional<PrimitiveProperty> createdByProperty = klass.getCreatedByProperty();
        Optional<PrimitiveProperty> createdOnProperty = klass.getCreatedOnProperty();

        createdByProperty.ifPresent(primitiveProperty -> {
            Optional<String> optionalUserId = this.mutationContext.getUserId();
            String userId = optionalUserId.orElseThrow(() -> this.expectAuditProperty(primitiveProperty));
            if (!this.dataStore.setDataTypeProperty(persistentInstance, primitiveProperty, userId)) {
                String detailMessage = "Expected to set createdBy property: %s on %s to %s".formatted(
                    primitiveProperty,
                    persistentInstance,
                    userId
                );
                throw new AssertionError(detailMessage);
            }
        });

        createdOnProperty.ifPresent(primitiveProperty -> {
            Instant transactionTime = this.mutationContext.getTransactionTime();
            if (!this.dataStore.setDataTypeProperty(persistentInstance, primitiveProperty, transactionTime)) {
                String detailMessage = "Expected to set createdOn property: %s on %s to %s".formatted(
                    primitiveProperty,
                    persistentInstance,
                    transactionTime
                );
                throw new AssertionError(detailMessage);
            }
        });
    }

    private AssertionError expectAuditProperty(PrimitiveProperty primitiveProperty) {
        String message = String.format(
            "Mutation context has no userId, but found an audit property: '%s'",
            primitiveProperty
        );
        return new AssertionError(message);
    }

    @Override
    protected void handleVersion(@Nonnull AssociationEnd associationEnd, Object persistentInstance) {
        Object persistentChildInstance = this.dataStore.getToOne(persistentInstance, associationEnd);
        if (persistentChildInstance != null) {
            throw new AssertionError();
        }

        ImmutableMap<DataTypeProperty, Object> keys = this.getKeysFromPersistentInstance(
            persistentInstance,
            associationEnd.getOwningClassifier()
        );

        MutableMap<DataTypeProperty, Object> versionKeys = getVersionKeys(associationEnd, keys);

        this.insertVersion(persistentInstance, associationEnd, versionKeys);
    }

    @Nonnull
    private static MutableMap<DataTypeProperty, Object> getVersionKeys(
        @Nonnull AssociationEnd associationEnd,
        ImmutableMap<DataTypeProperty, Object> keys
    ) {
        MutableMap<DataTypeProperty, Object> versionKeys = MapAdapter.adapt(new LinkedHashMap<>());
        keys.forEachKeyValue((keyProperty, keyValue) -> {
            DataTypeProperty versionKeyProperty = getVersionKeyProperty(associationEnd, keyProperty);
            versionKeys.put(versionKeyProperty, keyValue);
        });
        return versionKeys;
    }

    @Nonnull
    private static DataTypeProperty getVersionKeyProperty(
        @Nonnull AssociationEnd associationEnd,
        DataTypeProperty keyProperty
    ) {
        DataTypeProperty versionKeyProperty = keyProperty
            .getForeignKeysMatchingThisKey()
            .get(associationEnd.getOpposite());

        if (versionKeyProperty.getOwningClassifier() == associationEnd.getType()) {
            return versionKeyProperty;
        }

        String message =
            "Expected version key property '%s' to be owned by '%s' but it's owned by '%s' instead.".formatted(
                versionKeyProperty,
                associationEnd.getType(),
                versionKeyProperty.getOwningClassifier()
            );
        throw new AssertionError(message);
    }

    @Override
    protected boolean handleToOneOutsideProjection(
        @Nonnull AssociationEnd associationEnd,
        @Nonnull Object persistentParentInstance,
        @Nonnull ObjectNode incomingParentNode,
        @Nonnull JsonNode incomingChildInstance
    ) {
        if (associationEnd.isOwned()) {
            throw new AssertionError(
                "Assumption is that all owned association ends are inside projection, all unowned are outside projection"
            );
        }

        if (incomingChildInstance.isMissingNode() || incomingChildInstance.isNull()) {
            return false;
        }

        Object childPersistentInstanceWithKey = this.findExistingChildPersistentInstance(
            persistentParentInstance,
            incomingChildInstance,
            associationEnd
        );
        if (childPersistentInstanceWithKey == null) {
            // It's possible to trigger this code path when there is an id pointing at missing reference data.
            // We also hit this path when including an embedded to-one object that's outside the projection, during creation.
            return false;
        }

        this.dataStore.setToOne(persistentParentInstance, associationEnd, childPersistentInstanceWithKey);
        // TODO: Return a flag indicating a mutation happened
        return true;
    }

    private void insertVersion(
        Object persistentInstance,
        @Nonnull AssociationEnd associationEnd,
        @Nonnull MapIterable<DataTypeProperty, Object> keys
    ) {
        Klass versionType = associationEnd.getType();
        Object versionInstance = this.dataStore.instantiate(versionType, keys);

        // TODO: Test where version association end and version property are not named "version"

        // Set version: 1
        DataTypeProperty versionProperty = versionType.getVersionNumberProperty().get();
        this.dataStore.setDataTypeProperty(versionInstance, versionProperty, 1);

        this.synchronizeCreatedDataTypeProperties(versionType, versionInstance);
        this.synchronizeUpdatedDataTypeProperties(versionType, versionInstance);

        // TODO: This is the backwards order from how I used to do it
        this.dataStore.setToOne(persistentInstance, associationEnd, versionInstance);
        this.dataStore.insert(versionInstance);
    }

    @Nonnull
    @Override
    protected PersistentSynchronizer determineNextMode(OperationMode nextMode) {
        if (nextMode == OperationMode.CREATE) {
            return new PersistentCreator(this.mutationContext, this.dataStore, this.inTransaction);
        }

        throw new AssertionError(nextMode);
    }
}
