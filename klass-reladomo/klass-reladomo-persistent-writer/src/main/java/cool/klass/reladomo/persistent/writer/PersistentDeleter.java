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

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.data.store.DataStore;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
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
		this.deleteDependentChildren(klass, persistentInstance);

		// Collect all superclass instances before deleting anything,
		// because navigating relationships on deleted objects fails.
		List<Object> instancesToDelete = new java.util.ArrayList<>();
		instancesToDelete.add(persistentInstance);

		Klass superClass = klass;
		Object superInstance = persistentInstance;
		while (superClass.getSuperClass().isPresent()) {
			Object nextSuperInstance = this.dataStore.getSuperClass(superInstance, superClass);
			superClass = superClass.getSuperClass().get();
			this.deleteDependentChildren(superClass, nextSuperInstance);
			instancesToDelete.add(nextSuperInstance);
			superInstance = nextSuperInstance;
		}

		for (Object instance : instancesToDelete) {
			this.dataStore.deleteOrTerminate(instance);
		}
	}

	private void deleteDependentChildren(Klass klass, @Nonnull Object persistentInstance) {
		ImmutableList<AssociationEnd> associationEnds = klass.getAssociationEnds();

		for (AssociationEnd associationEnd : associationEnds) {
			if (!this.hasForeignKeyOnTargetSide(associationEnd)) {
				continue;
			}

			if (associationEnd.getMultiplicity().isToMany()) {
				List<Object> children = this.dataStore.getToMany(persistentInstance, associationEnd);
				Klass childKlass = associationEnd.getType();
				for (Object child : children) {
					Klass mostSpecificSubclass = this.dataStore.getMostSpecificSubclass(child, childKlass);
					this.deleteDependentChildren(mostSpecificSubclass, child);
					this.dataStore.deleteOrTerminate(child);
				}
			} else if (associationEnd.getMultiplicity().isToOne()) {
				Object child = this.dataStore.getToOne(persistentInstance, associationEnd);
				if (child != null) {
					Klass childKlass = associationEnd.getType();
					Klass mostSpecificSubclass = this.dataStore.getMostSpecificSubclass(child, childKlass);
					this.deleteDependentChildren(mostSpecificSubclass, child);
					this.dataStore.deleteOrTerminate(child);
				}
			}
		}
	}

	/**
	 * Determines if the target type of this association end has a foreign key
	 * pointing back to the source type. If so, deleting the source requires
	 * first deleting (or updating) the target to avoid FK constraint violations.
	 */
	private boolean hasForeignKeyOnTargetSide(@Nonnull AssociationEnd associationEnd) {
		Klass targetType = associationEnd.getType();
		return targetType
			.getDataTypeProperties()
			.anySatisfy(
				(property) ->
					property.isForeignKey()
					&& property.getKeysMatchingThisForeignKey().containsKey(associationEnd.getOpposite())
			);
	}
}
