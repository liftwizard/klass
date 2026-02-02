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

package cool.klass.model.converter.compiler.state;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Type.TypeGetter;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

// All AntlrTypes are named elements, but the interface doesn't exist yet
public interface AntlrType extends IAntlrElement {
	String getName();

	TypeGetter getTypeGetter();

	@Nonnull
	TypeGetter getElementBuilder();

	default ImmutableList<AntlrType> getPotentialWiderTypes() {
		return Lists.immutable.with(this);
	}
}
