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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gs.fw.common.mithra.finder.AbstractRelatedFinder;
import com.gs.fw.common.mithra.finder.Operation;
import com.gs.fw.common.mithra.finder.RelatedFinder;
import cool.klass.data.store.DataStore;
import cool.klass.data.store.TransactionalCommand;
import cool.klass.data.store.reladomo.OperationVisitor;
import cool.klass.data.store.reladomo.ReladomoDataStore;
import cool.klass.model.lens.ClassLens;
import cool.klass.model.lens.DataTypeLens;
import cool.klass.model.lens.LensRegistry;
import cool.klass.model.lens.reladomo.ReladomoAssociationLens;
import cool.klass.model.lens.reladomo.ReladomoClassLens;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import cool.klass.model.meta.domain.api.visitor.AssertObjectMatchesDataTypePropertyVisitor;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MapIterable;

/**
 * A DataStore that uses lenses for property access and Reladomo for persistence.
 * Delegates non-lens operations (transactions, insert, delete, etc.) to a ReladomoDataStore.
 */
public class ReladomoLensDataStore implements DataStore {

	private final ReladomoDataStore delegate;
	private final LensRegistry lensRegistry;

	public ReladomoLensDataStore(@Nonnull ReladomoDataStore delegate, @Nonnull LensRegistry lensRegistry) {
		this.delegate = Objects.requireNonNull(delegate);
		this.lensRegistry = Objects.requireNonNull(lensRegistry);
	}

	@Nonnull
	public ReladomoDataStore getDelegate() {
		return this.delegate;
	}

	// region Lens-based operations

	@Override
	@Nonnull
	public Object instantiate(@Nonnull Klass klass, @Nonnull MapIterable<DataTypeProperty, Object> keys) {
		if (klass.isAbstract()) {
			return this.delegate.instantiate(klass, keys);
		}

		keys.each(Objects::requireNonNull);

		ReladomoClassLens<?> reladomoLens = (ReladomoClassLens<?>) this.lensRegistry.getClassLens(klass);
		Object newInstance = reladomoLens.instantiate();

		keys.forEachKeyValue((property, value) -> {
			DataTypeLens<Object, Object> lens = (DataTypeLens<Object, Object>) reladomoLens.getLensByProperty(property);
			lens.set(newInstance, value);
		});

		return newInstance;
	}

	@Override
	@Nonnull
	public List<Object> findAll(@Nonnull Klass klass) {
		if (klass.isAbstract()) {
			return this.delegate.findAll(klass);
		}

		ReladomoClassLens<?> reladomoLens = (ReladomoClassLens<?>) this.lensRegistry.getClassLens(klass);
		RelatedFinder<?> finder = reladomoLens.getRelatedFinder();
		return (List<Object>) finder.findMany(finder.all());
	}

	@Override
	@Nullable
	public Object findByKey(@Nonnull Klass klass, @Nonnull MapIterable<DataTypeProperty, Object> keys) {
		if (klass.isAbstract()) {
			return this.delegate.findByKey(klass, keys);
		}

		ReladomoClassLens<?> reladomoLens = (ReladomoClassLens<?>) this.lensRegistry.getClassLens(klass);
		Operation operation = this.buildFindByKeyOperation(reladomoLens, klass, keys);
		RelatedFinder<?> finder = reladomoLens.getRelatedFinder();
		return finder.findOne(operation);
	}

	@Override
	@Nonnull
	public List<Object> findByKeyReturningList(
		@Nonnull Klass klass,
		@Nonnull MapIterable<DataTypeProperty, Object> keys
	) {
		if (klass.isAbstract()) {
			return this.delegate.findByKeyReturningList(klass, keys);
		}

		ReladomoClassLens<?> reladomoLens = (ReladomoClassLens<?>) this.lensRegistry.getClassLens(klass);
		Operation operation = this.buildFindByKeyOperation(reladomoLens, klass, keys);
		RelatedFinder<?> finder = reladomoLens.getRelatedFinder();
		return (List<Object>) finder.findMany(operation);
	}

	@Override
	@Nullable
	public Object getDataTypeProperty(@Nonnull Object persistentInstance, @Nonnull DataTypeProperty dataTypeProperty) {
		if (
			dataTypeProperty.isDerived()
			|| !(dataTypeProperty.getOwningClassifier() instanceof Klass klass)
			|| klass.isAbstract()
		) {
			return this.delegate.getDataTypeProperty(persistentInstance, dataTypeProperty);
		}

		ClassLens<?> classLens = this.lensRegistry.getClassLens(klass);
		DataTypeLens<Object, ?> dataTypeLens = (DataTypeLens<Object, ?>) classLens.getLensByProperty(dataTypeProperty);
		Object result = dataTypeLens.get(persistentInstance);

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
		if (
			dataTypeProperty.isDerived()
			|| !(dataTypeProperty.getOwningClassifier() instanceof Klass klass)
			|| klass.isAbstract()
		) {
			return this.delegate.setDataTypeProperty(persistentInstance, dataTypeProperty, newValue);
		}

		ClassLens<?> classLens = this.lensRegistry.getClassLens(klass);
		DataTypeLens<Object, Object> dataTypeLens = (DataTypeLens<Object, Object>) classLens.getLensByProperty(
			dataTypeProperty
		);
		Object oldValue = dataTypeLens.get(persistentInstance);
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

		dataTypeLens.set(persistentInstance, newValue);
		return true;
	}

	@Override
	@Nullable
	public Object getToOne(@Nonnull Object persistentSourceInstance, @Nonnull ReferenceProperty referenceProperty) {
		if (
			!(referenceProperty instanceof AssociationEnd associationEnd)
			|| !(associationEnd.getOwningClassifier() instanceof Klass klass)
			|| klass.isAbstract()
		) {
			return this.delegate.getToOne(persistentSourceInstance, referenceProperty);
		}

		ClassLens<?> classLens = this.lensRegistry.getClassLens(klass);
		ReladomoAssociationLens<Object, ?> associationLens = (ReladomoAssociationLens<
			Object,
			?
		>) classLens.getLensByProperty(associationEnd);
		AbstractRelatedFinder relationshipFinder = associationLens.getRelationshipFinder();
		Object result = relationshipFinder.valueOf(persistentSourceInstance);
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
		if (
			!(referenceProperty instanceof AssociationEnd associationEnd)
			|| !(associationEnd.getOwningClassifier() instanceof Klass klass)
			|| klass.isAbstract()
		) {
			return this.delegate.getToMany(persistentSourceInstance, referenceProperty);
		}

		ClassLens<?> classLens = this.lensRegistry.getClassLens(klass);
		ReladomoAssociationLens<Object, ?> associationLens = (ReladomoAssociationLens<
			Object,
			?
		>) classLens.getLensByProperty(associationEnd);
		AbstractRelatedFinder relationshipFinder = associationLens.getRelationshipFinder();
		Object result = relationshipFinder.valueOf(persistentSourceInstance);
		if (result == null) {
			return List.of();
		}
		if (!(result instanceof List)) {
			throw new IllegalStateException("Expected list but got " + result.getClass().getCanonicalName());
		}
		return (List<Object>) result;
	}

	// endregion

	// region Delegated to ReladomoDataStore

	@Override
	public <Result> Result runInTransaction(TransactionalCommand<Result> transactionalCommand) {
		return this.delegate.runInTransaction(transactionalCommand);
	}

	@Override
	public void runInTransaction(Runnable runnable) {
		this.delegate.runInTransaction(runnable);
	}

	@Override
	public void insert(@Nonnull Object persistentInstance) {
		this.delegate.insert(persistentInstance);
	}

	@Override
	public boolean setToOne(
		@Nonnull Object persistentSourceInstance,
		@Nonnull AssociationEnd associationEnd,
		@Nullable Object persistentTargetInstance
	) {
		return this.delegate.setToOne(persistentSourceInstance, associationEnd, persistentTargetInstance);
	}

	@Override
	public void deleteOrTerminate(@Nonnull Object persistentInstance) {
		this.delegate.deleteOrTerminate(persistentInstance);
	}

	@Override
	public void purgeAll(@Nonnull Klass klass) {
		this.delegate.purgeAll(klass);
	}

	@Override
	public boolean isInstanceOf(@Nonnull Object persistentInstance, @Nonnull Classifier classifier) {
		return this.delegate.isInstanceOf(persistentInstance, classifier);
	}

	@Override
	@Nonnull
	public Klass getMostSpecificSubclass(@Nonnull Object persistentInstance, @Nonnull Klass klass) {
		return this.delegate.getMostSpecificSubclass(persistentInstance, klass);
	}

	@Override
	@Nullable
	public Object getSuperClass(@Nonnull Object persistentInstance, @Nonnull Klass klass) {
		return this.delegate.getSuperClass(persistentInstance, klass);
	}

	@Override
	@Nullable
	public Object getSubClass(@Nonnull Object persistentInstance, @Nonnull Klass superClass, @Nonnull Klass subClass) {
		return this.delegate.getSubClass(persistentInstance, superClass, subClass);
	}

	// endregion

	// region Private helpers

	@Nonnull
	private Operation buildFindByKeyOperation(
		@Nonnull ReladomoClassLens<?> reladomoLens,
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

	// endregion
}
