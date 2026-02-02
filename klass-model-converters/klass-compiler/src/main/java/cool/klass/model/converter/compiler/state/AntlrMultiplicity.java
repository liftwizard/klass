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

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.grammar.KlassParser.MultiplicityContext;

public class AntlrMultiplicity extends AntlrElement {

	private final String lowerBoundText;
	private final String upperBoundText;

	@Nullable
	private final Multiplicity multiplicity;

	@Nullable
	private final AntlrMultiplicityOwner multiplicityOwner;

	public AntlrMultiplicity(
		@Nonnull MultiplicityContext context,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		@Nonnull AntlrMultiplicityOwner multiplicityOwner
	) {
		super(context, compilationUnit);
		this.lowerBoundText = context.multiplicityBody().lowerBound.getText();
		this.upperBoundText = context.multiplicityBody().upperBound.getText();

		this.multiplicity = this.findMultiplicity();

		this.multiplicityOwner = Objects.requireNonNull(multiplicityOwner);
	}

	@Nullable
	private Multiplicity findMultiplicity() {
		if (this.lowerBoundText.equals("0") && this.upperBoundText.equals("1")) {
			return Multiplicity.ZERO_TO_ONE;
		}
		if (this.lowerBoundText.equals("1") && this.upperBoundText.equals("1")) {
			return Multiplicity.ONE_TO_ONE;
		}
		if (this.lowerBoundText.equals("0") && this.upperBoundText.equals("*")) {
			return Multiplicity.ZERO_TO_MANY;
		}
		if (this.lowerBoundText.equals("1") && this.upperBoundText.equals("*")) {
			return Multiplicity.ONE_TO_MANY;
		}
		return null;
	}

	@Nonnull
	@Override
	public Optional<IAntlrElement> getSurroundingElement() {
		return Optional.ofNullable(this.multiplicityOwner);
	}

	@Nonnull
	@Override
	public MultiplicityContext getElementContext() {
		return (MultiplicityContext) super.getElementContext();
	}

	@Nullable
	public Multiplicity getMultiplicity() {
		return this.multiplicity;
	}

	public String getLowerBoundText() {
		return this.lowerBoundText;
	}

	public String getUpperBoundText() {
		return this.upperBoundText;
	}

	public boolean isToOne() {
		return this.multiplicity != null && this.multiplicity.isToOne();
	}

	public boolean isToMany() {
		return this.multiplicity != null && this.multiplicity.isToMany();
	}

	@Override
	public String toString() {
		return this.lowerBoundText + ".." + this.upperBoundText;
	}
}
