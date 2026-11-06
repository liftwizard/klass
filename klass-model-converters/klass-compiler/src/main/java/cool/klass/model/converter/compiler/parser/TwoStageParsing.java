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

import cool.klass.model.meta.grammar.KlassParser;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.eclipse.collections.api.block.function.Function;

/**
 * Parses with fast SLL prediction first and falls back to full LL prediction only when SLL reports an error.
 *
 * <p>The grammar is ambiguous wherever a primitive type keyword is also a valid identifier, so LL prediction
 * runs full-context lookahead and ambiguity detection for every data type property, and ANTLR cannot cache
 * full-context predictions. SLL prediction resolves the same ambiguities the same way, by choosing the lowest
 * alternative, without full-context lookahead. Input with any syntax error, including the errors that grammar
 * actions report, is reparsed with LL prediction so error messages are unchanged.
 */
public final class TwoStageParsing {

	private TwoStageParsing() {
		throw new AssertionError("Suppress default constructor for noninstantiability");
	}

	@Nonnull
	public static ParserRuleContext parse(
		@Nonnull KlassParser parser,
		@Nonnull ANTLRErrorListener errorListener,
		@Nonnull Function<? super KlassParser, ? extends ParserRuleContext> parserRule
	) {
		parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
		parser.removeErrorListeners();
		parser.addErrorListener(SllBailErrorListener.INSTANCE);
		parser.setErrorHandler(new SllBailErrorStrategy());
		try {
			return parserRule.apply(parser);
		} catch (SllParseFailure ignored) {
			parser.reset();
			parser.getInterpreter().setPredictionMode(PredictionMode.LL);
			parser.removeErrorListeners();
			parser.addErrorListener(errorListener);
			parser.setErrorHandler(new DefaultErrorStrategy());
			return parserRule.apply(parser);
		}
	}

	private static final class SllParseFailure extends RuntimeException {

		private static final long serialVersionUID = 1L;

		private SllParseFailure() {
			super(null, null, false, false);
		}
	}

	private static final class SllBailErrorListener extends BaseErrorListener {

		private static final SllBailErrorListener INSTANCE = new SllBailErrorListener();

		@Override
		public void syntaxError(
			Recognizer<?, ?> recognizer,
			Object offendingSymbol,
			int line,
			int charPositionInLine,
			String msg,
			RecognitionException e
		) {
			throw new SllParseFailure();
		}
	}

	private static final class SllBailErrorStrategy extends DefaultErrorStrategy {

		@Override
		public void recover(Parser recognizer, RecognitionException e) {
			throw new SllParseFailure();
		}

		@Override
		public Token recoverInline(Parser recognizer) {
			throw new SllParseFailure();
		}

		@Override
		public void sync(Parser recognizer) {
			// Don't attempt single-token recovery; any error falls back to LL prediction.
		}

		@Override
		public void reportError(Parser recognizer, RecognitionException e) {
			throw new SllParseFailure();
		}
	}
}
