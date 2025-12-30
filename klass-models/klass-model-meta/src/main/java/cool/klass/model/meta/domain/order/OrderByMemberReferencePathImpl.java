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

package cool.klass.model.meta.domain.order;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.AbstractElement;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.order.OrderByMemberReferencePath;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.order.OrderByDirectionDeclarationImpl.OrderByDirectionDeclarationBuilder;
import cool.klass.model.meta.domain.order.OrderByImpl.OrderByBuilder;
import cool.klass.model.meta.domain.value.ThisMemberReferencePathImpl;
import cool.klass.model.meta.domain.value.ThisMemberReferencePathImpl.ThisMemberReferencePathBuilder;
import cool.klass.model.meta.grammar.KlassParser.OrderByMemberReferencePathContext;

public final class OrderByMemberReferencePathImpl extends AbstractElement implements OrderByMemberReferencePath {

	@Nonnull
	private final OrderByImpl orderBy;

	private final int ordinal;

	@Nonnull
	private final ThisMemberReferencePathImpl thisMemberReferencePath;

	@Nullable
	private OrderByDirectionDeclarationImpl orderByDirectionDeclaration;

	private OrderByMemberReferencePathImpl(
		@Nonnull OrderByMemberReferencePathContext elementContext,
		@Nonnull Optional<Element> macroElement,
		@Nullable SourceCode sourceCode,
		@Nonnull OrderByImpl orderBy,
		int ordinal,
		@Nonnull ThisMemberReferencePathImpl thisMemberReferencePath
	) {
		super(elementContext, macroElement, sourceCode);
		this.orderBy = Objects.requireNonNull(orderBy);
		this.ordinal = ordinal;
		this.thisMemberReferencePath = Objects.requireNonNull(thisMemberReferencePath);
	}

	@Nonnull
	@Override
	public OrderByMemberReferencePathContext getElementContext() {
		return (OrderByMemberReferencePathContext) super.getElementContext();
	}

	@Override
	@Nonnull
	public ThisMemberReferencePathImpl getThisMemberReferencePath() {
		return this.thisMemberReferencePath;
	}

	@Override
	@Nonnull
	public OrderByDirectionDeclarationImpl getOrderByDirectionDeclaration() {
		return this.orderByDirectionDeclaration;
	}

	public void setOrderByDirectionDeclaration(@Nonnull OrderByDirectionDeclarationImpl orderByDirectionDeclaration) {
		this.orderByDirectionDeclaration = Objects.requireNonNull(orderByDirectionDeclaration);
	}

	public static final class OrderByMemberReferencePathBuilder extends ElementBuilder<OrderByMemberReferencePathImpl> {

		@Nonnull
		private final OrderByBuilder orderByBuilder;

		private final int ordinal;

		@Nonnull
		private final ThisMemberReferencePathBuilder thisMemberReferencePathBuilder;

		@Nullable
		private OrderByDirectionDeclarationBuilder orderByDirectionBuilder;

		public OrderByMemberReferencePathBuilder(
			@Nonnull OrderByMemberReferencePathContext elementContext,
			@Nonnull Optional<ElementBuilder<?>> macroElement,
			@Nullable SourceCodeBuilder sourceCode,
			@Nonnull OrderByBuilder orderByBuilder,
			int ordinal,
			@Nonnull ThisMemberReferencePathBuilder thisMemberReferencePathBuilder
		) {
			super(elementContext, macroElement, sourceCode);
			this.orderByBuilder = Objects.requireNonNull(orderByBuilder);
			this.ordinal = ordinal;
			this.thisMemberReferencePathBuilder = Objects.requireNonNull(thisMemberReferencePathBuilder);
		}

		public void setOrderByDirectionBuilder(@Nonnull OrderByDirectionDeclarationBuilder orderByDirectionBuilder) {
			this.orderByDirectionBuilder = Objects.requireNonNull(orderByDirectionBuilder);
		}

		@Override
		@Nonnull
		protected OrderByMemberReferencePathImpl buildUnsafe() {
			return new OrderByMemberReferencePathImpl(
				(OrderByMemberReferencePathContext) this.elementContext,
				this.macroElement.map(ElementBuilder::getElement),
				this.sourceCode.build(),
				this.orderByBuilder.getElement(),
				this.ordinal,
				this.thisMemberReferencePathBuilder.build()
			);
		}

		@Override
		protected void buildChildren() {
			this.element.setOrderByDirectionDeclaration(this.orderByDirectionBuilder.build());
		}
	}
}
