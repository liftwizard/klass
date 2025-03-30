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

package cool.klass.syntax.generator.css.token;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import cool.klass.model.converter.compiler.token.categories.TokenCategory;

public final class TokenCategoryToCss {

    public static final Converter<String, String> CONVERTER = CaseFormat.UPPER_UNDERSCORE.converterTo(
        CaseFormat.LOWER_HYPHEN
    );

    private TokenCategoryToCss() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * The console output gets synced to the file klass-syntax/klass-syntax-css-class/src/main/resources/ui/static/css/klass-syntax.css
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        for (TokenCategory tokenCategory : TokenCategory.values()) {
            // language=CSS
            String result =
                "" +
                ".klass-" +
                TokenCategoryToCss.getTokenCategoryName(tokenCategory) +
                " {\n" +
                "    color: " +
                TokenCategoryToCss.getCssVar(tokenCategory) +
                ";\n" +
                "}\n";
            System.out.print(result);
        }
    }

    private static String getCssVar(TokenCategory tokenCategory) {
        TokenCategory parentCategory = tokenCategory.getParentCategory();
        String fallbackCssVar = parentCategory == null
            ? "--color-foreground"
            : TokenCategoryToCss.getCssVar(parentCategory);

        return String.format(
            "var(--klass-color-%s, %s)",
            TokenCategoryToCss.getTokenCategoryName(tokenCategory),
            fallbackCssVar
        );
    }

    private static String getTokenCategoryName(TokenCategory tokenCategory) {
        return CONVERTER.convert(tokenCategory.name());
    }
}
