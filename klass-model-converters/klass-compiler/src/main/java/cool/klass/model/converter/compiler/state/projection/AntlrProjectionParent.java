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

package cool.klass.model.converter.compiler.state.projection;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.AntlrIdentifierElement;
import cool.klass.model.meta.domain.projection.AbstractProjectionParent;
import cool.klass.model.meta.domain.projection.AbstractProjectionParent.AbstractProjectionParentBuilder;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;

public abstract class AntlrProjectionParent extends AntlrIdentifierElement {

    @Nonnull
    protected final AntlrClassifier classifier;

    protected final MutableList<AntlrProjectionChild> children = Lists.mutable.empty();

    protected final MutableOrderedMap<String, AntlrProjectionChild> childrenByName = OrderedMapAdapter.adapt(
        new LinkedHashMap<>()
    );

    protected AntlrProjectionParent(
        @Nonnull ParserRuleContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        int ordinal,
        @Nonnull IdentifierContext nameContext,
        @Nonnull AntlrClassifier classifier
    ) {
        super(elementContext, compilationUnit, ordinal, nameContext);
        this.classifier = Objects.requireNonNull(classifier);
    }

    @Override
    @Nonnull
    public abstract AbstractProjectionParentBuilder<? extends AbstractProjectionParent> getElementBuilder();

    public MutableList<AntlrProjectionChild> getChildren() {
        return this.children.asUnmodifiable();
    }

    @Nonnull
    public AntlrClassifier getClassifier() {
        return this.classifier;
    }

    public int getNumChildren() {
        return this.children.size();
    }

    public void enterAntlrProjectionMember(@Nonnull AntlrProjectionChild child) {
        this.children.add(child);
        this.childrenByName.compute(child.getName(), (name, builder) ->
            builder == null ? child : AntlrProjectionDataTypeProperty.AMBIGUOUS
        );
    }

    protected ImmutableBag<String> getDuplicateMemberNames() {
        return this.children.collect(AntlrProjectionElement::getName)
            .toBag()
            .selectByOccurrences((occurrences) -> occurrences > 1)
            .toImmutable();
    }

    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        ImmutableBag<String> duplicateMemberNames = this.getDuplicateMemberNames();

        for (AntlrProjectionElement projectionMember : this.children) {
            if (duplicateMemberNames.contains(projectionMember.getName())) {
                projectionMember.reportDuplicateMemberName(compilerAnnotationHolder);
            }
        }
    }
}
