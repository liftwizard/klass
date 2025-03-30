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

package cool.klass.model.converter.compiler.state.property;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrClassReference;
import cool.klass.model.converter.compiler.state.AntlrClassReferenceOwner;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.antlr.v4.runtime.ParserRuleContext;

public abstract class AntlrClassReferenceProperty
    extends AntlrReferenceProperty<AntlrClass>
    implements AntlrClassReferenceOwner {

    protected AntlrClassReference classReference;

    protected AntlrClassReferenceProperty(
        @Nonnull ParserRuleContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        int ordinal,
        @Nonnull IdentifierContext nameContext
    ) {
        super(elementContext, compilationUnit, ordinal, nameContext);
    }

    @Override
    public void enterClassReference(@Nonnull AntlrClassReference classReference) {
        if (this.classReference != null) {
            throw new AssertionError();
        }

        this.classReference = Objects.requireNonNull(classReference);
    }

    @Nonnull
    @Override
    public AntlrClass getType() {
        return this.classReference.getKlass();
    }
}
