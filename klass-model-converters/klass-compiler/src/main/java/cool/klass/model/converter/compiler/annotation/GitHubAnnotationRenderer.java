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

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.state.TokenRange;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.MutableSet;

/**
 * Renders compiler annotations as GitHub Actions workflow commands, which GitHub shows inline on the pull request diff.
 */
public final class GitHubAnnotationRenderer
{
	private static final Comparator<Token> TOKEN_COMPARATOR = Comparator.comparing(Token::getLine).thenComparing(
		Token::getCharPositionInLine
	);

	private GitHubAnnotationRenderer()
	{
		throw new AssertionError("Suppress default constructor for noninstantiability");
	}

	@Nonnull
	public static String render(@Nonnull RootCompilerAnnotation annotation)
	{
		TokenRange tokenRange = GitHubAnnotationRenderer.getFirstAndLastToken(annotation);
		Token startToken = tokenRange.start();
		Token endToken = tokenRange.stop();

		String sourceName = annotation.getCompilationUnit().getSourceName();
		return "::%s file=%s,line=%d,endLine=%d,col=%d,endColumn=%d,title=%s::%s".formatted(
			GitHubAnnotationRenderer.getSeverityString(annotation.getSeverity()),
			sourceName,
			startToken.getLine(),
			endToken.getLine(),
			startToken.getCharPositionInLine(),
			endToken.getCharPositionInLine(),
			annotation.getAnnotationCode(),
			annotation.getMessage()
		);
	}

	@Nonnull
	private static String getSeverityString(@Nonnull AnnotationSeverity severity)
	{
		return switch (severity)
		{
			case ERROR -> "error";
			case WARNING -> "warning";
		};
	}

	private static TokenRange getFirstAndLastToken(@Nonnull AbstractCompilerAnnotation annotation)
	{
		MutableSet<Token> contextTokens = Sets.mutable.empty();
		annotation.getOffendingContexts().asLazy().collect(ParserRuleContext::getStart).into(contextTokens);
		annotation.getOffendingContexts().asLazy().collect(ParserRuleContext::getStop).into(contextTokens);

		return new TokenRange(contextTokens.min(TOKEN_COMPARATOR), contextTokens.max(TOKEN_COMPARATOR));
	}
}
