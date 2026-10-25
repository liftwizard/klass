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

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThrowingErrorListenerTest {

	private static final String SOURCE_CODE_TEXT = "first\r\nsecond\nthird\n";

	@Test
	void syntaxErrorOnFirstLine() {
		var listener = new ThrowingErrorListener("dir/example.klass", SOURCE_CODE_TEXT);

		assertThatThrownBy(() -> listener.syntaxError(null, null, 1, 2, "bad token", null))
			.isInstanceOf(ParseCancellationException.class)
			.hasMessage("(example.klass:1) bad token dir/example.klass[1:2]" + System.lineSeparator() + "first");
	}

	@Test
	void syntaxErrorOnLaterLineIncludesPreviousLine() {
		var listener = new ThrowingErrorListener("dir/example.klass", SOURCE_CODE_TEXT);

		assertThatThrownBy(() -> listener.syntaxError(null, null, 3, 0, "bad token", null))
			.isInstanceOf(ParseCancellationException.class)
			.hasMessage("(example.klass:3) bad token dir/example.klass[3:0]" + System.lineSeparator() + "second\nthird");
	}

	@Test
	void syntaxErrorAtEndOfFileAfterTrailingNewline() {
		var listener = new ThrowingErrorListener("dir/example.klass", SOURCE_CODE_TEXT);

		assertThatThrownBy(() -> listener.syntaxError(null, null, 4, 0, "bad token", null))
			.isInstanceOf(ParseCancellationException.class)
			.hasMessage("(example.klass:4) bad token dir/example.klass[4:0]" + System.lineSeparator() + "third");
	}

	@Test
	void syntaxErrorTreatsOnlyLineFeedsAsLineBreaksLikeAntlr() {
		var listener = new ThrowingErrorListener("dir/example.klass", "a\rb\nc\n");

		assertThatThrownBy(() -> listener.syntaxError(null, null, 2, 0, "bad token", null))
			.isInstanceOf(ParseCancellationException.class)
			.hasMessage("(example.klass:2) bad token dir/example.klass[2:0]" + System.lineSeparator() + "a\rb\nc");
	}

	@Test
	void syntaxErrorFindsLineFromOffendingTokenCodePointOffset() {
		var sourceCodeText = "😀\nsecond\nthird\n";
		var listener = new ThrowingErrorListener("dir/example.klass", sourceCodeText);
		// The emoji is one code point but two chars, so "third" starts at code point 9 and char 10
		Token offendingToken = new CommonToken(1, "third");
		((CommonToken) offendingToken).setStartIndex(9);

		assertThatThrownBy(() -> listener.syntaxError(null, offendingToken, 3, 0, "bad token", null))
			.isInstanceOf(ParseCancellationException.class)
			.hasMessage("(example.klass:3) bad token dir/example.klass[3:0]" + System.lineSeparator() + "second\nthird");
	}

	@Test
	void syntaxErrorAtEndOfFileAfterBlankLinesShowsLastLineWithCode() {
		var listener = new ThrowingErrorListener("dir/example.klass", "first\nsecond\r\n\n  \r\n\n");

		assertThatThrownBy(() -> listener.syntaxError(null, null, 6, 0, "bad token", null))
			.isInstanceOf(ParseCancellationException.class)
			.hasMessage("(example.klass:6) bad token dir/example.klass[6:0]" + System.lineSeparator() + "second");
	}
}
