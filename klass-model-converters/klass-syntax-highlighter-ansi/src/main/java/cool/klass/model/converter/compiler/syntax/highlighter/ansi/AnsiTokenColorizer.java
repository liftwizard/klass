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

package cool.klass.model.converter.compiler.syntax.highlighter.ansi;

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.token.categories.TokenCategories;
import cool.klass.model.converter.compiler.token.categories.TokenCategory;
import org.antlr.v4.runtime.Token;
import org.fusesource.jansi.Ansi;

public final class AnsiTokenColorizer
{
	@Nonnull
	private final AnsiColorScheme colorScheme;

	@Nonnull
	private final TokenCategories tokenCategories;

	public AnsiTokenColorizer(@Nonnull AnsiColorScheme colorScheme, @Nonnull TokenCategories tokenCategories)
	{
		this.colorScheme = Objects.requireNonNull(colorScheme);
		this.tokenCategories = Objects.requireNonNull(tokenCategories);
	}

	public void colorizeText(Ansi ansi, Token token)
	{
		TokenCategory tokenCategory = this.tokenCategories
			.getTokenCategory(token)
			.orElseThrow(() -> new AssertionError("Expected token category for " + token.getText()));
		TokenCategoryToAnsiColor.applyColor(tokenCategory, ansi, this.colorScheme);
		ansi.a(token.getText());
	}
}
