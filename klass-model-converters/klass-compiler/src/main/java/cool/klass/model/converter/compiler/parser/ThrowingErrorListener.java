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

import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
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
		String sourceLine = this.getSourceLine(line);
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

	private String getSourceLine(int line) {
		// Stream lazily and stop at the offending line, since most source code parses without errors
		int firstLine = Math.max(1, line - 1);
		return this.sourceCodeText.lines()
			.skip(firstLine - 1)
			.limit(line - firstLine + 1)
			.collect(Collectors.joining("\n"));
	}
}
