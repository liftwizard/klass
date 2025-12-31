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

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.order.AntlrOrderByDirection;
import cool.klass.model.converter.compiler.state.order.AntlrOrderByMemberReferencePath;
import cool.klass.model.meta.domain.api.order.OrderByDirection;
import cool.klass.model.meta.grammar.KlassParser.OrderByDirectionContext;

public class OrderByDirectionPhase extends AbstractCompilerPhase {

	public OrderByDirectionPhase(@Nonnull CompilerState compilerState) {
		super(compilerState);
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public void enterOrderByDirection(@Nonnull OrderByDirectionContext ctx) {
		super.enterOrderByDirection(ctx);

		AntlrOrderByDirection orderByDirection = new AntlrOrderByDirection(
			ctx,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			OrderByDirectionPhase.getOrderByDirection(ctx)
		);

		AntlrOrderByMemberReferencePath orderByMemberReferencePath =
			this.compilerState.getCompilerWalk().getOrderByMemberReferencePath();
		orderByMemberReferencePath.enterOrderByDirection(orderByDirection);
	}

	@Nonnull
	private static OrderByDirection getOrderByDirection(@Nonnull OrderByDirectionContext orderByDirectionContext) {
		String text = orderByDirectionContext.getText();

		if ("ascending".equals(text)) {
			return OrderByDirection.ASCENDING;
		}

		if ("descending".equals(text)) {
			return OrderByDirection.DESCENDING;
		}

		throw new AssertionError(text);
	}
}
