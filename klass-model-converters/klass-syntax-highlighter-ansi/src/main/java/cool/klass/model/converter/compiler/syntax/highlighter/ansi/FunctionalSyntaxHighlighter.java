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

package cool.klass.model.converter.compiler.syntax.highlighter.ansi;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.style.StyleExtractor;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.style.StyleState;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.style.StyleTransition;
import cool.klass.model.converter.compiler.token.categories.TokenCategory;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.map.MapIterable;
import org.fusesource.jansi.Ansi;

public class FunctionalSyntaxHighlighter {

    @Nonnull
    private final AnsiColorScheme colorScheme;

    @Nonnull
    private final MapIterable<Token, TokenCategory> tokenCategoriesFromLexer;

    @Nonnull
    private final MapIterable<Token, TokenCategory> tokenCategoriesFromParser;

    public FunctionalSyntaxHighlighter(
        @Nonnull AnsiColorScheme colorScheme,
        @Nonnull MapIterable<Token, TokenCategory> tokenCategoriesFromLexer,
        @Nonnull MapIterable<Token, TokenCategory> tokenCategoriesFromParser
    ) {
        this.colorScheme = colorScheme;
        this.tokenCategoriesFromLexer = tokenCategoriesFromLexer;
        this.tokenCategoriesFromParser = tokenCategoriesFromParser;
    }

    @Nonnull
    public Ansi highlightTokens(@Nonnull Iterable<Token> tokens) {
        // Initialize ANSI output with background color
        Ansi ansi = Ansi.ansi();
        this.colorScheme.background(ansi);

        // Set initial style state to empty
        StyleState currentStyle = StyleState.EMPTY;

        // Process tokens with functional style transitions
        for (Token token : tokens) {
            // Get token category (prioritizing parser categories over lexer categories)
            TokenCategory tokenCategory =
                this.tokenCategoriesFromParser.getIfAbsent(token, () -> this.tokenCategoriesFromLexer.get(token));

            if (tokenCategory != null) {
                // Extract the target style based on token category and color scheme
                StyleState targetStyle = StyleExtractor.extractStyleState(tokenCategory, this.colorScheme);

                // Calculate and apply the optimal transition between styles
                currentStyle = StyleTransition.transition(currentStyle, targetStyle, ansi);

                // Write the token text
                ansi.a(token.getText());
            } else {
                // If no category found, just write the text without styling
                ansi.a(token.getText());
            }
        }

        // Reset ANSI styles at the end
        ansi.reset();

        return ansi;
    }
}
