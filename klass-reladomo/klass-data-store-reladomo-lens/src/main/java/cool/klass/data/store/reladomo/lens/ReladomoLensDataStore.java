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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.gs.fw.common.mithra.MithraDatedTransactionalObject;
import com.gs.fw.common.mithra.MithraList;
import com.gs.fw.common.mithra.MithraManagerProvider;
import com.gs.fw.common.mithra.MithraObject;
import com.gs.fw.common.mithra.MithraTransaction;
import com.gs.fw.common.mithra.MithraTransactionalObject;
import com.gs.fw.common.mithra.attribute.AsOfAttribute;
import com.gs.fw.common.mithra.attribute.Attribute;
import com.gs.fw.common.mithra.attribute.TimestampAttribute;
import com.gs.fw.common.mithra.finder.AbstractRelatedFinder;
import com.gs.fw.common.mithra.finder.Operation;
import com.gs.fw.common.mithra.finder.RelatedFinder;
import com.gs.fw.finder.TransactionalDomainList;
import cool.klass.data.store.DataStore;
import cool.klass.data.store.Transaction;
import cool.klass.data.store.TransactionalCommand;
import cool.klass.data.store.reladomo.OperationVisitor;
import cool.klass.data.store.reladomo.TransactionAdapter;
import cool.klass.model.lens.ClassLens;
import cool.klass.model.lens.DataTypeLens;
import cool.klass.model.lens.reladomo.ReladomoAssociationLens;
import cool.klass.model.lens.reladomo.ReladomoClassLens;
import cool.klass.model.lens.reladomo.ReladomoLensRegistry;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Enumeration;
import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import cool.klass.model.meta.domain.api.visitor.AssertObjectMatchesDataTypePropertyVisitor;
import cool.klass.reladomo.utc.infinity.timestamp.UtcInfinityTimestamp;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.api.map.OrderedMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class ReladomoLensDataStore implements DataStore {

	private static final Marker MARKER = MarkerFactory.getMarker("reladomo transaction stats");
	private static final Logger LOGGER = LoggerFactory.getLogger(ReladomoLensDataStore.class);

	private static final Converter<String, String> LOWER_TO_UPPER_CAMEL = CaseFormat.LOWER_CAMEL.converterTo(
		CaseFormat.UPPER_CAMEL
	);

	private static final Converter<String, String> UPPER_TO_LOWER_CAMEL = CaseFormat.UPPER_CAMEL.converterTo(
		CaseFormat.LOWER_CAMEL
	);

	private final ReladomoLensRegistry lensRegistry;
	private final Supplier<UUID> uuidSupplier;
	private final int retryCount;

	private final MutableOrderedMap<Classifier, AbstractRelatedFinder> memoizedRelatedFinders = OrderedMapAdapter.adapt(
		new LinkedHashMap<>()
	);
	private final MutableOrderedMap<Pair<Class<?>, PrimitiveProperty>, Method> memoizedGenerateAndSetIdMethods =
		OrderedMapAdapter.adapt(new LinkedHashMap<>());
	private final MutableOrderedMap<Property, Method> memoizedGetters = OrderedMapAdapter.adapt(new LinkedHashMap<>());

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

		Object newInstance;
		if (this.lensRegistry.hasClassLens(klass)) {
			ReladomoClassLens<?> reladomoLens = (ReladomoClassLens<?>) this.lensRegistry.getClassLens(klass);
			newInstance = reladomoLens.instantiate();
		} else {
			newInstance = this.instantiateReflectively(klass);
		}

		this.generateAndSetId(newInstance, klass);

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
			this.setDataTypeProperty(newInstance, keyProperty, key);
		}

		return newInstance;
	}

	@Override
	@Nonnull
	public List<Object> findAll(@Nonnull Klass klass) {
		RelatedFinder<?> finder = this.getFinderForKlass(klass);
		return (List<Object>) finder.findMany(finder.all());
	}

	@Override
	@Nullable
	public Object findByKey(@Nonnull Klass klass, @Nonnull MapIterable<DataTypeProperty, Object> keys) {
		Operation operation = this.buildFindByKeyOperation(klass, keys);
		RelatedFinder<?> finder = this.getFinderForKlass(klass);
		return finder.findOne(operation);
	}

	@Override
	@Nonnull
	public List<Object> findByKeyReturningList(
		@Nonnull Klass klass,
		@Nonnull MapIterable<DataTypeProperty, Object> keys
	) {
		Operation operation = this.buildFindByKeyOperation(klass, keys);
		RelatedFinder<?> finder = this.getFinderForKlass(klass);
		return (List<Object>) finder.findMany(operation);
	}

	// endregion

	// region Property access

	@Override
	@Nullable
	public Object getDataTypeProperty(@Nonnull Object persistentInstance, @Nonnull DataTypeProperty dataTypeProperty) {
		if (dataTypeProperty.isDerived()) {
			return this.getPropertyReflectively(persistentInstance, dataTypeProperty);
		}

		if (dataTypeProperty.getOwningClassifier() instanceof Klass klass && this.lensRegistry.hasClassLens(klass)) {
			Object effectiveInstance = this.navigateToOwningInstance(persistentInstance, klass);
			ClassLens<?> classLens = this.lensRegistry.getClassLens(klass);
			DataTypeLens<Object, ?> dataTypeLens = (DataTypeLens<Object, ?>) classLens.getLensByProperty(
				dataTypeProperty
			);
			Object result = dataTypeLens.get(effectiveInstance);

			if (result == null && dataTypeProperty.isRequired()) {
				String message = String.format("Found null for required property: '%s'", dataTypeProperty);
				throw new IllegalStateException(message);
			}

			return result;
		}

		return this.getDataTypePropertyViaAttribute(persistentInstance, dataTypeProperty);
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

		if (dataTypeProperty.getOwningClassifier() instanceof Klass klass && this.lensRegistry.hasClassLens(klass)) {
			Object effectiveInstance = this.navigateToOwningInstance(persistentInstance, klass);
			ClassLens<?> classLens = this.lensRegistry.getClassLens(klass);
			DataTypeLens<Object, Object> dataTypeLens = (DataTypeLens<Object, Object>) classLens.getLensByProperty(
				dataTypeProperty
			);
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

			dataTypeLens.set(effectiveInstance, newValue);
			return true;
		}

		return this.setDataTypePropertyViaAttribute(persistentInstance, dataTypeProperty, newValue);
	}

	@Override
	@Nullable
	public Object getToOne(@Nonnull Object persistentSourceInstance, @Nonnull ReferenceProperty referenceProperty) {
		Classifier owningClassifier = referenceProperty.getOwningClassifier();

		if (
			referenceProperty instanceof AssociationEnd associationEnd
			&& owningClassifier instanceof Klass klass
			&& this.lensRegistry.hasClassLens(klass)
		) {
			Object effectiveInstance = this.navigateToOwningInstance(persistentSourceInstance, klass);
			ClassLens<?> classLens = this.lensRegistry.getClassLens(klass);
			ReladomoAssociationLens<Object, ?> associationLens = (ReladomoAssociationLens<
				Object,
				?
			>) classLens.getLensByProperty(associationEnd);
			AbstractRelatedFinder relationshipFinder = associationLens.getRelationshipFinder();
			Object result = relationshipFinder.valueOf(effectiveInstance);
			if (result instanceof List<?> list) {
				throw new IllegalStateException("Expected single object but got " + list.size());
			}
			return result;
		}

		return this.getToOneViaFinder(persistentSourceInstance, referenceProperty);
	}

	@Override
	@Nonnull
	public List<Object> getToMany(
		@Nonnull Object persistentSourceInstance,
		@Nonnull ReferenceProperty referenceProperty
	) {
		Classifier owningClassifier = referenceProperty.getOwningClassifier();

		if (
			referenceProperty instanceof AssociationEnd associationEnd
			&& owningClassifier instanceof Klass klass
			&& this.lensRegistry.hasClassLens(klass)
		) {
			Object effectiveInstance = this.navigateToOwningInstance(persistentSourceInstance, klass);
			ClassLens<?> classLens = this.lensRegistry.getClassLens(klass);
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

		return this.getToManyViaFinder(persistentSourceInstance, referenceProperty);
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

			Object keyValue = persistentTargetInstance == null
				? null
				: this.getDataTypeProperty(persistentTargetInstance, keyInRelatedObject);

			mutationOccurred |= this.setDataTypeProperty(persistentSourceInstance, targetDataTypeProperty, keyValue);
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

		RelatedFinder<?> finder = this.getFinderForKlass(klass);

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
		try {
			Class<?> persistentInstanceClass = persistentInstance.getClass();
			Class<?> domainModelClass = Class.forName(classifier.getFullyQualifiedName());
			return domainModelClass.isAssignableFrom(persistentInstanceClass);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
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
		AbstractRelatedFinder finder = this.getRelatedFinder(klass);

		String relationshipName = UPPER_TO_LOWER_CAMEL.convert(subClass.getName()) + "SubClass";

		AbstractRelatedFinder relationshipFinder = (AbstractRelatedFinder) finder.getRelationshipFinderByName(
			relationshipName
		);

		if (relationshipFinder == null) {
			String detailMessage =
				"Domain model and generated code are out of sync. Try rerunning a full clean build. Could not find relationship for property "
				+ relationshipName;
			throw new AssertionError(detailMessage);
		}

		Object result = relationshipFinder.valueOf(persistentInstance);
		return (MithraObject) result;
	}

	@Override
	@Nullable
	public Object getSuperClass(@Nonnull Object persistentInstance, @Nonnull Klass klass) {
		AbstractRelatedFinder finder = this.getRelatedFinder(klass);

		String relationshipName = UPPER_TO_LOWER_CAMEL.convert(klass.getSuperClass().get().getName()) + "SuperClass";

		AbstractRelatedFinder relationshipFinder = (AbstractRelatedFinder) finder.getRelationshipFinderByName(
			relationshipName
		);

		if (relationshipFinder == null) {
			String detailMessage =
				"Domain model and generated code are out of sync. Try rerunning a full clean build. Could not find relationship for property "
				+ relationshipName;
			throw new AssertionError(detailMessage);
		}

		Object result = relationshipFinder.valueOf(persistentInstance);
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

		AbstractRelatedFinder finder = this.getRelatedFinder(superClass);

		String relationshipName = UPPER_TO_LOWER_CAMEL.convert(subClass.getName()) + "SubClass";

		AbstractRelatedFinder relationshipFinder = (AbstractRelatedFinder) finder.getRelationshipFinderByName(
			relationshipName
		);

		if (relationshipFinder == null) {
			String detailMessage =
				"Domain model and generated code are out of sync. Try rerunning a full clean build. Could not find relationship for property "
				+ relationshipName;
			throw new AssertionError(detailMessage);
		}

		return relationshipFinder.valueOf(persistentInstance);
	}

	// endregion

	// region Private helpers — ID generation

	private void generateAndSetId(@Nonnull Object persistentInstance, @Nonnull Klass klass) {
		ImmutableList<DataTypeProperty> idProperties = klass.getDataTypeProperties().select(DataTypeProperty::isID);
		if (idProperties.isEmpty()) {
			return;
		}

		PrimitiveProperty idProperty = (PrimitiveProperty) idProperties.getOnly();

		if (idProperty.getType().isNumeric()) {
			try {
				Method generateAndSetIdMethod = this.getGenerateAndSetIdMethod(
					persistentInstance.getClass(),
					idProperty
				);
				generateAndSetIdMethod.invoke(persistentInstance);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		} else if (idProperty.getType() == PrimitiveType.STRING) {
			UUID uuid = this.uuidSupplier.get();
			String uuidString = uuid.toString();
			this.setDataTypeProperty(persistentInstance, idProperty, uuidString);
		} else {
			throw new AssertionError(idProperty);
		}
	}

	@Nonnull
	private Method getGenerateAndSetIdMethod(Class<?> klass, PrimitiveProperty idProperty)
		throws NoSuchMethodException {
		Pair<Class<?>, PrimitiveProperty> key = Tuples.pair(klass, idProperty);
		if (this.memoizedGenerateAndSetIdMethods.containsKey(key)) {
			return this.memoizedGenerateAndSetIdMethods.get(key);
		}
		String methodName = "generateAndSet" + LOWER_TO_UPPER_CAMEL.convert(idProperty.getName());
		Method generateAndSetIdMethod = klass.getMethod(methodName);
		this.memoizedGenerateAndSetIdMethods.put(key, generateAndSetIdMethod);
		return generateAndSetIdMethod;
	}

	// endregion

	// region Private helpers — finder operations

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

		RelatedFinder<?> finder = this.getFinderForKlass(klass);

		ImmutableList<Operation> operations = keyProperties.collect((keyProperty) -> {
			Object key = keys.get(keyProperty);
			if (key == null) {
				throw new IllegalStateException("Expected non-null key for property: " + keyProperty);
			}

			keyProperty.visit(new AssertObjectMatchesDataTypePropertyVisitor(key));

			Attribute attribute = finder.getAttributeByName(keyProperty.getName());

			OperationVisitor visitor = new OperationVisitor(attribute, key);
			keyProperty.visit(visitor);
			return visitor.getResult();
		});

		return operations.reduce(Operation::and).get();
	}

	private RelatedFinder<?> getFinderForKlass(@Nonnull Klass klass) {
		if (this.lensRegistry.hasClassLens(klass)) {
			ReladomoClassLens<?> reladomoLens = (ReladomoClassLens<?>) this.lensRegistry.getClassLens(klass);
			return reladomoLens.getRelatedFinder();
		}
		return this.getRelatedFinder(klass);
	}

	// endregion

	// region Private helpers — hierarchy navigation

	private Object navigateToOwningInstance(Object persistentInstance, Klass owningKlass) {
		if (!(persistentInstance instanceof MithraObject) || this.isInstanceOf(persistentInstance, owningKlass)) {
			return persistentInstance;
		}

		return this.navigateToClassifierInstance(persistentInstance, owningKlass);
	}

	private Object navigateToClassifierInstance(Object persistentInstance, Classifier targetClassifier) {
		RelatedFinder<?> concreteFinder = ((MithraObject) persistentInstance).zGetPortal().getFinder();

		String superClassRelationshipName = UPPER_TO_LOWER_CAMEL.convert(targetClassifier.getName()) + "SuperClass";
		AbstractRelatedFinder superClassRelationship =
			(AbstractRelatedFinder) concreteFinder.getRelationshipFinderByName(superClassRelationshipName);

		if (superClassRelationship != null) {
			return superClassRelationship.valueOf(persistentInstance);
		}

		String subClassRelationshipName = UPPER_TO_LOWER_CAMEL.convert(targetClassifier.getName()) + "SubClass";
		AbstractRelatedFinder subClassRelationship = (AbstractRelatedFinder) concreteFinder.getRelationshipFinderByName(
			subClassRelationshipName
		);

		if (subClassRelationship != null) {
			return subClassRelationship.valueOf(persistentInstance);
		}

		throw new AssertionError(
			"Could not navigate to "
			+ targetClassifier.getName()
			+ " from "
			+ persistentInstance.getClass().getCanonicalName()
		);
	}

	@Nonnull
	private AbstractRelatedFinder getRelatedFinder(@Nonnull Classifier classifier) {
		if (this.memoizedRelatedFinders.containsKey(classifier)) {
			return this.memoizedRelatedFinders.get(classifier);
		}

		try {
			String finderName = classifier.getFullyQualifiedName() + "Finder";
			Class<?> finderClass = Class.forName(finderName);
			Method getFinderMethod = finderClass.getMethod("getFinderInstance");
			AbstractRelatedFinder result = (AbstractRelatedFinder) getFinderMethod.invoke(null);
			this.memoizedRelatedFinders.put(classifier, result);
			return result;
		} catch (@Nonnull ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	// endregion

	// region Private helpers — attribute-based fallback for unregistered classes

	@Nonnull
	private Object instantiateReflectively(@Nonnull Klass klass) {
		try {
			Class<?> aClass = Class.forName(klass.getFullyQualifiedName());
			Class<?>[] parameterTypes = klass.isSystemTemporal()
				? new Class<?>[] { Timestamp.class }
				: new Class<?>[] {};
			Constructor<?> constructor = aClass.getConstructor(parameterTypes);
			Object[] constructorArgs = klass.isSystemTemporal()
				? new Object[] { UtcInfinityTimestamp.getDefaultInfinity() }
				: new Object[] {};
			return constructor.newInstance(constructorArgs);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Nullable
	private Object getDataTypePropertyViaAttribute(
		@Nonnull Object persistentInstance,
		@Nonnull DataTypeProperty dataTypeProperty
	) {
		Objects.requireNonNull(persistentInstance);
		if (!(persistentInstance instanceof MithraObject mithraObject)) {
			String detailMessage = "Expected MithraObject but got " + persistentInstance.getClass().getCanonicalName();
			throw new AssertionError(detailMessage);
		}

		Attribute attribute = this.findAttribute(mithraObject, dataTypeProperty);
		if (attribute == null) {
			String detailMessage =
				"Domain model and generated code are out of sync. Try rerunning a full clean build. Could not find attribute: "
				+ dataTypeProperty.getName();
			throw new AssertionError(detailMessage);
		}

		if (attribute.isAttributeNull(persistentInstance)) {
			if (dataTypeProperty.isOptional()) {
				return null;
			}

			String message = String.format("Found null for required property: '%s'", dataTypeProperty);
			throw new IllegalStateException(message);
		}

		Object result = attribute.valueOf(persistentInstance);

		if (dataTypeProperty.getType() == PrimitiveType.LOCAL_DATE) {
			return ((Date) result).toLocalDate();
		}

		if (dataTypeProperty.getType() == PrimitiveType.INSTANT) {
			return ((Timestamp) result).toInstant();
		}

		if (dataTypeProperty.isTemporalRange()) {
			Timestamp infinity = ((AsOfAttribute<?>) attribute).getInfinityDate();
			if (infinity.equals(result)) {
				return null;
			}
			return ((Timestamp) result).toInstant();
		}

		if (dataTypeProperty.isTemporalInstant()) {
			Timestamp infinity = ((TimestampAttribute<?>) attribute).getAsOfAttributeInfinity();
			if (infinity.equals(result)) {
				return null;
			}
			return ((Timestamp) result).toInstant();
		}

		if (dataTypeProperty instanceof EnumerationProperty enumerationProperty) {
			String prettyName = (String) result;
			Enumeration enumeration = enumerationProperty.getType();

			Optional<EnumerationLiteral> enumerationLiteral = enumeration
				.getEnumerationLiterals()
				.detectOptional((each) -> each.getPrettyName().equals(prettyName));

			return enumerationLiteral.orElseThrow(() ->
				new AssertionError("No enumeration literal found for " + prettyName + " in " + enumeration.getName())
			);
		}

		return result;
	}

	private boolean setDataTypePropertyViaAttribute(
		@Nonnull Object persistentInstance,
		@Nonnull DataTypeProperty dataTypeProperty,
		@Nullable Object newValue
	) {
		if (!(persistentInstance instanceof MithraObject mithraObject)) {
			String detailMessage = "Expected MithraObject but got " + persistentInstance.getClass().getCanonicalName();
			throw new AssertionError(detailMessage);
		}

		Attribute attribute = this.findAttribute(mithraObject, dataTypeProperty);

		Object oldValue = attribute.isAttributeNull(persistentInstance) ? null : attribute.valueOf(persistentInstance);
		if (Objects.equals(oldValue, newValue)) {
			return false;
		}

		if (newValue == null) {
			if (dataTypeProperty.isRequired()) {
				String message = String.format(
					"May not set required property to null: '%s.%s'",
					dataTypeProperty.getOwningClassifier().getName(),
					dataTypeProperty
				);
				throw new IllegalStateException(message);
			}
			attribute.setValueNull(persistentInstance);
		} else if (dataTypeProperty instanceof EnumerationProperty) {
			attribute.setValue(persistentInstance, ((EnumerationLiteral) newValue).getPrettyName());
		} else if (dataTypeProperty.getType() == PrimitiveType.LOCAL_DATE) {
			Timestamp timestamp = Timestamp.valueOf(((LocalDate) newValue).atStartOfDay());
			attribute.setValue(persistentInstance, timestamp);
		} else if (dataTypeProperty.getType() == PrimitiveType.INSTANT) {
			Timestamp timestamp = Timestamp.from((Instant) newValue);
			attribute.setValue(persistentInstance, timestamp);
		} else {
			attribute.setValue(persistentInstance, newValue);
		}

		return true;
	}

	@Nullable
	private Object getToOneViaFinder(Object persistentSourceInstance, ReferenceProperty referenceProperty) {
		Object result = this.getViaRelationshipFinder(persistentSourceInstance, referenceProperty);
		if (result instanceof List<?> list) {
			throw new AssertionError("Expected single object but got " + list.size());
		}
		return result;
	}

	@Nonnull
	private List<Object> getToManyViaFinder(Object persistentSourceInstance, ReferenceProperty referenceProperty) {
		Object result = this.getViaRelationshipFinder(persistentSourceInstance, referenceProperty);
		if (result == null) {
			return List.of();
		}
		if (!(result instanceof List)) {
			throw new AssertionError("Expected list but got " + result.getClass().getCanonicalName());
		}
		return (List<Object>) result;
	}

	private Object getViaRelationshipFinder(Object persistentSourceInstance, ReferenceProperty referenceProperty) {
		Classifier owningClassifier = referenceProperty.getOwningClassifier();
		RelatedFinder<?> finder = this.getRelatedFinder(owningClassifier);
		AbstractRelatedFinder relationshipFinder = (AbstractRelatedFinder) finder.getRelationshipFinderByName(
			referenceProperty.getName()
		);

		if (relationshipFinder == null) {
			String detailMessage =
				"Domain model and generated code are out of sync. Try rerunning a full clean build. Could not find relationship for property "
				+ referenceProperty;
			throw new AssertionError(detailMessage);
		}

		Object effectiveInstance = persistentSourceInstance;
		if (
			persistentSourceInstance instanceof MithraObject
			&& !this.isInstanceOf(persistentSourceInstance, owningClassifier)
		) {
			effectiveInstance = this.navigateToClassifierInstance(persistentSourceInstance, owningClassifier);
			if (effectiveInstance == null) {
				return null;
			}
		}

		return relationshipFinder.valueOf(effectiveInstance);
	}

	@Nullable
	private Attribute findAttribute(@Nonnull MithraObject mithraObject, @Nonnull DataTypeProperty dataTypeProperty) {
		RelatedFinder<?> concreteFinder = mithraObject.zGetPortal().getFinder();
		Attribute attribute = concreteFinder.getAttributeByName(dataTypeProperty.getName());
		if (attribute != null) {
			return attribute;
		}

		Classifier owningClassifier = dataTypeProperty.getOwningClassifier();
		if (owningClassifier instanceof Klass) {
			AbstractRelatedFinder ownerFinder = this.getRelatedFinder(owningClassifier);
			return ownerFinder.getAttributeByName(dataTypeProperty.getName());
		}

		return null;
	}

	// endregion

	// region Private helpers — reflection

	private Object getPropertyReflectively(@Nonnull Object persistentInstance, @Nonnull Property property) {
		try {
			Method method = this.getMethod(property);
			return method.invoke(persistentInstance);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Nonnull
	private Method getMethod(@Nonnull Property property) throws ClassNotFoundException, NoSuchMethodException {
		if (this.memoizedGetters.containsKey(property)) {
			return this.memoizedGetters.get(property);
		}
		Classifier owningClassifier = property.getOwningClassifier();
		String fullyQualifiedName = owningClassifier.getFullyQualifiedName();
		Class<?> aClass = Class.forName(fullyQualifiedName);
		String methodName = this.getMethodName(property);
		Method method = aClass.getMethod(methodName);
		this.memoizedGetters.put(property, method);
		return method;
	}

	@Nonnull
	private String getMethodName(Property property) {
		String prefix = property.getType() == PrimitiveType.BOOLEAN ? "is" : "get";
		String suffix = LOWER_TO_UPPER_CAMEL.convert(property.getName());
		return prefix + suffix;
	}

	// endregion
}
