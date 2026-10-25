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

package cool.klass.model.converter.compiler.parser;

import javax.annotation.Nonnull;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;

public class ThrowingErrorListener extends BaseErrorListener {

	private final String sourceName;
	private final String sourceCodeText;

	public ThrowingErrorListener(String sourceName, String sourceCodeText) {
		this.sourceName = sourceName;
		this.sourceCodeText = sourceCodeText;
	}

	@Override
	public void syntaxError(
		Recognizer<?, ?> recognizer,
		Object offendingSymbol,
		int line,
		int charPositionInLine,
		String msg,
		RecognitionException e
	) {
		String sourceLine = this.getSourceLine(offendingSymbol, line);
		String error = String.format(
			"(%s:%d) %s %s[%d:%d]%n%s",
			this.getFilenameWithoutDirectory(),
			line,
			msg,
			this.sourceName,
			line,
			charPositionInLine,
			sourceLine
		);
		throw new ParseCancellationException(error);
	}

	@Nonnull
	private String getFilenameWithoutDirectory() {
		return this.sourceName.substring(this.sourceName.lastIndexOf('/') + 1);
	}

	private String getSourceLine(Object offendingSymbol, int line) {
		// Find line boundaries with indexOf instead of splitting, since this only runs on a syntax error
		int offendingLineStart = this.getOffendingLineStart(offendingSymbol, line);
		int sourceLineStart = offendingLineStart == 0 ? 0 : this.getLineStart(offendingLineStart - 1);
		int lineFeedIndex = this.sourceCodeText.indexOf('\n', offendingLineStart);
		int sourceLineEnd = lineFeedIndex == -1 ? this.sourceCodeText.length() : lineFeedIndex;
		if (sourceLineEnd > offendingLineStart && this.sourceCodeText.charAt(sourceLineEnd - 1) == '\r') {
			sourceLineEnd--;
		}
		return this.sourceCodeText.substring(sourceLineStart, sourceLineEnd).replace("\r\n", "\n");
	}

	private int getOffendingLineStart(Object offendingSymbol, int line) {
		// Tokens conjured during error recovery have no offset
		if (offendingSymbol instanceof Token token && token.getStartIndex() >= 0) {
			return this.getLineStartFromToken(token);
		}
		return this.getLineStartFromLineNumber(line);
	}

	private int getLineStartFromToken(Token token) {
		// Parser errors carry the offending token, whose start index counts code points rather than chars
		int charIndex = this.sourceCodeText.offsetByCodePoints(0, token.getStartIndex());
		return this.getLineStart(charIndex);
	}

	private int getLineStartFromLineNumber(int line) {
		// Lexer errors have no offending token, so count line feeds like ANTLR does
		int lineStart = 0;
		for (int i = 1; i < line; i++) {
			int lineFeedIndex = this.sourceCodeText.indexOf('\n', lineStart);
			if (lineFeedIndex == -1) {
				return this.sourceCodeText.length();
			}
			lineStart = lineFeedIndex + 1;
		}
		return lineStart;
	}

	private int getLineStart(int charIndex) {
		return this.sourceCodeText.lastIndexOf('\n', charIndex - 1) + 1;
	}
}
