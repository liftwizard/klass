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
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;

public class PersistentDeleter {

	@Nonnull
	private final MutationContext mutationContext;

	private final DataStore dataStore;

	public PersistentDeleter(@Nonnull MutationContext mutationContext, @Nonnull DataStore dataStore) {
		this.mutationContext = Objects.requireNonNull(mutationContext);
		this.dataStore = Objects.requireNonNull(dataStore);
	}

	public void deleteOrTerminate(Klass klass, @Nonnull Object persistentInstance) {
		List<OrphanedChild> orphanedChildren = new ArrayList<>();
		this.collectOrphanedChildrenRecursive(klass, persistentInstance, orphanedChildren);

		this.dataStore.deleteOrTerminate(persistentInstance);

		for (OrphanedChild orphan : orphanedChildren) {
			this.deleteOrTerminate(orphan.klass(), orphan.instance());
		}
	}

	private void collectOrphanedChildrenRecursive(
		Klass klass,
		Object persistentInstance,
		List<OrphanedChild> orphanedChildren
	) {
		this.collectOrphanedChildrenAtLevel(klass, persistentInstance, orphanedChildren);

		for (Klass subClass : klass.getSubClasses()) {
			Object subClassInstance = this.dataStore.getSubClass(persistentInstance, klass, subClass);
			if (subClassInstance != null) {
				this.collectOrphanedChildrenRecursive(subClass, subClassInstance, orphanedChildren);
			}
		}
	}

	private void collectOrphanedChildrenAtLevel(
		Klass klass,
		Object persistentInstance,
		List<OrphanedChild> orphanedChildren
	) {
		var fkEnds = klass.getForeignKeys().keysView().toSet();

		for (AssociationEnd ownedEnd : klass.getAssociationEnds().select(ReferenceProperty::isOwned)) {
			if (ownedEnd.getOwningClassifier() != klass) {
				continue;
			}
			if (fkEnds.contains(ownedEnd)) {
				if (ownedEnd.getMultiplicity().isToOne()) {
					Object child = this.dataStore.getToOne(persistentInstance, ownedEnd);
					if (child != null) {
						orphanedChildren.add(new OrphanedChild(ownedEnd.getType(), child));
					}
				} else {
					List<Object> children = this.dataStore.getToMany(persistentInstance, ownedEnd);
					for (Object child : children) {
						orphanedChildren.add(new OrphanedChild(ownedEnd.getType(), child));
					}
				}
			} else {
				if (ownedEnd.getMultiplicity().isToOne()) {
					Object child = this.dataStore.getToOne(persistentInstance, ownedEnd);
					if (child != null) {
						this.collectOrphanedChildrenRecursive(ownedEnd.getType(), child, orphanedChildren);
					}
				} else {
					List<Object> children = this.dataStore.getToMany(persistentInstance, ownedEnd);
					for (Object child : children) {
						this.collectOrphanedChildrenRecursive(ownedEnd.getType(), child, orphanedChildren);
					}
				}
			}
		}
	}

	private record OrphanedChild(Klass klass, Object instance) {}
}
