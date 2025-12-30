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

package cool.klass.model.converter.compiler.state.criteria;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrElement;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.parameter.AntlrParameter;
import cool.klass.model.meta.domain.criteria.AbstractCriteria.AbstractCriteriaBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.map.OrderedMap;

public abstract class AntlrCriteria extends AntlrElement {

	@Nonnull
	private final IAntlrElement criteriaOwner;

	protected AntlrCriteria(
		@Nonnull ParserRuleContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		@Nonnull IAntlrElement criteriaOwner
	) {
		super(elementContext, compilationUnit);
		this.criteriaOwner = Objects.requireNonNull(criteriaOwner);
	}

	@Nonnull
	@Override
	public Optional<IAntlrElement> getSurroundingElement() {
		return Optional.of(this.criteriaOwner);
	}

	@Nonnull
	public abstract AbstractCriteriaBuilder<?> build();

	@Nonnull
	@Override
	public abstract AbstractCriteriaBuilder<?> getElementBuilder();

	public abstract void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder);

	public abstract void resolveServiceVariables(@Nonnull OrderedMap<String, AntlrParameter> formalParametersByName);

	public abstract void resolveTypes();

	public void addForeignKeys() {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".addForeignKeys() not implemented yet"
		);
	}

	public abstract void visit(AntlrCriteriaVisitor visitor);
}
