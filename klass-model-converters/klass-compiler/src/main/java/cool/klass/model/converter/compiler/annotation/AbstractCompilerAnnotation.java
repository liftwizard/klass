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

package cool.klass.model.converter.compiler.annotation;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.AnsiTokenColorizer;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCompilerAnnotation {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCompilerAnnotation.class);

	private static final Comparator<Token> TOKEN_COMPARATOR = Comparator.comparing(Token::getLine).thenComparing(
		Token::getCharPositionInLine
	);

	@Nonnull
	protected final CompilationUnit compilationUnit;

	@Nonnull
	protected final AnnotationSeverity severity;

	@Nonnull
	private final Optional<CauseCompilerAnnotation> macroCause;

	// TODO: Change type of offendingContexts to also be SourceContexts
	@Nonnull
	private final ImmutableList<ParserRuleContext> offendingContexts;

	@Nonnull
	private final ImmutableList<IAntlrElement> sourceContexts;

	@Nonnull
	private final AnsiTokenColorizer ansiTokenColorizer;

	protected AbstractCompilerAnnotation(
		@Nonnull CompilationUnit compilationUnit,
		@Nonnull Optional<CauseCompilerAnnotation> macroCause,
		@Nonnull ImmutableList<ParserRuleContext> offendingContexts,
		@Nonnull ImmutableList<IAntlrElement> sourceContexts,
		@Nonnull AnsiTokenColorizer ansiTokenColorizer,
		@Nonnull AnnotationSeverity severity
	) {
		this.macroCause = Objects.requireNonNull(macroCause);
		this.compilationUnit = Objects.requireNonNull(compilationUnit);
		this.offendingContexts = Objects.requireNonNull(offendingContexts);
		this.sourceContexts = Objects.requireNonNull(sourceContexts).select(IAntlrElement::isContext);
		this.ansiTokenColorizer = Objects.requireNonNull(ansiTokenColorizer);
		this.severity = Objects.requireNonNull(severity);

		if (offendingContexts.isEmpty()) {
			throw new AssertionError();
		}
		if (offendingContexts.anySatisfy((offendingContext) -> offendingContext.getStart() == null)) {
			throw new AssertionError();
		}

		this.applyListenerToStack();
	}

	@Nonnull
	protected CompilationUnit getCompilationUnit() {
		return this.compilationUnit;
	}

	public boolean isError() {
		return this.severity == AnnotationSeverity.ERROR;
	}

	public boolean isWarning() {
		return this.severity == AnnotationSeverity.WARNING;
	}

	@Nonnull
	public AnnotationSeverity getSeverity() {
		return this.severity;
	}

	protected String getContextString() {
		ImmutableList<AbstractContextString> contextStrings = this.applyListenerToStack();

		int maxLine = contextStrings.getLast().getLine();
		String maxLineString = String.valueOf(maxLine);
		int lineNumberWidth = maxLineString.length();

		String entireContext = contextStrings
			.collect((contextString) -> contextString.toString(lineNumberWidth))
			.makeString("", "\n", "\n");
		return Ansi.ansi().a(entireContext).reset().toString();
	}

	@Nonnull
	protected String getFilenameWithoutDirectory() {
		String sourceName = this.compilationUnit.getSourceName();
		return this.macroCause.map((ignore) -> sourceName).orElseGet(() ->
			sourceName.substring(sourceName.lastIndexOf('/') + 1)
		);
	}

	protected String getShortLocationString() {
		return this.macroCause.map((ignore) -> this.getLocationWithoutLine()).orElseGet(this::getLocationWithLine);
	}

	private String getLocationWithoutLine() {
		return String.format("(%s)", this.compilationUnit);
	}

	private String getLocationWithLine() {
		return String.format(
			"(%s:%d)",
			// TODO: This should be part of the source information
			this.getFilenameWithoutDirectory(),
			this.getLine()
		);
	}

	protected ImmutableList<AbstractContextString> applyListenerToStack() {
		SetIterable<Token> contextTokens = this.getContextTokens();

		MutableSet<Token> underlinedTokens = this.offendingContexts.asLazy()
			.flatCollect(this::getUnderlinedTokenRange)
			.into(SetAdapter.adapt(new LinkedHashSet<>()));

		MutableSet<Integer> contextLines = contextTokens
			.asLazy()
			.collect(Token::getLine)
			.into(SetAdapter.adapt(new LinkedHashSet<>()));

		MutableSet<Integer> underlinedLines = underlinedTokens
			.asLazy()
			.collect(Token::getLine)
			.into(SetAdapter.adapt(new LinkedHashSet<>()));

		if (!contextTokens.containsAll(underlinedTokens)) {
			String message = this.offendingContexts.asLazy()
				.flatCollect(this::getUnderlinedTokenRange)
				.collect(Token::getText)
				.toList()
				.makeString("");
			LOGGER.warn("Not all underlined tokens are in the context: {}", message);
		}

		if (!contextLines.containsAll(underlinedLines)) {
			LOGGER.warn("Not all underlined lines are in the context: {}", underlinedLines);
		}

		ImmutableList<TokenLine> tokenLines = this.getTokenLines(contextTokens);
		MutableList<AbstractContextString> contextStrings = Lists.mutable.empty();

		for (TokenLine tokenLine : tokenLines) {
			Ansi ansi = Ansi.ansi();
			tokenLine.getTokens().forEach((token) -> this.ansiTokenColorizer.colorizeText(ansi, token));

			if (!ansi.toString().endsWith(System.getProperty("line.separator"))) {
				ansi.newline();
			}
			contextStrings.add(new ContextString(tokenLine.getLine(), ansi.toString()));
			if (underlinedLines.contains(tokenLine.getLine())) {
				String underline = this.getUnderline(tokenLine, underlinedTokens);
				contextStrings.add(new UnderlineContextString(tokenLine.getLine(), underline));
			}
		}

		return contextStrings.toImmutable();
	}

	protected Pair<Token, Token> getFirstAndLastToken() {
		MutableSet<Token> contextTokens = Sets.mutable.empty();
		this.offendingContexts.asLazy().collect(ParserRuleContext::getStart).into(contextTokens);
		this.offendingContexts.asLazy().collect(ParserRuleContext::getStop).into(contextTokens);

		return Tuples.pair(contextTokens.min(TOKEN_COMPARATOR), contextTokens.max(TOKEN_COMPARATOR));
	}

	@Nonnull
	private SetIterable<Token> getContextTokens() {
		MutableSet<Token> contextTokens = SetAdapter.adapt(new LinkedHashSet<>());

		this.sourceContexts.asReversed()
			.collect(IAntlrElement::getContextBefore)
			.flatCollect(this::getTokenRange)
			.into(contextTokens);

		this.sourceContexts.asLazy()
			.collect(IAntlrElement::getContextAfter)
			.reject(Objects::isNull)
			.flatCollect(this::getTokenRange)
			.into(contextTokens);
		return contextTokens;
	}

	private ImmutableList<TokenLine> getTokenLines(SetIterable<Token> contextTokens) {
		Iterator<Token> contextTokenIterator = contextTokens.iterator();
		Deque<Token> currentLine = new ArrayDeque<>();
		Token currentToken = null;
		MutableList<TokenLine> tokenLines = Lists.mutable.empty();
		while (contextTokenIterator.hasNext()) {
			Token nextToken = contextTokenIterator.next();
			if (currentLine.isEmpty()) {
				this.startLine(currentLine, nextToken);
				currentToken = nextToken;
			} else if (
				currentToken.getTokenSource() == nextToken.getTokenSource()
				&& currentToken.getLine() == nextToken.getLine()
			) {
				this.endLine(currentLine, nextToken);
				currentToken = nextToken;
			} else {
				tokenLines.add(new TokenLine(currentToken.getLine(), Lists.immutable.withAll(currentLine)));
				currentLine.clear();

				this.startLine(currentLine, nextToken);
				currentToken = nextToken;
			}
		}
		tokenLines.add(new TokenLine(currentToken.getLine(), Lists.immutable.withAll(currentLine)));
		return tokenLines.toImmutable();
	}

	private void startLine(Deque<Token> currentLine, Token nextToken) {
		int beginTokenIndex = this.getBeginTokenIndex(nextToken.getLine(), nextToken.getTokenIndex());
		CommonTokenStream tokenStream = (CommonTokenStream) this.compilationUnit.getTokenStream();
		List<Token> tokenRange = tokenStream.get(beginTokenIndex, nextToken.getTokenIndex() - 1);
		MutableList<Token> tokens = tokenRange == null ? Lists.mutable.empty() : ListAdapter.adapt(tokenRange);
		currentLine.addAll(tokens);
		currentLine.add(nextToken);
	}

	private void endLine(Deque<Token> currentLine, Token nextToken) {
		int endTokenIndex = this.getEndTokenIndex(nextToken.getLine(), nextToken.getTokenIndex());
		CommonTokenStream tokenStream = (CommonTokenStream) this.compilationUnit.getTokenStream();
		List<Token> tokenRange = tokenStream.get(endTokenIndex, nextToken.getTokenIndex() - 1);
		MutableList<Token> tokens = tokenRange == null ? Lists.mutable.empty() : ListAdapter.adapt(tokenRange);
		currentLine.add(nextToken);
		currentLine.addAll(tokens);
	}

	private int getBeginTokenIndex(int startLine, int startTokenIndex) {
		TokenStream tokenStream = this.compilationUnit.getTokenStream();
		int beginTokenIndex = startTokenIndex;
		while (beginTokenIndex > 0 && tokenStream.get(beginTokenIndex - 1).getLine() == startLine) {
			beginTokenIndex -= 1;
		}
		return beginTokenIndex;
	}

	private int getEndTokenIndex(int stopLine, int stopTokenIndex) {
		TokenStream tokenStream = this.compilationUnit.getTokenStream();
		int endTokenIndex = stopTokenIndex;
		while (endTokenIndex + 1 < tokenStream.size() && tokenStream.get(endTokenIndex + 1).getLine() == stopLine) {
			endTokenIndex += 1;
		}
		return endTokenIndex;
	}

	private String getUnderline(TokenLine tokenLine, MutableSet<Token> underlinedTokens) {
		String uncoloredString = tokenLine
			.getTokens()
			.collectWith(this::getSpaceOrUnderline, underlinedTokens)
			.makeString("")
			.stripTrailing();

		Color caretColor = this.getCaretColor();

		return Ansi.ansi().fg(caretColor).a(uncoloredString + "\n").toString();
	}

	@Nonnull
	protected abstract Color getCaretColor();

	private String getSpaceOrUnderline(Token token, MutableSet<Token> underlinedTokens) {
		String character = underlinedTokens.contains(token) ? "^" : " ";
		int length = token.getText().length();
		return character.repeat(length);
	}

	private ImmutableList<Token> getUnderlinedTokenRange(@Nonnull ParserRuleContext ctx) {
		Token startToken = ctx.getStart();
		Token stopToken = ctx.getStop();

		return this.getTokenRange(startToken, stopToken);
	}

	private ImmutableList<Token> getTokenRange(Token startToken, Token stopToken) {
		int startTokenIndex = startToken.getTokenIndex();
		int stopTokenIndex = stopToken.getTokenIndex();

		CommonTokenStream tokenStream = (CommonTokenStream) this.compilationUnit.getTokenStream();

		List<Token> tokens = tokenStream.get(startTokenIndex, stopTokenIndex);
		return Lists.immutable.withAll(tokens);
	}

	private ImmutableList<Token> getTokenRange(Pair<Token, Token> pair) {
		return this.getTokenRange(pair.getOne(), pair.getTwo());
	}

	protected int getLine() {
		return this.getOffendingToken().getLine();
	}

	protected int getCharPositionInLine() {
		return this.getOffendingToken().getCharPositionInLine();
	}

	private Token getOffendingToken() {
		return this.offendingContexts.getFirst().getStart();
	}

	@Nonnull
	protected String getCauseString() {
		return this.macroCause.map(CauseCompilerAnnotation::toString).orElse("");
	}

	@Nonnull
	protected String getOptionalLocationMessage() {
		return this.macroCause.map((ignored) -> "").orElseGet(this::getLocationMessage);
	}

	@Nonnull
	private String getLocationMessage() {
		// @formatter:off
		return Ansi.ansi().a("\n")
				.fg(Color.CYAN).a("Location:  ").reset().a(this.getFilenameWithoutDirectory()).a(":").a(this.getLine()).reset().a("\n")
				.fg(Color.CYAN).a("File:      ").reset().a(this.compilationUnit).reset().a("\n")
				.fg(Color.CYAN).a("Line:      ").reset().a(this.getLine()).reset().a("\n")
				.fg(Color.CYAN).a("Character: ").reset().a(this.getCharPositionInLine() + 1)
				.toString();
		// @formatter:on
	}

	public abstract String toGitHubAnnotation();

	public ImmutableList<Integer> getLines() {
		MutableList<Integer> result = Lists.mutable.empty();
		this.getLines(result);
		return result.toImmutable();
	}

	protected void getLines(MutableList<Integer> lines) {
		lines.add(this.getLine());
		this.macroCause.ifPresent((cause) -> cause.getLines(lines));
	}
}
