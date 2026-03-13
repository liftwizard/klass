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

package cool.klass.reladomo.persistent.writer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.data.store.DataStore;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import org.eclipse.collections.api.list.ImmutableList;

public class PersistentDeleter {

	@Nonnull
	private final MutationContext mutationContext;

	private final DataStore dataStore;

	public PersistentDeleter(@Nonnull MutationContext mutationContext, @Nonnull DataStore dataStore) {
		this.mutationContext = Objects.requireNonNull(mutationContext);
		this.dataStore = Objects.requireNonNull(dataStore);
	}

	public void deleteOrTerminate(Klass klass, @Nonnull Object persistentInstance) {
		this.dataStore.deleteOrTerminate(persistentInstance);
	}

	public void deleteAllOrTerminate(Klass klass, @Nonnull List<?> persistentInstances) {
		if (persistentInstances.isEmpty()) {
			return;
		}

		ImmutableList<AssociationEnd> ownedEnds = klass.getAssociationEnds().select(ReferenceProperty::isOwned);

		// Recursively delete to-many owned children first (FK is on child side)
		for (AssociationEnd ownedEnd : ownedEnds.select((end) -> end.getMultiplicity().isToMany())) {
			List<Object> allChildren = new ArrayList<>();
			for (Object instance : persistentInstances) {
				Object resolved = this.resolveInstanceForEnd(instance, klass, ownedEnd);
				allChildren.addAll(this.dataStore.getToMany(resolved, ownedEnd));
			}
			this.deleteAllOrTerminate(ownedEnd.getType(), allChildren);
		}

		// Delete non-owned to-many references that would block deletion of these instances
		for (AssociationEnd nonOwnedEnd : klass
			.getAssociationEnds()
			.reject(ReferenceProperty::isOwned)
			.select((end) -> end.getMultiplicity().isToMany())) {
			for (Object instance : persistentInstances) {
				Object resolved = this.resolveInstanceForEnd(instance, klass, nonOwnedEnd);
				List<Object> references = this.dataStore.getToMany(resolved, nonOwnedEnd);
				for (int i = references.size() - 1; i >= 0; i--) {
					this.dataStore.deleteOrTerminate(references.get(i));
				}
			}
		}

		// Collect to-one owned children before deleting parents
		List<List<Object>> toOneChildGroups = new ArrayList<>();
		List<AssociationEnd> toOneEnds = new ArrayList<>();
		for (AssociationEnd ownedEnd : ownedEnds.reject((end) -> end.getMultiplicity().isToMany())) {
			List<Object> allChildren = new ArrayList<>();
			for (Object instance : persistentInstances) {
				Object resolved = this.resolveInstanceForEnd(instance, klass, ownedEnd);
				Object child = this.dataStore.getToOne(resolved, ownedEnd);
				if (child != null) {
					allChildren.add(child);
				}
			}
			toOneChildGroups.add(allChildren);
			toOneEnds.add(ownedEnd);
		}

		// Delete the instances themselves
		for (int i = persistentInstances.size() - 1; i >= 0; i--) {
			this.dataStore.delete(persistentInstances.get(i));
		}

		// Delete to-one owned children (FK was on parent side, now removed)
		for (int j = 0; j < toOneEnds.size(); j++) {
			this.deleteAllOrTerminate(toOneEnds.get(j).getType(), toOneChildGroups.get(j));
		}
	}

	private Object resolveInstanceForEnd(Object instance, Klass klass, AssociationEnd end) {
		Classifier owningClassifier = end.getOwningClassifier();
		Object current = instance;
		Klass currentKlass = klass;
		while (!currentKlass.equals(owningClassifier) && currentKlass.getSuperClass().isPresent()) {
			current = this.dataStore.getSuperClass(current, currentKlass);
			currentKlass = currentKlass.getSuperClass().get();
		}
		return current;
	}
}
