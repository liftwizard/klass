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

import cool.klass.model.converter.compiler.syntax.highlighter.ansi.functional.AnsiColorStyle;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.functional.AnsiStreamWriter;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.functional.AnsiStreamWriter.StyledToken;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.dto.StyleSettings;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.util.AnsiColorUtils;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.util.StyleConversionUtils;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.util.TokenFormatUtils;
import cool.klass.model.converter.compiler.token.categories.TokenCategory;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.fusesource.jansi.Ansi;

/**
 * Compatibility wrapper for the legacy FunctionalSyntaxHighlighter API.
 * Delegates to the modern functional implementation while maintaining backward compatibility.
 */
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
    public AnsiColorScheme getColorScheme() {
        return this.colorScheme;
    }

    public void applyInitialThemeStyles(Ansi ansi) {
        // Apply both foreground and background for theme consistency
        this.colorScheme.foreground(ansi);
        this.colorScheme.background(ansi);
    }

    public void applyFinalReset(Ansi ansi) {
        ansi.reset();
    }

    public void applyLineNumberStyle(Ansi ansi) {
        // Use the block comment style for line numbers (typically muted/gray)
        TokenCategory lineNumberCategory = TokenCategory.BLOCK_COMMENT;
        AnsiColorStyle lineNumberStyle = convertCategoryToStyle(lineNumberCategory);

        // Apply the line number style by converting to Ansi instructions
        // This is a simplified approach compared to the old StyleTransition logic
        if (lineNumberStyle.foreground() != null) {
            AnsiColorUtils.applyColor(ansi, lineNumberStyle.foreground(), true);
        }
        if (lineNumberStyle.background() != null) {
            AnsiColorUtils.applyColor(ansi, lineNumberStyle.background(), false);
        }
        // Note: Other style attributes like bold, italic, etc. would need similar handling
    }

    @Nonnull
    public Ansi highlightTokens(@Nonnull Iterable<Token> tokens) {
        // Get theme defaults for foreground and background
        StyleSettings foregroundSettings = this.colorScheme.getStyleSettings(TokenCategory.IDENTIFIER);
        StyleSettings backgroundSettings = this.colorScheme.getStyleSettings(null);

        Object defaultForeground = foregroundSettings.foreground();
        Object defaultBackground = backgroundSettings.background();

        MutableList<StyledToken> styledTokens = Lists.mutable.empty();

        for (Token token : tokens) {
            TokenCategory category = TokenFormatUtils.getTokenCategory(
                token,
                this.tokenCategoriesFromLexer,
                this.tokenCategoriesFromParser
            ).orElse(null);

            AnsiColorStyle style = category != null ? convertCategoryToStyle(category) : AnsiColorStyle.EMPTY;

            String formattedText = TokenFormatUtils.formatTokenText(token, category);

            styledTokens.add(new StyledToken(style, formattedText));
        }

        return AnsiStreamWriter.write(styledTokens.toImmutable(), defaultForeground, defaultBackground);
    }

    private AnsiColorStyle convertCategoryToStyle(TokenCategory category) {
        StyleSettings styleSettings = this.colorScheme.getStyleSettings(category);
        return StyleConversionUtils.convertToAnsiColorStyle(styleSettings);
    }
}
