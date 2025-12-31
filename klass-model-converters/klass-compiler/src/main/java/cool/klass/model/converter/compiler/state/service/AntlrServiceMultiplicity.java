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

package cool.klass.model.converter.compiler.state.service;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.state.AntlrElement;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.meta.domain.api.service.ServiceMultiplicity;
import cool.klass.model.meta.grammar.KlassParser.ServiceMultiplicityContext;

public class AntlrServiceMultiplicity extends AntlrElement {

	@Nonnull
	private final ServiceMultiplicity serviceMultiplicity;

	public AntlrServiceMultiplicity(
		@Nonnull ServiceMultiplicityContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		@Nonnull ServiceMultiplicity serviceMultiplicity
	) {
		super(elementContext, compilationUnit);
		this.serviceMultiplicity = Objects.requireNonNull(serviceMultiplicity);
	}

	@Nonnull
	@Override
	public Optional<IAntlrElement> getSurroundingElement() {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".getSurroundingContext() not implemented yet"
		);
	}

	@Nonnull
	public ServiceMultiplicity getServiceMultiplicity() {
		return this.serviceMultiplicity;
	}
}
