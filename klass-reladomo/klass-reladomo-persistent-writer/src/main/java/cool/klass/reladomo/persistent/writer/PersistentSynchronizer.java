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

package cool.klass.reladomo.persistent.writer;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cool.klass.data.store.DataStore;
import cool.klass.deserializer.json.JsonDataTypeValueVisitor;
import cool.klass.deserializer.json.OperationMode;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.api.map.OrderedMap;
import org.eclipse.collections.api.partition.list.PartitionImmutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;
import org.eclipse.collections.impl.utility.ListIterate;

public abstract class PersistentSynchronizer {

	@Nonnull
	protected final MutationContext mutationContext;

	@Nonnull
	protected final DataStore dataStore;

	// TODO: Consider getting rid of this, it's just for assertions
	protected boolean inTransaction;

	protected PersistentSynchronizer(
		@Nonnull MutationContext mutationContext,
		@Nonnull DataStore dataStore,
		boolean inTransaction
	) {
		this.mutationContext = Objects.requireNonNull(mutationContext);
		this.dataStore = Objects.requireNonNull(dataStore);
		this.inTransaction = inTransaction;
	}

	protected abstract boolean shouldWriteKey();

	protected abstract boolean shouldWriteId();

	public boolean synchronize(@Nonnull Klass klass, Object persistentInstance, @Nonnull ObjectNode incomingJson) {
		Runnable noop = () -> {};
		return this.synchronize(klass, persistentInstance, incomingJson, noop);
	}

	public boolean synchronize(
		@Nonnull Klass klass,
		Object persistentInstance,
		@Nonnull ObjectNode incomingJson,
		Runnable finalizer
	) {
		if (this.inTransaction) {
			throw new AssertionError();
		}

		return this.dataStore.runInTransaction((transaction) -> {
				Instant transactionTime = this.mutationContext.getTransactionTime();
				long transactionTimeMillis = transactionTime.toEpochMilli();
				transaction.setSystemTime(transactionTimeMillis);

				this.inTransaction = true;
				try {
					boolean result = this.synchronizeInTransaction(
						klass,
						Optional.empty(),
						persistentInstance,
						incomingJson
					);
					finalizer.run();
					return result;
				} finally {
					this.inTransaction = false;
				}
			});
	}

	// TODO: Get rid of this
	public void synchronizeWithoutStartingTransaction(
		@Nonnull Klass klass,
		Object persistentInstance,
		@Nonnull ObjectNode incomingJson
	) {
		if (this.inTransaction) {
			throw new AssertionError();
		}

		this.inTransaction = true;
		try {
			this.synchronizeInTransaction(klass, Optional.empty(), persistentInstance, incomingJson);
		} finally {
			this.inTransaction = false;
		}
	}

	protected boolean synchronizeInTransaction(
		@Nonnull Klass klass,
		@Nonnull Optional<AssociationEnd> pathHere,
		Object persistentInstance,
		@Nonnull ObjectNode incomingJson
	) {
		if (!this.inTransaction) {
			throw new AssertionError();
		}

		boolean propertyMutationOccurred = false;
		if (!this.isRestrictedFromWriting(klass)) {
			propertyMutationOccurred |= this.synchronizeDataTypeProperties(klass, persistentInstance, incomingJson);
		}
		boolean associatedMutationOccurred = this.synchronizeAssociationEnds(
			klass,
			pathHere,
			persistentInstance,
			incomingJson
		);
		boolean mutationOccurred = propertyMutationOccurred || associatedMutationOccurred;

		this.synchronizeUpdatedDataTypeProperties(klass, persistentInstance, propertyMutationOccurred);

		if (mutationOccurred) {
			// TODO: Bump version number and version audit properties
			klass
				.getVersionProperty()
				.ifPresent((associationEnd) -> this.handleVersion(associationEnd, persistentInstance));
		}

		return mutationOccurred;
	}

	protected abstract void synchronizeUpdatedDataTypeProperties(
		@Nonnull Klass klass,
		Object persistentInstance,
		boolean propertyMutationOccurred
	);

	protected void synchronizeUpdatedDataTypeProperties(Klass klass, Object persistentInstance) {
		Optional<PrimitiveProperty> lastUpdatedByProperty = klass.getLastUpdatedByProperty();
		lastUpdatedByProperty.ifPresent((primitiveProperty) -> {
			Optional<String> optionalUserId = this.mutationContext.getUserId();
			String userId = optionalUserId.orElseThrow(() -> new AssertionError(primitiveProperty));
			this.dataStore.setDataTypeProperty(persistentInstance, primitiveProperty, userId);
		});
	}

	// region DataTypeProperties
	private boolean isRestrictedFromWriting(@Nonnull Klass klass) {
		return klass.isTransient() || klass.getVersionedProperty().isPresent();
	}

	protected boolean synchronizeDataTypeProperties(
		@Nonnull Klass klass,
		Object persistentInstance,
		@Nonnull ObjectNode incomingJson
	) {
		ImmutableList<DataTypeProperty> dataTypeProperties = klass.getDataTypeProperties();
		ImmutableList<DataTypeProperty> simpleDataTypeProperties = dataTypeProperties.rejectWith(
			this::shouldSkipDataTypeProperty,
			klass
		);

		this.validateSetIdDataTypeProperties(klass, persistentInstance);

		boolean mutationOccurred = false;
		for (DataTypeProperty dataTypeProperty : simpleDataTypeProperties) {
			mutationOccurred |= this.synchronizeDataTypeProperty(dataTypeProperty, persistentInstance, incomingJson);
		}

		this.synchronizeCreatedDataTypeProperties(klass, persistentInstance);

		return mutationOccurred;
	}

	protected abstract void validateSetIdDataTypeProperties(Klass klass, Object persistentInstance);

	protected abstract void synchronizeCreatedDataTypeProperties(Klass klass, Object persistentInstance);

	protected boolean synchronizeDataTypeProperty(
		@Nonnull DataTypeProperty dataTypeProperty,
		Object persistentInstance,
		@Nonnull ObjectNode incomingJson
	) {
		Object newValue = this.mutationContext.getPropertyDataFromUrl().getIfAbsent(dataTypeProperty, () ->
			JsonDataTypeValueVisitor.extractDataTypePropertyFromJson(dataTypeProperty, incomingJson)
		);
		return this.dataStore.setDataTypeProperty(persistentInstance, dataTypeProperty, newValue);
	}

	private boolean shouldSkipDataTypeProperty(@Nonnull DataTypeProperty dataTypeProperty, @Nonnull Klass klass) {
		return (
			dataTypeProperty.isForeignKey()
			|| dataTypeProperty.isAudit()
			|| dataTypeProperty.isTemporal()
			|| dataTypeProperty.isDerived()
			|| this.hasReferencePropertyDependentOnDataTypeProperty(klass, dataTypeProperty)
			|| (dataTypeProperty.isKey() && !this.shouldWriteKey())
			|| (dataTypeProperty.isID() && !this.shouldWriteId())
		);
	}

	// endregion

	// region AssociationEnds
	private boolean synchronizeAssociationEnds(
		@Nonnull Klass klass,
		@Nonnull Optional<AssociationEnd> pathHere,
		Object persistentInstance,
		@Nonnull ObjectNode incomingObjectNode
	) {
		PartitionImmutableList<AssociationEnd> forwardOwnedAssociationEnds = klass
			.getAssociationEnds()
			.reject((associationEnd) -> pathHere.equals(Optional.of(associationEnd.getOpposite())))
			.reject(AssociationEnd::isVersion)
			.reject(AssociationEnd::isAudit)
			.partition(AssociationEnd::isOwned);

		boolean mutationOccurred = false;
		for (AssociationEnd associationEnd : forwardOwnedAssociationEnds.getSelected()) {
			Multiplicity multiplicity = associationEnd.getMultiplicity();

			JsonNode jsonNode = incomingObjectNode.path(associationEnd.getName());
			if (multiplicity.isToOne()) {
				mutationOccurred |= this.handleToOne(associationEnd, persistentInstance, jsonNode);
			} else {
				mutationOccurred |= this.handleToMany(associationEnd, persistentInstance, jsonNode);
			}
		}

		for (AssociationEnd associationEnd : forwardOwnedAssociationEnds.getRejected()) {
			Multiplicity multiplicity = associationEnd.getMultiplicity();
			JsonNode jsonNode = incomingObjectNode.path(associationEnd.getName());

			if (multiplicity.isToOne()) {
				mutationOccurred |= this.handleToOneOutsideProjection(
					associationEnd,
					persistentInstance,
					incomingObjectNode,
					jsonNode
				);
			} else {
				mutationOccurred |= this.handleToManyOutsideProjection(
					associationEnd,
					persistentInstance,
					incomingObjectNode,
					jsonNode
				);
			}
		}
		return mutationOccurred;
	}

	protected abstract void handleVersion(AssociationEnd associationEnd, Object persistentInstance);

	private boolean handleToOne(
		@Nonnull AssociationEnd associationEnd,
		Object persistentParentInstance,
		@Nonnull JsonNode incomingChildInstance
	) {
		Object persistentChildInstance = this.dataStore.getToOne(persistentParentInstance, associationEnd);

		if (
			persistentChildInstance == null && !incomingChildInstance.isMissingNode() && !incomingChildInstance.isNull()
		) {
			MapIterable<DataTypeProperty, Object> keys = this.getKeysFromJsonNode(
				incomingChildInstance,
				associationEnd,
				persistentParentInstance
			);
			this.insert(associationEnd, persistentParentInstance, incomingChildInstance, keys);
			return true;
		}

		if (
			(persistentChildInstance != null && incomingChildInstance.isMissingNode()) || incomingChildInstance.isNull()
		) {
			if (incomingChildInstance.isMissingNode()) {
				return this.handleMissingToOne(associationEnd.getType(), persistentChildInstance);
			}
			if (incomingChildInstance.isNull()) {
				return this.handleNullToOne(associationEnd.getType(), persistentChildInstance);
			}
		}

		if (persistentChildInstance != null && incomingChildInstance != null) {
			PersistentSynchronizer synchronizer = this.determineNextMode(OperationMode.REPLACE);
			return synchronizer.synchronizeInTransaction(
				associationEnd.getType(),
				Optional.of(associationEnd),
				persistentChildInstance,
				(ObjectNode) incomingChildInstance
			);
		}

		return false;
	}

	protected boolean handleMissingToOne(Klass klass, Object persistentChildInstance) {
		this.deleteOrTerminate(klass, persistentChildInstance);
		return true;
	}

	protected boolean handleNullToOne(Klass klass, Object persistentChildInstance) {
		this.deleteOrTerminate(klass, persistentChildInstance);
		return true;
	}

	protected abstract boolean handleToOneOutsideProjection(
		@Nonnull AssociationEnd associationEnd,
		@Nonnull Object persistentParentInstance,
		@Nonnull ObjectNode incomingParentNode,
		@Nonnull JsonNode incomingChildInstance
	);

	protected Object findExistingChildPersistentInstance(
		@Nonnull Object persistentParentInstance,
		@Nonnull JsonNode incomingChildInstance,
		@Nonnull AssociationEnd associationEnd
	) {
		MapIterable<DataTypeProperty, Object> keys = this.getKeysFromJsonNode(
			incomingChildInstance,
			associationEnd,
			persistentParentInstance
		);
		return this.dataStore.findByKey(associationEnd.getType(), keys);
	}

	private void insert(
		@Nonnull AssociationEnd associationEnd,
		Object persistentParentInstance,
		JsonNode incomingChildInstance,
		MapIterable<DataTypeProperty, Object> keys
	) {
		Klass resultType = associationEnd.getType();
		Object newInstance = this.dataStore.instantiate(resultType, keys);
		PersistentSynchronizer synchronizer = this.determineNextMode(OperationMode.CREATE);
		boolean mutationOccurred = synchronizer.synchronizeInTransaction(
			resultType,
			Optional.of(associationEnd),
			newInstance,
			(ObjectNode) incomingChildInstance
		);
		if (!mutationOccurred) {
			// TODO: This is a workaround for a bug and should be revisited to see if it still applies in the happy path. The bug started with an association between Owner[1..1] and Details[1..1] owned. The database wound up corrupted with no row or Details. The incoming Details object is {}, because the key matches and no other properties are being patched.
			// throw new AssertionError();
		}
		// TODO: This is the backwards order from how I used to do it
		this.dataStore.setToOne(persistentParentInstance, associationEnd, newInstance);
		this.dataStore.insert(newInstance);
	}

	private void deleteOrTerminate(Klass klass, @Nonnull Object persistentInstance) {
		PersistentDeleter reladomoPersistentDeleter = new PersistentDeleter(this.mutationContext, this.dataStore);
		reladomoPersistentDeleter.deleteOrTerminate(klass, persistentInstance);
	}

	protected boolean handleToMany(
		@Nonnull AssociationEnd associationEnd,
		Object persistentParentInstance,
		@Nonnull JsonNode incomingChildInstances
	) {
		boolean mutationOccurred = false;
		// TODO: Test null where an array goes

		ImmutableList<JsonNode> incomingInstancesForUpdate = Lists.immutable
			.withAll(incomingChildInstances)
			.rejectWith(this::jsonNodeNeedsIdInferredOnInsert, associationEnd);

		MapIterable<MapIterable<DataTypeProperty, Object>, JsonNode> incomingChildInstancesByKey =
			this.indexIncomingJsonInstances(incomingInstancesForUpdate, associationEnd, persistentParentInstance);

		List<Object> persistentChildInstances = this.dataStore.getToMany(persistentParentInstance, associationEnd);
		for (Object persistentChildInstance : persistentChildInstances) {
			ImmutableMap<DataTypeProperty, Object> keys = this.getKeysFromPersistentInstance(
				persistentChildInstance,
				associationEnd.getType()
			);
			if (!incomingChildInstancesByKey.containsKey(keys)) {
				PersistentDeleter reladomoPersistentDeleter = new PersistentDeleter(
					this.mutationContext,
					this.dataStore
				);
				reladomoPersistentDeleter.deleteOrTerminate(associationEnd.getType(), persistentChildInstance);
				mutationOccurred = true;
			}
		}

		// Do a second query for the same list, but without the instances we just terminated
		List<Object> nonTerminatedPersistentChildInstances = this.dataStore.getToMany(
			persistentParentInstance,
			associationEnd
		);

		MapIterable<MapIterable<DataTypeProperty, Object>, Object> persistentChildInstancesByKey =
			this.indexPersistentInstances(nonTerminatedPersistentChildInstances, associationEnd.getType());

		for (JsonNode incomingChildInstance : incomingChildInstances) {
			Object persistentChildInstance = this.getPersistentChildInstance(
				associationEnd,
				persistentParentInstance,
				persistentChildInstancesByKey,
				incomingChildInstance
			);
			if (persistentChildInstance == null) {
				if (!associationEnd.isOwned()) {
					throw new AssertionError();
				}

				Klass resultType = associationEnd.getType();
				MapIterable<DataTypeProperty, Object> keys = this.getKeysFromJsonNode(
					incomingChildInstance,
					associationEnd,
					persistentParentInstance
				);
				Object newInstance = this.dataStore.instantiate(resultType, keys);

				this.dataStore.setToOne(newInstance, associationEnd.getOpposite(), persistentParentInstance);

				PersistentSynchronizer synchronizer = this.determineNextMode(OperationMode.CREATE);
				synchronizer.synchronizeInTransaction(
					associationEnd.getType(),
					Optional.of(associationEnd),
					newInstance,
					(ObjectNode) incomingChildInstance
				);

				this.dataStore.insert(newInstance);
				mutationOccurred = true;
			} else {
				PersistentSynchronizer synchronizer = this.determineNextMode(OperationMode.REPLACE);
				mutationOccurred |= synchronizer.synchronizeInTransaction(
					associationEnd.getType(),
					Optional.of(associationEnd),
					persistentChildInstance,
					(ObjectNode) incomingChildInstance
				);
			}
		}

		return mutationOccurred;
	}

	@Nullable
	private Object getPersistentChildInstance(
		@Nonnull AssociationEnd associationEnd,
		Object persistentParentInstance,
		@Nonnull MapIterable<MapIterable<DataTypeProperty, Object>, Object> persistentChildInstancesByKey,
		@Nonnull JsonNode incomingChildInstance
	) {
		if (this.jsonNodeNeedsIdInferredOnInsert(incomingChildInstance, associationEnd)) {
			return null;
		}

		MapIterable<DataTypeProperty, Object> keys = this.getKeysFromJsonNode(
			incomingChildInstance,
			associationEnd,
			persistentParentInstance
		);

		return persistentChildInstancesByKey.get(keys);
	}

	private boolean handleToManyOutsideProjection(
		@Nonnull AssociationEnd associationEnd,
		@Nonnull Object persistentParentInstance,
		@Nonnull ObjectNode incomingParentNode,
		@Nonnull JsonNode incomingChildInstances
	) {
		if (incomingChildInstances.isMissingNode()) {
			// Arguably this should only be the behavior of PATCH, but these instances aren't even owned, so it would be normal to not want to edit them.
			return false;
		}

		boolean mutationOccurred = false;

		// TODO: Test null where an array goes

		MapIterable<MapIterable<DataTypeProperty, Object>, JsonNode> incomingChildInstancesByKey =
			this.indexIncomingJsonInstances(incomingChildInstances, associationEnd, persistentParentInstance);

		List<Object> persistentChildInstances = this.dataStore.getToMany(persistentParentInstance, associationEnd);
		for (Object persistentChildInstance : persistentChildInstances) {
			MapIterable<DataTypeProperty, Object> keys = this.getKeysFromPersistentInstance(
				persistentChildInstance,
				associationEnd.getType()
			);
			if (!incomingChildInstancesByKey.containsKey(keys)) {
				throw new AssertionError();
				// ReladomoPersistentDeleter reladomoPersistentDeleter = new ReladomoPersistentDeleter(this.dataStore);
				// reladomoPersistentDeleter.deleteOrTerminate(persistentChildInstance, associationEnd);
			}
		}

		MapIterable<MapIterable<DataTypeProperty, Object>, Object> persistentChildInstancesByKey =
			this.indexPersistentInstances(persistentChildInstances, associationEnd.getType());

		for (JsonNode incomingChildInstance : incomingChildInstances) {
			MapIterable<DataTypeProperty, Object> keys = this.getKeysFromJsonNode(
				incomingChildInstance,
				associationEnd,
				persistentParentInstance
			);

			Object persistentChildInstance = persistentChildInstancesByKey.get(keys);
			if (persistentChildInstance == null) {
				/*
                Klass  resultType  = associationEnd.getType();
                Object newInstance = this.dataStore.instantiate(resultType, keys);
                */

				if (associationEnd.isOwned()) {
					throw new AssertionError("TODO: handle owned");
					// this.dataStore.generateAndSetId(newInstance, resultType);
					// // TODO: This is the backwards order from how I used to do it
					// this.dataStore.setToOne(newInstance, associationEnd.getOpposite(), persistentParentInstance);
					// // TODO: Determine next mode
					// OperationMode nextMode = this.determineNextMode(mode, OperationMode.CREATE);
					// this.synchronize(newInstance, (ObjectNode) incomingChildInstance, associationEnd, nextMode);
					// this.dataStore.insert(newInstance);
				} else {
					throw new AssertionError("TODO: handle not owned");
				}
			} else {
				PersistentSynchronizer synchronizer = this.determineNextMode(OperationMode.REPLACE);
				mutationOccurred = synchronizer.synchronizeInTransaction(
					associationEnd.getType(),
					Optional.of(associationEnd),
					persistentChildInstance,
					(ObjectNode) incomingChildInstance
				);
			}
		}

		return mutationOccurred;
	}

	@Nonnull
	private MapIterable<MapIterable<DataTypeProperty, Object>, JsonNode> indexIncomingJsonInstances(
		@Nonnull Iterable<JsonNode> incomingInstances,
		@Nonnull AssociationEnd associationEnd,
		Object persistentParentInstance
	) {
		MutableOrderedMap<MapIterable<DataTypeProperty, Object>, JsonNode> result = OrderedMapAdapter.adapt(
			new LinkedHashMap<>()
		);
		for (JsonNode incomingInstance : incomingInstances) {
			MapIterable<DataTypeProperty, Object> keys = this.getKeysFromJsonNode(
				incomingInstance,
				associationEnd,
				persistentParentInstance
			);
			result.put(keys, incomingInstance);
		}
		return result.asUnmodifiable();
	}

	protected ImmutableMap<DataTypeProperty, Object> getKeysFromPersistentInstance(
		Object persistentInstance,
		@Nonnull Klass klass
	) {
		return klass
			.getKeyProperties()
			.toImmutableMap(
				(keyProperty) -> keyProperty,
				(keyProperty) -> this.dataStore.getDataTypeProperty(persistentInstance, keyProperty)
			);
	}

	@Nonnull
	protected abstract PersistentSynchronizer determineNextMode(OperationMode nextMode);

	private boolean hasReferencePropertyDependentOnDataTypeProperty(Klass klass, DataTypeProperty dataTypeProperty) {
		return false;
	}

	private ImmutableList<DataTypeProperty> getNonDerivedDataTypeProperties(
		ImmutableList<DataTypeProperty> dataTypeProperties
	) {
		return dataTypeProperties;
	}

	private void handleForeignKeysForAssociationsOutsideProjection(
		Object persistentInstance,
		ObjectNode incomingJson,
		Klass klass
	) {}

	// endregion

	private Object getKeyFromJsonNode(
		@Nonnull DataTypeProperty keyProperty,
		@Nonnull JsonNode jsonNode,
		@Nonnull AssociationEnd associationEnd,
		Object persistentParentInstance
	) {
		OrderedMap<AssociationEnd, DataTypeProperty> keysMatchingThisForeignKey =
			keyProperty.getKeysMatchingThisForeignKey();

		AssociationEnd opposite = associationEnd.getOpposite();

		DataTypeProperty oppositeForeignKey = keysMatchingThisForeignKey.get(opposite);

		if (oppositeForeignKey != null) {
			Object result = this.mutationContext.getPropertyDataFromUrl().getIfAbsent(oppositeForeignKey, () ->
				this.dataStore.getDataTypeProperty(persistentParentInstance, oppositeForeignKey)
			);

			return Objects.requireNonNull(result);
		}

		if (keysMatchingThisForeignKey.notEmpty()) {
			if (keysMatchingThisForeignKey.size() != 1) {
				throw new AssertionError();
			}

			Pair<AssociationEnd, DataTypeProperty> pair = keysMatchingThisForeignKey.keyValuesView().getOnly();

			JsonNode childNode = jsonNode.path(pair.getOne().getName());
			if (childNode instanceof ObjectNode objectNode) {
				Object result = JsonDataTypeValueVisitor.extractDataTypePropertyFromJson(pair.getTwo(), objectNode);
				return Objects.requireNonNull(result);
			}

			// Leniently allow a foreign key to be used in incoming json, instead of a nested object with a primary key
			if (jsonNode.has(keyProperty.getName()) && !keyProperty.isPrivate()) {
				return JsonDataTypeValueVisitor.extractDataTypePropertyFromJson(keyProperty, (ObjectNode) jsonNode);
			}

			throw new AssertionError();
		}

		Object result = JsonDataTypeValueVisitor.extractDataTypePropertyFromJson(keyProperty, (ObjectNode) jsonNode);
		return Objects.requireNonNull(result);
	}

	@Nonnull
	private MapIterable<MapIterable<DataTypeProperty, Object>, Object> indexPersistentInstances(
		@Nonnull List<Object> persistentInstances,
		@Nonnull Klass klass
	) {
		MutableOrderedMap<MapIterable<DataTypeProperty, Object>, Object> result = OrderedMapAdapter.adapt(
			new LinkedHashMap<>()
		);

		ListIterate.groupByUniqueKey(
			persistentInstances,
			(persistentInstance) -> this.getKeysFromPersistentInstance(persistentInstance, klass),
			result
		);

		return result.asUnmodifiable();
	}

	private boolean jsonNodeNeedsIdInferredOnInsert(JsonNode jsonNode, @Nonnull AssociationEnd associationEnd) {
		return associationEnd
			.getType()
			.getKeyProperties()
			.allSatisfy((keyProperty) -> this.jsonNodeNeedsIdInferredOnInsert(keyProperty, jsonNode, associationEnd));
	}

	private boolean jsonNodeNeedsIdInferredOnInsert(
		@Nonnull DataTypeProperty keyProperty,
		JsonNode jsonNode,
		@Nonnull AssociationEnd associationEnd
	) {
		OrderedMap<AssociationEnd, DataTypeProperty> keyMatchingThisForeignKey =
			keyProperty.getKeysMatchingThisForeignKey();

		AssociationEnd opposite = associationEnd.getOpposite();

		if (keyMatchingThisForeignKey.containsKey(opposite)) {
			return false;
		}

		if (keyMatchingThisForeignKey.notEmpty()) {
			if (keyMatchingThisForeignKey.size() != 1) {
				throw new AssertionError();
			}

			return false;
		}

		return JsonDataTypeValueVisitor.dataTypePropertyIsNullInJson(keyProperty, (ObjectNode) jsonNode);
	}

	protected MapIterable<DataTypeProperty, Object> getKeysFromJsonNode(
		@Nonnull JsonNode jsonNode,
		@Nonnull AssociationEnd associationEnd,
		Object persistentParentInstance
	) {
		MutableMap<DataTypeProperty, Object> result = MapAdapter.adapt(new LinkedHashMap<>());

		for (DataTypeProperty keyProperty : associationEnd.getType().getKeyProperties()) {
			if (
				!jsonNode.has(keyProperty.getName())
				&& this.jsonNodeNeedsIdInferredOnInsert(keyProperty, jsonNode, associationEnd)
			) {
				continue;
			}

			Object key = this.getKeyFromJsonNode(keyProperty, jsonNode, associationEnd, persistentParentInstance);
			result.put(keyProperty, key);
		}

		return result.asUnmodifiable();
	}
}
