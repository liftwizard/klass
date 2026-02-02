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

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.data.store.DataStore;
import cool.klass.model.meta.domain.api.Klass;

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
}
