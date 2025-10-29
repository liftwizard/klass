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

package cool.klass.model.reladomo.projection;

import java.util.LinkedHashMap;

import javax.annotation.Nonnull;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.projection.Projection;
import cool.klass.model.meta.domain.api.projection.ProjectionChild;
import cool.klass.model.meta.domain.api.projection.ProjectionDataTypeProperty;
import cool.klass.model.meta.domain.api.projection.ProjectionParent;
import cool.klass.model.meta.domain.api.projection.ProjectionProjectionReference;
import cool.klass.model.meta.domain.api.projection.ProjectionReferenceProperty;
import cool.klass.model.meta.domain.api.property.Property;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;

public final class ReladomoProjectionConverter {

    public static final Converter<String, String> UPPER_TO_LOWER_CAMEL = CaseFormat.UPPER_CAMEL.converterTo(
        CaseFormat.LOWER_CAMEL
    );

    private final MutableOrderedMap<Projection, RootReladomoNode> rootNodesByProjection = OrderedMapAdapter.adapt(
        new LinkedHashMap<>()
    );

    // TODO: MutableOrderedMap once it implements toImmutable()
    private final MutableMap<
        ProjectionProjectionReference,
        ProjectionElementReladomoNode
    > projectionHoldersByProjectionReference = Maps.mutable.empty();

    @Nonnull
    public RootReladomoNode getRootReladomoNode(Classifier classifier, Projection projection) {
        var projectionReladomoNode = new RootReladomoNode("root", classifier, projection);
        this.projectionChildrenToReladomoTree(projectionReladomoNode, projection);
        this.rootNodesByProjection.put(projection, projectionReladomoNode);

        this.projectionHoldersByProjectionReference.toImmutable().forEachKeyValue(
                (eachProjectionReference, eachProjectionReferenceNode) -> {
                    RootReladomoNode rootReladomoNode = this.rootNodesByProjection.getIfAbsent(
                        eachProjectionReference.getProjection(),
                        () ->
                            this.getRootReladomoNode(
                                eachProjectionReference.getClassifier(),
                                eachProjectionReference.getProjection()
                            )
                    );

                    eachProjectionReferenceNode.setProjection(rootReladomoNode);
                    this.projectionHoldersByProjectionReference.remove(eachProjectionReference);
                }
            );

        return projectionReladomoNode;
    }

    public void projectionChildrenToReladomoTree(
        ProjectionElementReladomoNode reladomoNode,
        ProjectionParent projectionParent
    ) {
        for (ProjectionChild projectionChild : projectionParent.getChildren()) {
            this.projectionElementToReladomoTree(reladomoNode, projectionChild);
        }
    }

    private void projectionElementToReladomoTree(
        ProjectionElementReladomoNode projectionReladomoNode,
        ProjectionChild projectionChild
    ) {
        Property property = projectionChild.getProperty();
        String name = property.getName();

        ProjectionElementReladomoNode reladomoNode = projectionReladomoNode;
        reladomoNode = getInheritancePath(reladomoNode, property.getOwningClassifier());
        if (reladomoNode == null) {
            return;
        }

        if (projectionChild instanceof ProjectionProjectionReference projectionProjectionReference) {
            var childNode = new ProjectionProjectionReferenceReladomoNode(name, projectionProjectionReference);
            reladomoNode = reladomoNode.computeChild(name, childNode);
            reladomoNode = getInheritancePath(
                reladomoNode,
                projectionProjectionReference.getProjection().getClassifier()
            );
            this.projectionHoldersByProjectionReference.put(projectionProjectionReference, reladomoNode);
        } else if (projectionChild instanceof ProjectionReferenceProperty projectionReferenceProperty) {
            var childNode = new ProjectionReferencePropertyReladomoNode(name, projectionReferenceProperty);
            reladomoNode = reladomoNode.computeChild(name, childNode);
            this.projectionChildrenToReladomoTree(reladomoNode, projectionReferenceProperty);
        } else if (projectionChild instanceof ProjectionDataTypeProperty projectionDataTypeProperty) {
            var childNode = new ProjectionDataTypePropertyReladomoNode(name, projectionDataTypeProperty);
            reladomoNode = reladomoNode.computeChild(name, childNode);
        } else {
            throw new AssertionError(
                "Expected ProjectionProjectionReference or ProjectionReferenceProperty but got " +
                projectionChild.getClass().getCanonicalName()
            );
        }
    }

    public static ProjectionElementReladomoNode getInheritancePath(
        ProjectionElementReladomoNode reladomoNode,
        Classifier end
    ) {
        ProjectionElementReladomoNode eachReladomoNode = reladomoNode;
        Classifier start = (Classifier) reladomoNode.getType();

        if (start.isStrictSubTypeOf(end)) {
            Klass eachKlass = (Klass) start;
            while (eachKlass != end) {
                Klass superClass = eachKlass.getSuperClass().orElse(null);
                if (superClass == null) {
                    return eachReladomoNode;
                }
                String name = UPPER_TO_LOWER_CAMEL.convert(superClass.getName()) + "SuperClass";
                var nextReladomoNode = new SuperClassReladomoNode(name, eachKlass, superClass);
                eachReladomoNode = eachReladomoNode.computeChild(name, nextReladomoNode);
                eachKlass = superClass;
            }
            return eachReladomoNode;
        }

        if (start.isStrictSuperTypeOf(end)) {
            MutableStack<Klass> stack = Stacks.mutable.empty();
            Klass eachClassifier = (Klass) end;
            while (eachClassifier != start) {
                Klass superClass = eachClassifier.getSuperClass().get();
                stack.push(eachClassifier);
                eachClassifier = superClass;
            }

            Klass eachClassifier2 = (Klass) start;
            while (stack.notEmpty()) {
                Klass eachSubClass = stack.pop();
                String name = UPPER_TO_LOWER_CAMEL.convert(eachSubClass.getName()) + "SubClass";
                var nextReladomoNode = new SubClassReladomoNode(name, eachClassifier2, eachSubClass);
                eachReladomoNode = eachReladomoNode.computeChild(name, nextReladomoNode);
                eachClassifier2 = eachSubClass;
            }

            return eachReladomoNode;
        }

        if (start == end) {
            return eachReladomoNode;
        }

        return null;
    }
}
