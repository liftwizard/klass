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

package cool.klass.model.meta.domain;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.EnumerationLiteralImpl.EnumerationLiteralBuilder;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.source.EnumerationWithSourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.grammar.KlassParser.EnumerationDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.eclipse.collections.api.list.ImmutableList;

public final class EnumerationImpl extends AbstractPackageableElement implements EnumerationWithSourceCode {

    private ImmutableList<EnumerationLiteral> enumerationLiterals;

    private EnumerationImpl(
        @Nonnull EnumerationDeclarationContext elementContext,
        @Nonnull Optional<Element> macroElement,
        @Nullable SourceCode sourceCode,
        int ordinal,
        @Nonnull IdentifierContext nameContext,
        @Nonnull String packageName
    ) {
        super(elementContext, macroElement, sourceCode, ordinal, nameContext, packageName);
    }

    @Nonnull
    @Override
    public EnumerationDeclarationContext getElementContext() {
        return (EnumerationDeclarationContext) super.getElementContext();
    }

    @Override
    public ImmutableList<EnumerationLiteral> getEnumerationLiterals() {
        return this.enumerationLiterals;
    }

    private void setEnumerationLiterals(@Nonnull ImmutableList<EnumerationLiteral> enumerationLiterals) {
        this.enumerationLiterals = enumerationLiterals;
    }

    public static final class EnumerationBuilder
        extends PackageableElementBuilder<EnumerationImpl>
        implements DataTypeGetter, TopLevelElementBuilderWithSourceCode {

        private ImmutableList<EnumerationLiteralBuilder> enumerationLiteralBuilders;

        public EnumerationBuilder(
            @Nonnull EnumerationDeclarationContext elementContext,
            @Nonnull Optional<ElementBuilder<?>> macroElement,
            @Nullable SourceCodeBuilder sourceCode,
            int ordinal,
            @Nonnull IdentifierContext nameContext,
            @Nonnull String packageName
        ) {
            super(elementContext, macroElement, sourceCode, ordinal, nameContext, packageName);
        }

        public void setEnumerationLiteralBuilders(
            @Nonnull ImmutableList<EnumerationLiteralBuilder> enumerationLiteralBuilders
        ) {
            this.enumerationLiteralBuilders = enumerationLiteralBuilders;
        }

        @Override
        @Nonnull
        protected EnumerationImpl buildUnsafe() {
            return new EnumerationImpl(
                (EnumerationDeclarationContext) this.elementContext,
                this.macroElement.map(ElementBuilder::getElement),
                this.sourceCode.build(),
                this.ordinal,
                this.getNameContext(),
                this.packageName
            );
        }

        @Override
        protected void buildChildren() {
            ImmutableList<EnumerationLiteral> enumerationLiterals =
                this.enumerationLiteralBuilders.collect(EnumerationLiteralBuilder::build);
            this.element.setEnumerationLiterals(enumerationLiterals);
        }

        @Override
        @Nonnull
        public EnumerationImpl getType() {
            return this.getElement();
        }
    }
}
