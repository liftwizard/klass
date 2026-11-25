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

package cool.klass.model.converter.compiler.annotation;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.AnsiTokenColorizer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCompilerAnnotation
{
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCompilerAnnotation.class);

	private static final Comparator<Token> TOKEN_COMPARATOR = Comparator.comparing(Token::getLine).thenComparing(
		Token::getCharPositionInLine
	);

	@Nonnull
	protected final SnippetSource snippetSource;

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

	private final boolean enableIdeLinks;

	protected AbstractCompilerAnnotation(
		@Nonnull SnippetSource snippetSource,
		@Nonnull Optional<CauseCompilerAnnotation> macroCause,
		@Nonnull ImmutableList<ParserRuleContext> offendingContexts,
		@Nonnull ImmutableList<IAntlrElement> sourceContexts,
		@Nonnull AnsiTokenColorizer ansiTokenColorizer,
		@Nonnull AnnotationSeverity severity,
		boolean enableIdeLinks
	)
	{
		this.macroCause = Objects.requireNonNull(macroCause);
		this.snippetSource = Objects.requireNonNull(snippetSource);
		this.offendingContexts = Objects.requireNonNull(offendingContexts);
		this.sourceContexts = Objects.requireNonNull(sourceContexts).select(IAntlrElement::isContext);
		this.ansiTokenColorizer = Objects.requireNonNull(ansiTokenColorizer);
		this.severity = Objects.requireNonNull(severity);
		this.enableIdeLinks = enableIdeLinks;

		if (offendingContexts.isEmpty())
		{
			throw new AssertionError();
		}
		if (offendingContexts.anySatisfy((offendingContext) -> offendingContext.getStart() == null))
		{
			throw new AssertionError();
		}

		this.applyListenerToStack();
	}

	@Nonnull
	protected CompilationUnit getCompilationUnit()
	{
		return this.snippetSource.annotatedCompilationUnit();
	}

	public boolean isError()
	{
		return this.severity == AnnotationSeverity.ERROR;
	}

	public boolean isWarning()
	{
		return this.severity == AnnotationSeverity.WARNING;
	}

	@Nonnull
	public AnnotationSeverity getSeverity()
	{
		return this.severity;
	}

	protected String getContextString()
	{
		ImmutableList<AbstractContextString> contextStrings = this.applyListenerToStack();

		int lineNumberWidth = contextStrings.collectInt(AbstractContextString::getLineNumberWidth).max();

		String entireContext = contextStrings
			.collectWith(AbstractContextString::toString, lineNumberWidth)
			.makeString("", "\n", "\n");
		return Ansi.ansi().a(entireContext).reset().toString();
	}

	@Nonnull
	protected String getFilenameWithoutDirectory()
	{
		String sourceName = this.getCompilationUnit().getSourceName();
		return this.macroCause
			.map((ignore) -> sourceName)
			.orElseGet(() -> sourceName.substring(sourceName.lastIndexOf('/') + 1));
	}

	protected String getShortLocationString()
	{
		return this.macroCause.map((ignore) -> this.getLocationWithoutLine()).orElseGet(this::getLocationWithLine);
	}

	private String getLocationWithoutLine()
	{
		return String.format("(%s)", this.getCompilationUnit());
	}

	private String getLocationWithLine()
	{
		return String.format(
			"(%s:%d:%d)",
			// TODO: This should be part of the source information
			this.getFilenameWithoutDirectory(),
			this.getLine(),
			this.getCharPositionInLine() + 1
		);
	}

	protected ImmutableList<AbstractContextString> applyListenerToStack()
	{
		MutableSet<Token> contextTokens = this.getContextTokens();

		MutableSet<Token> underlinedTokens = this.offendingContexts
			.asLazy()
			.flatCollect(this::getUnderlinedTokenRange)
			.into(SetAdapter.adapt(new LinkedHashSet<>()));

		if (!contextTokens.containsAll(underlinedTokens))
		{
			String message = this.offendingContexts
				.asLazy()
				.flatCollect(this::getUnderlinedTokenRange)
				.collect(Token::getText)
				.toList()
				.makeString("");
			LOGGER.warn("Not all underlined tokens are in the context: {}", message);
		}

		MutableSet<Pair<CompilationUnit, Integer>> underlinedLines = underlinedTokens.collect((token) ->
			Tuples.pair(this.snippetSource.getCompilationUnit(token), token.getLine())
		);

		ImmutableList<TokenLine> tokenLines = this.snippetSource.getTokenLines(contextTokens);
		MutableList<AbstractContextString> contextStrings = Lists.mutable.empty();

		for (TokenLine tokenLine : tokenLines)
		{
			Ansi ansi = Ansi.ansi();
			tokenLine.getTokens().forEach((token) -> this.ansiTokenColorizer.colorizeText(ansi, token));

			if (!ansi.toString().endsWith(System.getProperty("line.separator")))
			{
				ansi.newline();
			}
			contextStrings.add(new ContextString(tokenLine.getLine(), ansi.toString(), tokenLine.isGenerated()));
			if (underlinedLines.contains(Tuples.pair(tokenLine.getCompilationUnit(), tokenLine.getLine())))
			{
				String underline = this.getUnderline(tokenLine, underlinedTokens);
				contextStrings.add(new UnderlineContextString(tokenLine.getLine(), underline));
			}
		}

		return contextStrings.toImmutable();
	}

	protected Pair<Token, Token> getFirstAndLastToken()
	{
		MutableSet<Token> contextTokens = Sets.mutable.empty();
		this.offendingContexts.asLazy().collect(ParserRuleContext::getStart).into(contextTokens);
		this.offendingContexts.asLazy().collect(ParserRuleContext::getStop).into(contextTokens);

		return Tuples.pair(contextTokens.min(TOKEN_COMPARATOR), contextTokens.max(TOKEN_COMPARATOR));
	}

	@Nonnull
	private MutableSet<Token> getContextTokens()
	{
		MutableSet<Token> contextTokens = SetAdapter.adapt(new LinkedHashSet<>());

		this.sourceContexts
			.asReversed()
			.collect(IAntlrElement::getContextBefore)
			.flatCollect((pair) -> this.getTokenRange(pair.getOne(), pair.getTwo()))
			.into(contextTokens);

		this.sourceContexts
			.asLazy()
			.collect(IAntlrElement::getContextAfter)
			.reject(Objects::isNull)
			.flatCollect((pair) -> this.getTokenRange(pair.getOne(), pair.getTwo()))
			.into(contextTokens);
		return contextTokens;
	}

	private String getUnderline(TokenLine tokenLine, MutableSet<Token> underlinedTokens)
	{
		String uncoloredString = tokenLine
			.getTokens()
			.collectWith(this::getSpaceOrUnderline, underlinedTokens)
			.makeString("")
			.stripTrailing();

		Color caretColor = this.getCaretColor();

		return Ansi.ansi()
			.fg(caretColor)
			.a(uncoloredString + "\n")
			.toString();
	}

	@Nonnull
	protected abstract Color getCaretColor();

	private String getSpaceOrUnderline(Token token, MutableSet<Token> underlinedTokens)
	{
		String text = token.getText();
		if (underlinedTokens.contains(token))
		{
			return "^".repeat(text.length());
		}
		// Keep tabs so the carets line up under source lines indented with tabs.
		return text.replaceAll("[^\t]", " ");
	}

	private ImmutableList<Token> getUnderlinedTokenRange(@Nonnull ParserRuleContext ctx)
	{
		return this.getTokenRange(ctx.getStart(), ctx.getStop());
	}

	private ImmutableList<Token> getTokenRange(Token startToken, Token stopToken)
	{
		List<Token> tokens = this.snippetSource
			.getTokenStream(startToken)
			.get(startToken.getTokenIndex(), stopToken.getTokenIndex());
		return Lists.immutable.withAll(tokens);
	}

	protected int getLine()
	{
		return this.getOffendingToken().getLine();
	}

	protected int getCharPositionInLine()
	{
		return this.getOffendingToken().getCharPositionInLine();
	}

	private Token getOffendingToken()
	{
		return this.offendingContexts.getFirst().getStart();
	}

	@Nonnull
	protected String getCauseString()
	{
		return this.macroCause.map(CauseCompilerAnnotation::toString).orElse("");
	}

	@Nonnull
	protected String getOptionalLocationMessage()
	{
		return this.macroCause.map((ignored) -> "").orElseGet(this::getLocationMessage);
	}

	@Nonnull
	private String getLocationMessage()
	{
		// @formatter:off
		return Ansi.ansi().a("\n")
				.fg(Color.CYAN).a("Location:  ").reset().a(this.getFilenameWithoutDirectory()).a(":").a(this.getLine()).a(":").a(this.getCharPositionInLine() + 1).reset().a("\n")
				.fg(Color.CYAN).a("File:      ").reset().a(this.getCompilationUnit()).reset().a("\n")
				.fg(Color.CYAN).a("Line:      ").reset().a(this.getLine()).reset().a("\n")
				.fg(Color.CYAN).a("Character: ").reset().a(this.getCharPositionInLine() + 1)
				.toString();
		// @formatter:on
	}

	@Nonnull
	protected String getIdeUrlMessage()
	{
		if (!this.enableIdeLinks)
		{
			return "";
		}

		String absolutePath = this.getCompilationUnit().getSourceName();
		int line = this.getLine();
		int column = this.getCharPositionInLine() + 1;

		String vscodeUrl = String.format("vscode://file/%s:%d:%d", absolutePath, line, column);
		String jetbrainsUrl = String.format(
			"jetbrains://idea/navigate/reference?project=&file=%s&line=%d&column=%d",
			absolutePath,
			line,
			column
		);

		// @formatter:off
		return Ansi.ansi().a("\n")
				.fg(Color.CYAN).a("VS Code:   ").reset().a(vscodeUrl).a("\n")
				.fg(Color.CYAN).a("JetBrains: ").reset().a(jetbrainsUrl)
				.toString();
		// @formatter:on
	}

	public abstract String toGitHubAnnotation();

	public ImmutableList<Integer> getLines()
	{
		MutableList<Integer> result = Lists.mutable.empty();
		this.getLines(result);
		return result.toImmutable();
	}

	protected void getLines(MutableList<Integer> lines)
	{
		lines.add(this.getLine());
		this.macroCause.ifPresent((cause) -> cause.getLines(lines));
	}
}
