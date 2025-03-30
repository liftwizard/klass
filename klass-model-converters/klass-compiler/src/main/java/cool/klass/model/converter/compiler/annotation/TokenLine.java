/*
 * Copyright 2024 Craig Motlin
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

import java.util.Objects;

import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.list.ImmutableList;

public class TokenLine {

    private final int line;
    private final ImmutableList<Token> tokens;

    public TokenLine(int line, ImmutableList<Token> tokens) {
        this.line = line;
        this.tokens = Objects.requireNonNull(tokens);
    }

    public int getLine() {
        return this.line;
    }

    public ImmutableList<Token> getTokens() {
        return this.tokens;
    }

    @Override
    public String toString() {
        return String.format("%2d: %s", this.line, this.tokens.collect(Token::getText).makeString(""));
    }
}
