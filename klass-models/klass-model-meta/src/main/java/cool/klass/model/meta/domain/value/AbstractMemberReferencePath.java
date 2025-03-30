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

package cool.klass.model.meta.domain.value;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.KlassImpl;
import cool.klass.model.meta.domain.KlassImpl.KlassBuilder;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.api.value.MemberReferencePath;
import cool.klass.model.meta.domain.property.AbstractDataTypeProperty;
import cool.klass.model.meta.domain.property.AbstractDataTypeProperty.DataTypePropertyBuilder;
import cool.klass.model.meta.domain.property.AssociationEndImpl.AssociationEndBuilder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;

public abstract class AbstractMemberReferencePath extends AbstractExpressionValue implements MemberReferencePath {

    @Nonnull
    private final KlassImpl klass;

    @Nonnull
    private final ImmutableList<AssociationEnd> associationEnds;

    @Nonnull
    private final AbstractDataTypeProperty<?> property;

    protected AbstractMemberReferencePath(
        @Nonnull ParserRuleContext elementContext,
        @Nonnull Optional<Element> macroElement,
        @Nullable SourceCode sourceCode,
        @Nonnull KlassImpl klass,
        @Nonnull ImmutableList<AssociationEnd> associationEnds,
        @Nonnull AbstractDataTypeProperty<?> property
    ) {
        super(elementContext, macroElement, sourceCode);
        this.klass = Objects.requireNonNull(klass);
        this.associationEnds = Objects.requireNonNull(associationEnds);
        this.property = Objects.requireNonNull(property);
    }

    @Override
    @Nonnull
    public KlassImpl getKlass() {
        return this.klass;
    }

    @Override
    @Nonnull
    public ImmutableList<AssociationEnd> getAssociationEnds() {
        return this.associationEnds;
    }

    @Override
    @Nonnull
    public AbstractDataTypeProperty<?> getProperty() {
        return this.property;
    }

    public abstract static class AbstractMemberReferencePathBuilder<BuiltElement extends AbstractMemberReferencePath>
        extends AbstractExpressionValueBuilder<BuiltElement> {

        @Nonnull
        protected final KlassBuilder klassBuilder;

        @Nonnull
        protected final ImmutableList<AssociationEndBuilder> associationEndBuilders;

        @Nonnull
        protected final DataTypePropertyBuilder<?, ?, ?> propertyBuilder;

        protected AbstractMemberReferencePathBuilder(
            @Nonnull ParserRuleContext elementContext,
            @Nonnull Optional<ElementBuilder<?>> macroElement,
            @Nullable SourceCodeBuilder sourceCode,
            @Nonnull KlassBuilder klassBuilder,
            @Nonnull ImmutableList<AssociationEndBuilder> associationEndBuilders,
            @Nonnull DataTypePropertyBuilder<?, ?, ?> propertyBuilder
        ) {
            super(elementContext, macroElement, sourceCode);
            this.klassBuilder = Objects.requireNonNull(klassBuilder);
            this.associationEndBuilders = Objects.requireNonNull(associationEndBuilders);
            this.propertyBuilder = Objects.requireNonNull(propertyBuilder);
        }
    }
}
