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

package cool.klass.reladomo.tree.serializer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.gs.fw.common.mithra.MithraObject;
import com.gs.fw.common.mithra.finder.AbstractRelatedFinder;
import com.gs.fw.common.mithra.finder.RelatedFinder;
import com.gs.fw.finder.DomainList;
import cool.klass.data.store.reladomo.ReladomoDataStore;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import cool.klass.model.reladomo.tree.DataTypePropertyReladomoTreeNode;
import cool.klass.model.reladomo.tree.ReferencePropertyReladomoTreeNode;
import cool.klass.model.reladomo.tree.ReferenceReladomoTreeNode;
import cool.klass.model.reladomo.tree.ReladomoTreeNodeToManyAwareListener;
import cool.klass.model.reladomo.tree.RootReladomoTreeNode;
import cool.klass.model.reladomo.tree.SubClassReladomoTreeNode;
import cool.klass.model.reladomo.tree.SuperClassReladomoTreeNode;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.ImmutableStack;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.stack.mutable.ArrayStack;

public class ReladomoTreeObjectToMapSerializerListener implements ReladomoTreeNodeToManyAwareListener {

    private static final Converter<String, String> UPPER_TO_LOWER_CAMEL = CaseFormat.UPPER_CAMEL.converterTo(
        CaseFormat.LOWER_CAMEL
    );

    private final MutableStack<Object> contextStack = new ArrayStack<>();
    private final MutableStack<AbstractRelatedFinder> finderStack = new ArrayStack<>();
    private final MutableStack<Object> persistentInstanceStack = new ArrayStack<>();
    private final MutableStack<Object> resultNodeStack = new ArrayStack<>();

    private final ReladomoDataStore dataStore;
    private final DomainList domainList;
    private final Klass klass;

    private final MutableList<Object> result = Lists.mutable.empty();

    public ReladomoTreeObjectToMapSerializerListener(ReladomoDataStore dataStore, DomainList domainList, Klass klass) {
        this.dataStore = Objects.requireNonNull(dataStore);
        this.domainList = Objects.requireNonNull(domainList);
        this.klass = Objects.requireNonNull(klass);
    }

    public MutableList<Object> getResult() {
        return this.result;
    }

    @Override
    public Object getStateToAssertInvariants() {
        return new State(
            this.contextStack.toImmutable(),
            this.finderStack.toImmutable(),
            this.persistentInstanceStack.toImmutable(),
            this.resultNodeStack.toImmutable()
        );
    }

    @Override
    public void enterListIndex(int index) {
        MutableList<Object> resultNode = (MutableList<Object>) this.resultNodeStack.peek();

        Object persistentInstance = this.persistentInstanceStack.peek();
        if (!(persistentInstance instanceof List)) {
            throw new AssertionError("Expected List but found: " + persistentInstance.getClass().getCanonicalName());
        }
        List<Object> persistentList = (List<Object>) persistentInstance;
        Object nextPersistentInstance = persistentList.get(index);

        MutableMap<String, Object> nextResultNode = MapAdapter.adapt(new LinkedHashMap<>());

        Object context = this.contextStack.peek();
        if (context instanceof ReferenceProperty referenceProperty) {
            if (referenceProperty.getType().isAbstract()) {
                nextResultNode.put("__typeName", referenceProperty.getType().getName());
            }
        } else if (context instanceof Classifier classifier) {
            if (classifier.isAbstract()) {
                nextResultNode.put("__typeName", ((Classifier) context).getName());
            }
        } else {
            throw new AssertionError("Unknown context: " + context);
        }
        resultNode.add(nextResultNode);

        this.contextStack.push(index);
        this.persistentInstanceStack.push(nextPersistentInstance);
        this.resultNodeStack.push(nextResultNode);
    }

    @Override
    public void exitListIndex(int index) {
        this.contextStack.pop();
        this.persistentInstanceStack.pop();
        this.resultNodeStack.pop();
    }

    @Override
    public Optional<Integer> enterRoot(RootReladomoTreeNode rootReladomoTreeNode) {
        if (this.klass != rootReladomoTreeNode.getOwningClassifier()) {
            String detailMessage = "Expected " + this.klass + " but got " + rootReladomoTreeNode.getOwningClassifier();
            throw new AssertionError(detailMessage);
        }
        AbstractRelatedFinder relatedFinder = this.dataStore.getRelatedFinder(
            rootReladomoTreeNode.getOwningClassifier()
        );

        this.contextStack.push(rootReladomoTreeNode.getOwningClassifier());
        this.finderStack.push(relatedFinder);
        this.persistentInstanceStack.push(this.domainList);
        this.resultNodeStack.push(this.result);

        return Optional.of(this.domainList.size());
    }

    @Override
    public void exitRoot(RootReladomoTreeNode rootReladomoTreeNode) {
        this.contextStack.pop();
        this.finderStack.pop();
        this.persistentInstanceStack.pop();
        this.resultNodeStack.pop();
    }

    @Override
    public void enterDataTypeProperty(DataTypePropertyReladomoTreeNode dataTypePropertyReladomoTreeNode) {
        DataTypeProperty dataTypeProperty = dataTypePropertyReladomoTreeNode.getDataTypeProperty();
        this.contextStack.push(dataTypeProperty);

        Object persistentInstance = this.persistentInstanceStack.peek();
        if (persistentInstance == null) {
            return;
        }

        MutableMap<String, Object> resultNode = (MutableMap<String, Object>) this.resultNodeStack.peek();

        Object data = this.dataStore.getDataTypeProperty(persistentInstance, dataTypeProperty);
        if (data == null) {
            return;
        }

        Object value = data instanceof EnumerationLiteral enumerationLiteral ? enumerationLiteral.getName() : data;
        resultNode.put(dataTypeProperty.getName(), value);
    }

    @Override
    public void exitDataTypeProperty(DataTypePropertyReladomoTreeNode dataTypePropertyReladomoTreeNode) {
        this.contextStack.pop();
    }

    @Override
    public void enterSuperClass(SuperClassReladomoTreeNode superClassReladomoTreeNode) {
        Klass owningClassifier = superClassReladomoTreeNode.getOwningClassifier();
        Klass superClass = superClassReladomoTreeNode.getType();
        String relationshipName = UPPER_TO_LOWER_CAMEL.convert(superClass.getName()) + "SuperClass";
        RelatedFinder<?> relatedFinder = this.finderStack.peek();
        AbstractRelatedFinder nextFinder = (AbstractRelatedFinder) relatedFinder.getRelationshipFinderByName(
            relationshipName
        );
        Object persistentInstance = this.persistentInstanceStack.peek();

        Object superClassPersistentInstance = this.dataStore.getSuperClass(persistentInstance, owningClassifier);

        this.contextStack.push(superClass);
        this.finderStack.push(nextFinder);
        this.persistentInstanceStack.push(superClassPersistentInstance);
    }

    @Override
    public void exitSuperClass(SuperClassReladomoTreeNode superClassReladomoTreeNode) {
        this.contextStack.pop();
        this.finderStack.pop();
        this.persistentInstanceStack.pop();
    }

    @Override
    public void enterSubClass(SubClassReladomoTreeNode subClassReladomoTreeNode) {
        Klass owningClassifier = subClassReladomoTreeNode.getOwningClassifier();
        Klass subClass = subClassReladomoTreeNode.getType();
        String relationshipName = UPPER_TO_LOWER_CAMEL.convert(subClass.getName()) + "SubClass";
        RelatedFinder<?> relatedFinder = this.finderStack.peek();
        AbstractRelatedFinder nextFinder = (AbstractRelatedFinder) relatedFinder.getRelationshipFinderByName(
            relationshipName
        );
        Object persistentInstance = this.persistentInstanceStack.peek();

        Object subClassPersistentInstance = this.dataStore.getSubClassPersistentInstance(
            owningClassifier,
            subClass,
            (MithraObject) persistentInstance
        );

        if (subClassPersistentInstance != null) {
            MutableMap<String, Object> resultNode = (MutableMap<String, Object>) this.resultNodeStack.peek();
            resultNode.put("__typeName", subClass.getName());
        }

        this.contextStack.push(subClass);
        this.finderStack.push(nextFinder);
        this.persistentInstanceStack.push(subClassPersistentInstance);
    }

    @Override
    public void exitSubClass(SubClassReladomoTreeNode subClassReladomoTreeNode) {
        this.contextStack.pop();
        this.finderStack.pop();
        this.persistentInstanceStack.pop();
    }

    @Override
    public Optional<Integer> enterReferenceProperty(
        ReferencePropertyReladomoTreeNode referencePropertyReladomoTreeNode
    ) {
        ReferenceProperty referenceProperty = referencePropertyReladomoTreeNode.getReferenceProperty();
        this.contextStack.push(referenceProperty);

        String propertyName = referenceProperty.getName();
        RelatedFinder<?> relatedFinder = this.finderStack.peek();
        AbstractRelatedFinder nextFinder = (AbstractRelatedFinder) relatedFinder.getRelationshipFinderByName(
            propertyName
        );
        this.finderStack.push(nextFinder);
        Object persistentInstance = this.persistentInstanceStack.peek();
        MutableMap<String, Object> resultNode = (MutableMap<String, Object>) this.resultNodeStack.peek();
        Object nextPersistentInstance = this.dataStore.get(persistentInstance, referenceProperty);
        this.persistentInstanceStack.push(nextPersistentInstance);

        Multiplicity multiplicity = referenceProperty.getMultiplicity();
        if (multiplicity.isToOne()) {
            if (nextPersistentInstance == null) {
                this.resultNodeStack.push(null);

                // Returning 0 children is a way to say stop recursing through the Projection
                return Optional.of(0);
            }
            MutableMap<String, Object> nextResultNode = MapAdapter.adapt(new LinkedHashMap<>());
            Classifier type = referenceProperty.getType();
            if (type.isAbstract()) {
                nextResultNode.put("__typeName", type.getName());
            }
            resultNode.put(referenceProperty.getName(), nextResultNode);
            this.resultNodeStack.push(nextResultNode);
            return Optional.empty();
        }

        if (multiplicity.isToMany()) {
            MutableList<Object> nextResultNode = Lists.mutable.empty();
            resultNode.put(referenceProperty.getName(), nextResultNode);

            this.resultNodeStack.push(nextResultNode);

            List<Object> toMany = (List<Object>) nextPersistentInstance;

            return Optional.of(toMany.size());
        }

        throw new AssertionError("Unknown multiplicity: " + multiplicity);
    }

    @Override
    public void exitReferenceProperty(ReferencePropertyReladomoTreeNode referencePropertyReladomoTreeNode) {
        this.contextStack.pop();
        this.finderStack.pop();
        this.persistentInstanceStack.pop();
        this.resultNodeStack.pop();
    }

    @Override
    public Optional<Integer> enterReference(ReferenceReladomoTreeNode referenceReladomoTreeNode) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".enterReference() not implemented yet"
        );
    }

    @Override
    public void exitReference(ReferenceReladomoTreeNode referenceReladomoTreeNode) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".exitReference() not implemented yet"
        );
    }

    private record State(
        ImmutableStack<Object> contextStack,
        ImmutableStack<AbstractRelatedFinder> finderStack,
        ImmutableStack<Object> persistentInstanceStack,
        ImmutableStack<Object> resultNodeStack
    ) {}
}
