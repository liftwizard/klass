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

package cool.klass.model.meta.domain.criteria;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.criteria.BinaryCriteria;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import org.antlr.v4.runtime.ParserRuleContext;

public abstract class AbstractBinaryCriteria extends AbstractCriteria implements BinaryCriteria {

	@Nonnull
	protected final AbstractCriteria left;

	@Nonnull
	protected final AbstractCriteria right;

	protected AbstractBinaryCriteria(
		@Nonnull ParserRuleContext elementContext,
		@Nonnull Optional<Element> macroElement,
		@Nullable SourceCode sourceCode,
		@Nonnull AbstractCriteria left,
		@Nonnull AbstractCriteria right
	) {
		super(elementContext, macroElement, sourceCode);
		this.left = Objects.requireNonNull(left);
		this.right = Objects.requireNonNull(right);
	}

	@Override
	@Nonnull
	public AbstractCriteria getLeft() {
		return this.left;
	}

	@Override
	@Nonnull
	public AbstractCriteria getRight() {
		return this.right;
	}

	public abstract static class AbstractBinaryCriteriaBuilder<BuiltElement extends AbstractBinaryCriteria>
		extends AbstractCriteriaBuilder<BuiltElement> {

		@Nonnull
		protected final AbstractCriteriaBuilder<?> left;

		@Nonnull
		protected final AbstractCriteriaBuilder<?> right;

		protected AbstractBinaryCriteriaBuilder(
			@Nonnull ParserRuleContext elementContext,
			@Nonnull Optional<ElementBuilder<?>> macroElement,
			@Nullable SourceCodeBuilder sourceCode,
			@Nonnull AbstractCriteriaBuilder<?> left,
			@Nonnull AbstractCriteriaBuilder<?> right
		) {
			super(elementContext, macroElement, sourceCode);
			this.left = Objects.requireNonNull(left);
			this.right = Objects.requireNonNull(right);
		}
	}
}
