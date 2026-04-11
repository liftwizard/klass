/*
 * Copyright 2026 Craig Motlin
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

package cool.klass.data.store.reladomo.lens;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gs.fw.common.mithra.MithraDatedTransactionalObject;
import com.gs.fw.common.mithra.MithraList;
import com.gs.fw.common.mithra.MithraManagerProvider;
import com.gs.fw.common.mithra.MithraObject;
import com.gs.fw.common.mithra.MithraTransaction;
import com.gs.fw.common.mithra.MithraTransactionalObject;
import com.gs.fw.common.mithra.finder.AbstractRelatedFinder;
import com.gs.fw.common.mithra.finder.Operation;
import com.gs.fw.common.mithra.finder.RelatedFinder;
import com.gs.fw.finder.TransactionalDomainList;
import cool.klass.data.store.DataStore;
import cool.klass.data.store.Transaction;
import cool.klass.data.store.TransactionalCommand;
import cool.klass.data.store.reladomo.OperationVisitor;
import cool.klass.data.store.reladomo.TransactionAdapter;
import cool.klass.model.lens.AssociationLens;
import cool.klass.model.lens.ClassLens;
import cool.klass.model.lens.DataTypeLens;
import cool.klass.model.lens.PrimitiveLens;
import cool.klass.model.lens.reladomo.ReladomoAssociationLens;
import cool.klass.model.lens.reladomo.ReladomoClassLens;
import cool.klass.model.lens.reladomo.ReladomoLensRegistry;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import cool.klass.model.meta.domain.api.visitor.AssertObjectMatchesDataTypePropertyVisitor;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.OrderedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class ReladomoLensDataStore implements DataStore {

	private static final Marker MARKER = MarkerFactory.getMarker("reladomo transaction stats");
	private static final Logger LOGGER = LoggerFactory.getLogger(ReladomoLensDataStore.class);

	private final ReladomoLensRegistry lensRegistry;
	private final Supplier<UUID> uuidSupplier;
	private final int retryCount;

	public ReladomoLensDataStore(
		@Nonnull ReladomoLensRegistry lensRegistry,
		@Nonnull Supplier<UUID> uuidSupplier,
		int retryCount
	) {
		this.lensRegistry = Objects.requireNonNull(lensRegistry);
		this.uuidSupplier = Objects.requireNonNull(uuidSupplier);
		this.retryCount = retryCount;
	}

	// region Transaction management

	@Override
	public <Result> Result runInTransaction(@Nonnull TransactionalCommand<Result> transactionalCommand) {
		return MithraManagerProvider.getMithraManager().executeTransactionalCommand(
				(transaction) -> {
					try {
						Transaction transactionAdapter = new TransactionAdapter(transaction);
						return transactionalCommand.run(transactionAdapter);
					} finally {
						ReladomoLensDataStore.logTransactionalStats(transaction);
					}
				},
				this.retryCount
			);
	}

	@Override
	public void runInTransaction(@Nonnull Runnable runnable) {
		MithraManagerProvider.getMithraManager().executeTransactionalCommand(
				(tx) -> {
					runnable.run();
					return null;
				},
				this.retryCount
			);
	}

	private static void logTransactionalStats(MithraTransaction reladomoTransaction) {
		if (MithraManagerProvider.getMithraManager().getCurrentTransaction() != reladomoTransaction) {
			throw new AssertionError();
		}

		MDC.put(
			"total database retrievals",
			String.valueOf(MithraManagerProvider.getMithraManager().getDatabaseRetrieveCount())
		);
		MDC.put("database retrievals", String.valueOf(reladomoTransaction.getDatabaseRetrieveCount()));

		LOGGER.debug(
			MARKER,
			"total database retrievals: {} database retrievals: {}",
			MithraManagerProvider.getMithraManager().getDatabaseRetrieveCount(),
			reladomoTransaction.getDatabaseRetrieveCount()
		);

		MDC.remove("total database retrievals");
		MDC.remove("database retrievals");
	}

	// endregion

	// region Query operations

	@Override
	@Nonnull
	public Object instantiate(@Nonnull Klass klass, @Nonnull MapIterable<DataTypeProperty, Object> keys) {
		keys.each(Objects::requireNonNull);

		@SuppressWarnings("unchecked")
		ReladomoClassLens<Object> reladomoLens = (ReladomoClassLens<Object>) this.lensRegistry.getClassLens(klass);
		Object newInstance = reladomoLens.instantiate();

		this.generateAndSetId(newInstance, klass, reladomoLens);

		ImmutableList<DataTypeProperty> keyProperties = klass
			.getKeyProperties()
			.reject(DataTypeProperty::isID)
			.reject(DataTypeProperty::isAudit);
		if (keyProperties.size() != keys.size()) {
			String error = String.format(
				"Expected one key for each key property in %s but got %s",
				keyProperties,
				keys
			);
			throw new IllegalArgumentException(error);
		}

		for (DataTypeProperty keyProperty : keyProperties) {
			Object key = keys.get(keyProperty);
			Objects.requireNonNull(key, () -> "Expected non-null key for property: " + keyProperty);
			setDataTypeProperty(newInstance, reladomoLens, keyProperty, key);
		}

		return newInstance;
	}

	@Override
	@Nonnull
	public List<Object> findAll(@Nonnull Klass klass) {
		ReladomoClassLens<?> reladomoLens = this.lensRegistry.getClassLens(klass);
		RelatedFinder<?> finder = reladomoLens.getRelatedFinder();
		return (List<Object>) finder.findMany(finder.all());
	}

	@Override
	@Nullable
	public Object findByKey(@Nonnull Klass klass, @Nonnull MapIterable<DataTypeProperty, Object> keys) {
		Operation operation = this.buildFindByKeyOperation(klass, keys);
		ReladomoClassLens<?> reladomoLens = (ReladomoClassLens<?>) this.lensRegistry.getClassLens(klass);
		return reladomoLens.getRelatedFinder().findOne(operation);
	}

	@Override
	@Nonnull
	public List<Object> findByKeyReturningList(
		@Nonnull Klass klass,
		@Nonnull MapIterable<DataTypeProperty, Object> keys
	) {
		Operation operation = this.buildFindByKeyOperation(klass, keys);
		ReladomoClassLens<?> reladomoLens = this.lensRegistry.getClassLens(klass);
		return (List<Object>) reladomoLens.getRelatedFinder().findMany(operation);
	}

	// endregion

	// region Property access

	@Override
	@Nullable
	public Object getDataTypeProperty(@Nonnull Object persistentInstance, @Nonnull DataTypeProperty dataTypeProperty) {
		Klass klass = this.resolveKlassForProperty(persistentInstance, dataTypeProperty);

		Object effectiveInstance = this.navigateToOwningInstance(persistentInstance, klass);
		ClassLens<?> classLens = this.lensRegistry.getClassLens(klass);
		DataTypeLens<Object, ?> dataTypeLens = (DataTypeLens<Object, ?>) classLens.getLensByProperty(dataTypeProperty);
		Object result = dataTypeLens.get(effectiveInstance);

		if (result == null && dataTypeProperty.isRequired()) {
			String message = String.format("Found null for required property: '%s'", dataTypeProperty);
			throw new IllegalStateException(message);
		}

		return result;
	}

	@Override
	public boolean setDataTypeProperty(
		@Nonnull Object persistentInstance,
		@Nonnull DataTypeProperty dataTypeProperty,
		@Nullable Object newValue
	) {
		if (dataTypeProperty.isDerived()) {
			throw new AssertionError("May not set derived property: " + dataTypeProperty);
		}

		Klass klass = this.resolveKlassForProperty(persistentInstance, dataTypeProperty);

		Object effectiveInstance = this.navigateToOwningInstance(persistentInstance, klass);
		ClassLens<?> classLens = this.lensRegistry.getClassLens(klass);
		var dataTypeLens = (DataTypeLens<Object, Object>) classLens.getLensByProperty(dataTypeProperty);
		Object oldValue = dataTypeLens.get(effectiveInstance);
		if (Objects.equals(oldValue, newValue)) {
			return false;
		}

		if (newValue == null && dataTypeProperty.isRequired()) {
			String message = String.format(
				"May not set required property to null: '%s.%s'",
				dataTypeProperty.getOwningClassifier().getName(),
				dataTypeProperty
			);
			throw new IllegalStateException(message);
		}

		if (newValue == null) {
			dataTypeLens.setNull(effectiveInstance);
		} else {
			dataTypeLens.set(effectiveInstance, newValue);
		}
		return true;
	}

	@Override
	@Nullable
	public Object getToOne(@Nonnull Object persistentSourceInstance, @Nonnull ReferenceProperty referenceProperty) {
		if (!(referenceProperty instanceof AssociationEnd associationEnd)) {
			throw new AssertionError("Expected AssociationEnd but got " + referenceProperty);
		}

		Klass klass = this.resolveKlassForProperty(persistentSourceInstance, associationEnd);

		Object effectiveInstance = this.navigateToOwningInstance(persistentSourceInstance, klass);
		ClassLens<?> classLens = this.lensRegistry.getClassLens(klass);
		@SuppressWarnings("unchecked")
		AssociationLens<Object, ?> associationLens = (AssociationLens<Object, ?>) classLens.getLensByProperty(
			associationEnd
		);
		Object result = associationLens.get(effectiveInstance);
		if (result instanceof List<?> list) {
			throw new IllegalStateException("Expected single object but got " + list.size());
		}
		return result;
	}

	@Override
	@Nonnull
	public List<Object> getToMany(
		@Nonnull Object persistentSourceInstance,
		@Nonnull ReferenceProperty referenceProperty
	) {
		if (!(referenceProperty instanceof AssociationEnd associationEnd)) {
			throw new AssertionError("Expected AssociationEnd but got " + referenceProperty);
		}

		Klass klass = this.resolveKlassForProperty(persistentSourceInstance, associationEnd);

		Object effectiveInstance = this.navigateToOwningInstance(persistentSourceInstance, klass);
		ReladomoClassLens<?> classLens = this.lensRegistry.getClassLens(klass);
		@SuppressWarnings("unchecked")
		ReladomoAssociationLens<Object, ?> associationLens = (ReladomoAssociationLens<
			Object,
			?
		>) classLens.getLensByProperty(associationEnd);
		AbstractRelatedFinder relationshipFinder = associationLens.getRelationshipFinder();
		Object result = relationshipFinder.valueOf(effectiveInstance);
		if (result == null) {
			return List.of();
		}
		if (!(result instanceof List)) {
			throw new IllegalStateException("Expected list but got " + result.getClass().getCanonicalName());
		}
		return (List<Object>) result;
	}

	// endregion

	// region Persistence operations

	@Override
	public void insert(@Nonnull Object persistentInstance) {
		if (!(persistentInstance instanceof MithraTransactionalObject)) {
			String detailMessage =
				"Expected MithraTransactionalObject but got " + persistentInstance.getClass().getCanonicalName();
			throw new AssertionError(detailMessage);
		}

		((MithraTransactionalObject) persistentInstance).insert();
	}

	@Override
	public boolean setToOne(
		@Nonnull Object persistentSourceInstance,
		@Nonnull AssociationEnd associationEnd,
		@Nullable Object persistentTargetInstance
	) {
		boolean mutationOccurred = false;

		// A Reladomo bug prevents just calling a method like setQuestion here. Instead, we have to call foreign key setters like setQuestionId

		ImmutableList<DataTypeProperty> targetDataTypeProperties = associationEnd
			.getOwningClassifier()
			.getDataTypeProperties();
		for (DataTypeProperty targetDataTypeProperty : targetDataTypeProperties) {
			OrderedMap<AssociationEnd, DataTypeProperty> keysMatchingThisForeignKey =
				targetDataTypeProperty.getKeysMatchingThisForeignKey();

			DataTypeProperty keyInRelatedObject = keysMatchingThisForeignKey.getIfAbsentValue(associationEnd, null);
			if (keyInRelatedObject == null) {
				continue;
			}

			DataTypeProperty foreignKey = targetDataTypeProperty;

			Object keyValue = persistentTargetInstance == null
				? null
				: this.getDataTypeProperty(persistentTargetInstance, keyInRelatedObject);

			mutationOccurred |= this.setDataTypeProperty(persistentSourceInstance, foreignKey, keyValue);
		}

		return mutationOccurred;
	}

	@Override
	public void deleteOrTerminate(@Nonnull Object persistentInstance) {
		if (persistentInstance instanceof MithraDatedTransactionalObject transactionalObject) {
			transactionalObject.terminate();
		} else if (persistentInstance instanceof MithraTransactionalObject transactionalObject) {
			transactionalObject.cascadeDelete();
		} else {
			String detailMessage =
				"Unexpected persistent instance type: " + persistentInstance.getClass().getCanonicalName();
			throw new AssertionError(detailMessage);
		}
	}

	@Override
	public void purgeAll(@Nonnull Klass klass) {
		if (klass.isAbstract()) {
			return;
		}

		ReladomoClassLens<?> reladomoLens = this.lensRegistry.getClassLens(klass);
		RelatedFinder<?> finder = reladomoLens.getRelatedFinder();

		if (klass.isSystemTemporal()) {
			throw new UnsupportedOperationException(
				this.getClass().getSimpleName() + ".purgeAll() not implemented yet"
			);
		}

		Operation operation = finder.all();
		MithraList<?> mithraList = finder.findMany(operation);
		var transactionalDomainList = (TransactionalDomainList<?>) mithraList;
		transactionalDomainList.deleteAll();
	}

	// endregion

	// region Type checking and hierarchy navigation

	@Override
	public boolean isInstanceOf(@Nonnull Object persistentInstance, @Nonnull Classifier classifier) {
		Klass concreteKlass = this.lensRegistry.getClassLensForJavaClass(persistentInstance.getClass()).getKlass();
		return concreteKlass.isSubTypeOf(classifier);
	}

	@Override
	@Nonnull
	public Klass getMostSpecificSubclass(@Nonnull Object persistentInstance, @Nonnull Klass klass) {
		if (!(persistentInstance instanceof MithraObject)) {
			String detailMessage = "Expected MithraObject but got " + persistentInstance.getClass().getCanonicalName();
			throw new AssertionError(detailMessage);
		}

		ImmutableList<Klass> potentialSubClasses = klass
			.getSubClasses()
			.select((subClass) -> {
				MithraObject subClassPersistentInstance = this.getSubClassPersistentInstance(
					klass,
					subClass,
					(MithraObject) persistentInstance
				);
				return subClassPersistentInstance != null;
			});

		if (potentialSubClasses.isEmpty()) {
			return klass;
		}

		if (potentialSubClasses.size() == 1) {
			Klass onlySubClass = potentialSubClasses.getOnly();
			MithraObject subClassPersistentInstance = this.getSubClassPersistentInstance(
				klass,
				onlySubClass,
				(MithraObject) persistentInstance
			);

			return this.getMostSpecificSubclass(subClassPersistentInstance, onlySubClass);
		}

		String detailMessage = "Expected one subclass but got " + potentialSubClasses;
		throw new AssertionError(detailMessage);
	}

	private MithraObject getSubClassPersistentInstance(Klass klass, Klass subClass, MithraObject persistentInstance) {
		return (MithraObject) this.getUntypedClassLens(klass).getSubClassInstance(persistentInstance, subClass);
	}

	@Override
	@Nonnull
	public Object getSuperClass(@Nonnull Object persistentInstance, @Nonnull Klass klass) {
		Object result = this.getUntypedClassLens(klass).getSuperClassInstance(persistentInstance);
		Objects.requireNonNull(result, () ->
			"Expected result to not be null for superClass: %s, persistentInstance: %s".formatted(
				klass,
				persistentInstance
			)
		);
		return result;
	}

	@Override
	@Nullable
	public Object getSubClass(@Nonnull Object persistentInstance, @Nonnull Klass superClass, @Nonnull Klass subClass) {
		if (!subClass.isStrictSubTypeOf(superClass)) {
			throw new AssertionError("Expected " + subClass + " to be a strict subtype of " + superClass);
		}

		return this.getUntypedClassLens(superClass).getSubClassInstance(persistentInstance, subClass);
	}

	// endregion

	// region Private helpers

	@SuppressWarnings("unchecked")
	private ReladomoClassLens<Object> getUntypedClassLens(@Nonnull Klass klass) {
		return (ReladomoClassLens<Object>) this.lensRegistry.getClassLens(klass);
	}

	private <T> void generateAndSetId(
		@Nonnull T persistentInstance,
		@Nonnull Klass klass,
		@Nonnull ReladomoClassLens<T> reladomoLens
	) {
		ImmutableList<DataTypeProperty> idProperties = klass.getDataTypeProperties().select(DataTypeProperty::isID);
		if (idProperties.isEmpty()) {
			return;
		}

		PrimitiveProperty idProperty = (PrimitiveProperty) idProperties.getOnly();

		if (idProperty.getType().isNumeric()) {
			reladomoLens.generateAndSetId(persistentInstance);
		} else if (idProperty.getType() == PrimitiveType.STRING) {
			UUID uuid = this.uuidSupplier.get();
			String uuidString = uuid.toString();
			PrimitiveLens<T, String> idLens = (PrimitiveLens<T, String>) reladomoLens.getLensByProperty(idProperty);
			idLens.set(persistentInstance, uuidString);
		} else {
			throw new AssertionError(idProperty);
		}
	}

	private static <T> void setDataTypeProperty(
		@Nonnull T persistentInstance,
		@Nonnull ReladomoClassLens<T> reladomoLens,
		@Nonnull DataTypeProperty dataTypeProperty,
		@Nonnull Object value
	) {
		@SuppressWarnings("unchecked")
		var dataTypeLens = (DataTypeLens<T, Object>) reladomoLens.getLensByProperty(dataTypeProperty);
		dataTypeLens.set(persistentInstance, value);
	}

	@Nonnull
	private Operation buildFindByKeyOperation(
		@Nonnull Klass klass,
		@Nonnull MapIterable<DataTypeProperty, Object> keys
	) {
		ImmutableList<DataTypeProperty> keyProperties = klass.getKeyProperties();
		if (keyProperties.size() != keys.size()) {
			String error = String.format(
				"Expected keys for properties %s but got the wrong number of keys %s",
				keyProperties,
				keys
			);
			throw new IllegalArgumentException(error);
		}

		ReladomoClassLens<?> reladomoLens = this.lensRegistry.getClassLens(klass);
		RelatedFinder<?> finder = reladomoLens.getRelatedFinder();

		ImmutableList<Operation> operations = keyProperties.collect((keyProperty) -> {
			Object key = keys.get(keyProperty);
			if (key == null) {
				throw new IllegalStateException("Expected non-null key for property: " + keyProperty);
			}

			keyProperty.visit(new AssertObjectMatchesDataTypePropertyVisitor(key));

			com.gs.fw.common.mithra.attribute.Attribute attribute = finder.getAttributeByName(keyProperty.getName());

			OperationVisitor visitor = new OperationVisitor(attribute, key);
			keyProperty.visit(visitor);
			return visitor.getResult();
		});

		return operations.reduce(Operation::and).get();
	}

	private Klass resolveKlassForProperty(Object persistentInstance, Property property) {
		if (property.getOwningClassifier() instanceof Klass klass) {
			return klass;
		}
		return this.lensRegistry.getClassLensForJavaClass(persistentInstance.getClass()).getKlass();
	}

	private Object navigateToOwningInstance(Object persistentInstance, Klass owningKlass) {
		if (!(persistentInstance instanceof MithraObject) || this.isInstanceOf(persistentInstance, owningKlass)) {
			return persistentInstance;
		}

		return this.navigateToClassifierInstance(persistentInstance, owningKlass);
	}

	private Object navigateToClassifierInstance(Object persistentInstance, Classifier targetClassifier) {
		Klass concreteKlass = this.lensRegistry.getClassLensForJavaClass(persistentInstance.getClass()).getKlass();

		if (concreteKlass.isSubTypeOf(targetClassifier)) {
			// Walk up the superclass chain
			Object current = persistentInstance;
			Klass currentKlass = concreteKlass;
			while (!currentKlass.equals(targetClassifier)) {
				current = this.getUntypedClassLens(currentKlass).getSuperClassInstance(current);
				currentKlass = currentKlass.getSuperClass().get();
			}
			return current;
		}

		if (targetClassifier instanceof Klass targetKlass && targetKlass.isStrictSubTypeOf(concreteKlass)) {
			// Walk down the subclass chain, finding the direct child on the path to target at each step
			Object current = persistentInstance;
			Klass currentKlass = concreteKlass;
			while (!currentKlass.equals(targetKlass)) {
				Klass nextSubClass = currentKlass
					.getSubClasses()
					.detect((sub) -> targetKlass.equals(sub) || targetKlass.isStrictSubTypeOf(sub));
				if (nextSubClass == null) {
					throw new AssertionError(
						"Could not find subclass path from " + currentKlass.getName() + " to " + targetKlass.getName()
					);
				}
				current = this.getUntypedClassLens(currentKlass).getSubClassInstance(current, nextSubClass);
				currentKlass = nextSubClass;
			}
			return current;
		}

		throw new AssertionError(
			"Could not navigate to "
			+ targetClassifier.getName()
			+ " from "
			+ persistentInstance.getClass().getCanonicalName()
		);
	}

	// endregion
}
