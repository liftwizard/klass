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

package cool.klass.model.meta.domain.criteria;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.AbstractElement;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.criteria.Criteria;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import org.antlr.v4.runtime.ParserRuleContext;

public abstract class AbstractCriteria extends AbstractElement implements Criteria {

	protected AbstractCriteria(
		@Nonnull ParserRuleContext elementContext,
		@Nonnull Optional<Element> macroElement,
		@Nullable SourceCode sourceCode
	) {
		super(elementContext, macroElement, sourceCode);
	}

	public abstract static class AbstractCriteriaBuilder<BuiltElement extends AbstractCriteria>
		extends ElementBuilder<BuiltElement> {

		protected AbstractCriteriaBuilder(
			@Nonnull ParserRuleContext elementContext,
			@Nonnull Optional<ElementBuilder<?>> macroElement,
			@Nullable SourceCodeBuilder sourceCode
		) {
			super(elementContext, macroElement, sourceCode);
		}
	}
}
