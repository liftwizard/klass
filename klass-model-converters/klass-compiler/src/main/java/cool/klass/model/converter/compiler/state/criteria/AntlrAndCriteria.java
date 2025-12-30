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
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.meta.domain.criteria.AndCriteriaImpl.AndCriteriaBuilder;
import cool.klass.model.meta.grammar.KlassParser.CriteriaExpressionAndContext;

public class AntlrAndCriteria extends AntlrBinaryCriteria {

	private AndCriteriaBuilder elementBuilder;

	public AntlrAndCriteria(
		@Nonnull CriteriaExpressionAndContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		@Nonnull IAntlrElement criteriaOwner
	) {
		super(elementContext, compilationUnit, criteriaOwner);
	}

	@Nonnull
	@Override
	public CriteriaExpressionAndContext getElementContext() {
		return (CriteriaExpressionAndContext) super.getElementContext();
	}

	@Nonnull
	@Override
	public AndCriteriaBuilder build() {
		if (this.elementBuilder != null) {
			throw new IllegalStateException();
		}
		this.elementBuilder = new AndCriteriaBuilder(
			(CriteriaExpressionAndContext) this.elementContext,
			this.getMacroElementBuilder(),
			this.getSourceCodeBuilder(),
			this.left.build(),
			this.right.build()
		);
		return this.elementBuilder;
	}

	@Nonnull
	@Override
	public AndCriteriaBuilder getElementBuilder() {
		return Objects.requireNonNull(this.elementBuilder);
	}

	@Override
	public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		// TODO: Error if both clauses are identical, or if any left true subclause is a subclause of the right
		// Java | Probable bugs | Constant conditions & exceptions

		super.reportErrors(compilerAnnotationHolder);
	}

	@Override
	public void addForeignKeys() {
		this.left.addForeignKeys();
		this.right.addForeignKeys();
	}

	@Override
	public void visit(AntlrCriteriaVisitor visitor) {
		visitor.visitAnd(this);
		this.left.visit(visitor);
		this.right.visit(visitor);
	}
}
