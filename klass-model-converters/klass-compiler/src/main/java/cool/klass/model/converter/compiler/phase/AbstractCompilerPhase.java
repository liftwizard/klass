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

package cool.klass.model.converter.compiler.phase;

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.parser.DelegatingKlassListener;
import cool.klass.model.meta.grammar.KlassListener;

@SuppressWarnings("AbstractClassExtendsConcreteClass")
public abstract class AbstractCompilerPhase extends DelegatingKlassListener {

	@Nonnull
	protected final CompilerState compilerState;

	private final KlassListener delegate;

	protected AbstractCompilerPhase(@Nonnull CompilerState compilerState) {
		this.compilerState = Objects.requireNonNull(compilerState);
		this.delegate = compilerState.asListener();
	}

	@Override
	protected KlassListener getDelegate() {
		return this.delegate;
	}

	@Nonnull
	public String getName() {
		throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".getName() not implemented yet");
	}
}
