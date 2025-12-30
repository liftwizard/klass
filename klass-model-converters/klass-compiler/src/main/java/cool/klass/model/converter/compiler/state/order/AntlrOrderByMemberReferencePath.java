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

package cool.klass.model.converter.compiler.state.order;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrElement;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.value.AntlrThisMemberReferencePath;
import cool.klass.model.meta.domain.order.OrderByMemberReferencePathImpl.OrderByMemberReferencePathBuilder;
import cool.klass.model.meta.domain.value.ThisMemberReferencePathImpl.ThisMemberReferencePathBuilder;
import cool.klass.model.meta.grammar.KlassParser.OrderByMemberReferencePathContext;

public class AntlrOrderByMemberReferencePath extends AntlrElement {

	@Nonnull
	private final AntlrOrderBy orderBy;

	private final int ordinal;

	@Nonnull
	private final AntlrThisMemberReferencePath thisMemberReferencePath;

	@Nullable
	private AntlrOrderByDirection orderByDirection;

	private OrderByMemberReferencePathBuilder elementBuilder;

	public AntlrOrderByMemberReferencePath(
		@Nonnull OrderByMemberReferencePathContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		@Nonnull AntlrOrderBy orderBy,
		int ordinal,
		@Nonnull AntlrThisMemberReferencePath thisMemberReferencePath
	) {
		super(elementContext, compilationUnit);
		this.orderBy = Objects.requireNonNull(orderBy);
		this.ordinal = ordinal;
		this.thisMemberReferencePath = Objects.requireNonNull(thisMemberReferencePath);
	}

	@Nonnull
	@Override
	public Optional<IAntlrElement> getSurroundingElement() {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".getSurroundingContext() not implemented yet"
		);
	}

	public int getOrdinal() {
		return this.ordinal;
	}

	public void enterOrderByDirection(@Nonnull AntlrOrderByDirection orderByDirection) {
		this.orderByDirection = Objects.requireNonNull(orderByDirection);
	}

	@Nullable
	public AntlrOrderByDirection getOrderByDirection() {
		return this.orderByDirection;
	}

	@Nonnull
	public AntlrThisMemberReferencePath getThisMemberReferencePath() {
		return this.thisMemberReferencePath;
	}

	public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		// TODO: ❗️ Redo context stack for error reporting
		this.thisMemberReferencePath.reportErrors(compilerAnnotationHolder);
	}

	@Nonnull
	public OrderByMemberReferencePathBuilder build() {
		if (this.elementBuilder != null) {
			throw new IllegalStateException();
		}
		ThisMemberReferencePathBuilder thisMemberReferencePathBuilder = this.thisMemberReferencePath.build();

		this.elementBuilder = new OrderByMemberReferencePathBuilder(
			(OrderByMemberReferencePathContext) this.elementContext,
			this.getMacroElementBuilder(),
			this.getSourceCodeBuilder(),
			this.orderBy.getElementBuilder(),
			this.ordinal,
			thisMemberReferencePathBuilder
		);

		this.elementBuilder.setOrderByDirectionBuilder(this.orderByDirection.build());
		return this.elementBuilder;
	}

	@Override
	@Nonnull
	public OrderByMemberReferencePathBuilder getElementBuilder() {
		return Objects.requireNonNull(this.elementBuilder);
	}
}
