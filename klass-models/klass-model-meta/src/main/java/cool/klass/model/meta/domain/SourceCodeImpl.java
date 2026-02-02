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

package cool.klass.model.meta.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.google.common.base.Splitter;
import cool.klass.model.meta.domain.AbstractElement.ElementBuilder;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.source.SourceCode;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public final class SourceCodeImpl implements SourceCode {

	@Nonnull
	private final String sourceName;

	@Nonnull
	private final String sourceCodeText;

	@Nonnull
	private final BufferedTokenStream tokenStream;

	@Nonnull
	private final ParserRuleContext parserContext;

	@Nonnull
	private final Optional<SourceCodeImpl> macroSourceCode;

	private Optional<Element> macroElement;

	public SourceCodeImpl(
		@Nonnull String sourceName,
		@Nonnull String sourceCodeText,
		@Nonnull BufferedTokenStream tokenStream,
		@Nonnull ParserRuleContext parserContext,
		@Nonnull Optional<SourceCodeImpl> macroSourceCode
	) {
		this.sourceName = Objects.requireNonNull(sourceName);
		this.sourceCodeText = Objects.requireNonNull(sourceCodeText);
		this.tokenStream = Objects.requireNonNull(tokenStream);
		this.parserContext = Objects.requireNonNull(parserContext);
		this.macroSourceCode = Objects.requireNonNull(macroSourceCode);
	}

	@Override
	@Nonnull
	public String getSourceName() {
		return this.sourceName;
	}

	@Nonnull
	@Override
	public String getFullPathSourceName() {
		if (this.macroElement.isEmpty()) {
			List<String> split = Splitter.on('/').splitToList(this.sourceName);
			return split.get(split.size() - 1);
		}

		String fullPathSourceName = this.macroSourceCode.map(SourceCode::getFullPathSourceName).orElseThrow();

		var abstractElement = (AbstractElement) this.macroElement.orElseThrow();
		Token startToken = abstractElement.getElementContext().getStart();

		return "%s:%d:%d --> %s".formatted(
			fullPathSourceName,
			startToken.getLine(),
			startToken.getCharPositionInLine(),
			this.sourceName
		);
	}

	@Override
	@Nonnull
	public String getSourceCodeText() {
		return this.sourceCodeText;
	}

	@Override
	@Nonnull
	public BufferedTokenStream getTokenStream() {
		return this.tokenStream;
	}

	@Override
	@Nonnull
	public ParserRuleContext getParserContext() {
		return this.parserContext;
	}

	@Nonnull
	@Override
	public Optional<SourceCode> getMacroSourceCode() {
		return this.macroSourceCode.map(Function.identity());
	}

	@Override
	public String toString() {
		return this.sourceName;
	}

	public void setMacroElement(Optional<Element> macroElement) {
		if (this.macroElement != null) {
			throw new IllegalStateException();
		}
		this.macroElement = macroElement;
	}

	public static final class SourceCodeBuilderImpl implements SourceCodeBuilder {

		@Nonnull
		private final String sourceName;

		@Nonnull
		private final String sourceCodeText;

		@Nonnull
		private final BufferedTokenStream tokenStream;

		@Nonnull
		private final ParserRuleContext parserContext;

		@Nonnull
		private final Optional<SourceCodeBuilderImpl> macroSourceCodeBuilder;

		private SourceCodeImpl sourceCode;
		private Optional<ElementBuilder<?>> macroElement;

		public SourceCodeBuilderImpl(
			@Nonnull String sourceName,
			@Nonnull String sourceCodeText,
			@Nonnull BufferedTokenStream tokenStream,
			@Nonnull ParserRuleContext parserContext,
			@Nonnull Optional<SourceCodeBuilderImpl> macroSourceCodeBuilder
		) {
			this.sourceName = Objects.requireNonNull(sourceName);
			this.sourceCodeText = Objects.requireNonNull(sourceCodeText);
			this.tokenStream = Objects.requireNonNull(tokenStream);
			this.parserContext = Objects.requireNonNull(parserContext);
			this.macroSourceCodeBuilder = Objects.requireNonNull(macroSourceCodeBuilder);
		}

		@Override
		public SourceCodeImpl build() {
			if (this.sourceCode == null) {
				this.sourceCode = new SourceCodeImpl(
					this.sourceName,
					this.sourceCodeText,
					this.tokenStream,
					this.parserContext,
					this.macroSourceCodeBuilder.map(SourceCodeBuilderImpl::build)
				);
			}
			return this.sourceCode;
		}

		public Optional<ElementBuilder<?>> getMacroElement() {
			return Objects.requireNonNull(this.macroElement);
		}

		public void setMacroElement(Optional<ElementBuilder<?>> macroElement) {
			if (this.macroElement != null) {
				throw new IllegalStateException();
			}
			this.macroElement = macroElement;
		}

		public void build2() {
			Optional<Element> element = this.macroElement.map(ElementBuilder::getElement);
			this.sourceCode.setMacroElement(element);
		}
	}
}
