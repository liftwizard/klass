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

package cool.klass.model.converter.compiler.parser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.antlr.v4.runtime.ParserRuleContext;

public final class AntlrUtils {

    private AntlrUtils() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    @Nullable
    public static <T> T getParentOfType(@Nonnull ParserRuleContext ctx, @Nonnull Class<T> aClass /* klass? */) {
        if (aClass.isInstance(ctx)) {
            return aClass.cast(ctx);
        }

        ParserRuleContext parent = ctx.getParent();
        if (parent == null) {
            return null;
        }

        return getParentOfType(parent, aClass);
    }
}
