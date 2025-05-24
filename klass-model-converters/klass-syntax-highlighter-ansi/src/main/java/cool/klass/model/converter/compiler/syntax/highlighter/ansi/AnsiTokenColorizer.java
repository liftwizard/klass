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

package cool.klass.model.converter.compiler.syntax.highlighter.ansi;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.style.StyleExtractor;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.style.StyleState;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.style.StyleTransition;
import cool.klass.model.converter.compiler.token.categories.TokenCategory;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.map.MapIterable;
import org.fusesource.jansi.Ansi;

public final class AnsiTokenColorizer {

    @Nonnull
    private final AnsiColorScheme colorScheme;

    @Nonnull
    private final MapIterable<Token, TokenCategory> tokenCategoriesFromParser;

    @Nonnull
    private final MapIterable<Token, TokenCategory> tokenCategoriesFromLexer;

    @Nonnull
    private StyleState currentStyleState = StyleState.EMPTY;

    public AnsiTokenColorizer(
        @Nonnull AnsiColorScheme colorScheme,
        @Nonnull MapIterable<Token, TokenCategory> tokenCategoriesFromParser,
        @Nonnull MapIterable<Token, TokenCategory> tokenCategoriesFromLexer
    ) {
        this.colorScheme = Objects.requireNonNull(colorScheme);
        this.tokenCategoriesFromParser = Objects.requireNonNull(tokenCategoriesFromParser);
        this.tokenCategoriesFromLexer = Objects.requireNonNull(tokenCategoriesFromLexer);
    }

    public void resetStyle() {
        this.currentStyleState = StyleState.EMPTY;
    }

    public void applyInitialThemeStyles(Ansi ansi) {
        ansi.a(Ansi.Attribute.RESET);

        // Apply background color from theme
        this.colorScheme.background(ansi);
    }

    public void applyFinalReset(Ansi ansi) {
        ansi.a(Ansi.Attribute.RESET);
        resetStyle();
    }

    @Nonnull
    public void colorizeText(Ansi ansi, Token token) {
        Optional<TokenCategory> tokenCategory = this.getTokenCategory(token);
        String tokenText = token.getText();

        if (tokenCategory.isEmpty()) {
            ansi.a(tokenText);
            return;
        }

        TokenCategory category = tokenCategory.get();

        StyleState targetStyleState = StyleExtractor.extractStyleState(category, this.colorScheme);

        this.currentStyleState = StyleTransition.transition(this.currentStyleState, targetStyleState, ansi);

        if (category == TokenCategory.WHITESPACE) {
            ansi.a(tokenText.replace(' ', '·'));
        } else if (category == TokenCategory.NEWLINE) {
            ansi.a("¶");
            applyFinalReset(ansi);
        } else {
            ansi.a(tokenText);
        }
    }

    private Optional<TokenCategory> getTokenCategory(Token token) {
        TokenCategory lexerCategory = this.tokenCategoriesFromLexer.get(token);
        TokenCategory parserCategory = this.tokenCategoriesFromParser.get(token);
        if (lexerCategory != null && parserCategory != null) {
            throw new AssertionError(token);
        }
        if (lexerCategory != null) {
            return Optional.of(lexerCategory);
        }
        if (parserCategory != null) {
            return Optional.of(parserCategory);
        }
        throw new AssertionError("Expected token category for " + token.getText());
    }
}
