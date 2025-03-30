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

package cool.klass.reladomo.tree.serializer;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.gs.fw.common.mithra.MithraObject;
import com.gs.fw.common.mithra.finder.AbstractRelatedFinder;
import com.gs.fw.common.mithra.finder.RelatedFinder;
import com.gs.fw.finder.DomainList;
import cool.klass.data.store.reladomo.ReladomoDataStore;
import cool.klass.model.meta.domain.api.Classifier;
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
import org.eclipse.collections.api.stack.ImmutableStack;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.stack.mutable.ArrayStack;

public class ReladomoTreeObjectToDTOSerializerListener implements ReladomoTreeNodeToManyAwareListener {

    private static final Converter<String, String> LOWER_TO_UPPER_CAMEL = CaseFormat.LOWER_CAMEL.converterTo(
        CaseFormat.UPPER_CAMEL
    );

    private static final Converter<String, String> UPPER_TO_LOWER_CAMEL = CaseFormat.UPPER_CAMEL.converterTo(
        CaseFormat.LOWER_CAMEL
    );

    private final ReflectionCache reflectionCache = new ReflectionCache();

    private final MutableStack<Object> contextStack = new ArrayStack<>();
    private final MutableStack<AbstractRelatedFinder> finderStack = new ArrayStack<>();
    private final MutableStack<Object> persistentInstanceStack = new ArrayStack<>();
    private final MutableStack<Object> resultNodeStack = new ArrayStack<>();

    private final ReladomoDataStore dataStore;
    private final DomainList domainList;
    private final Klass klass;

    private final MutableList<Object> result = Lists.mutable.empty();

    public ReladomoTreeObjectToDTOSerializerListener(ReladomoDataStore dataStore, DomainList domainList, Klass klass) {
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
        Object persistentInstance = this.persistentInstanceStack.peek();
        if (!(persistentInstance instanceof List)) {
            String detailMessage = "Expected List but found: " + persistentInstance.getClass().getCanonicalName();
            throw new AssertionError(detailMessage);
        }

        List<Object> persistentList = (List<Object>) persistentInstance;
        Object nextPersistentInstance = persistentList.get(index);
        Classifier classifierFromContext = this.getClassifierFromPersistentInstance(nextPersistentInstance);
        Object nextResultNode = this.instantiateDTO(classifierFromContext);

        MutableList<Object> resultNode = (MutableList<Object>) this.resultNodeStack.peek();
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
        AbstractRelatedFinder relatedFinder =
            this.dataStore.getRelatedFinder(rootReladomoTreeNode.getOwningClassifier());

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

        Object resultNode = this.resultNodeStack.peek();

        Object data = this.dataStore.getDataTypeProperty(persistentInstance, dataTypeProperty);
        if (data == null) {
            return;
        }

        var dataTypePropertyVisitor = new ReflectionSetterDataTypePropertyVisitor(
            this.reflectionCache,
            resultNode,
            data
        );
        dataTypeProperty.visit(dataTypePropertyVisitor);
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

        Object subClassPersistentInstance =
            this.dataStore.getSubClassPersistentInstance(owningClassifier, subClass, (MithraObject) persistentInstance);

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
        Object resultNode = this.resultNodeStack.peek();
        Object nextPersistentInstance = this.dataStore.get(persistentInstance, referenceProperty);
        this.persistentInstanceStack.push(nextPersistentInstance);

        if (nextPersistentInstance == null) {
            this.resultNodeStack.push(null);

            // Returning 0 children is a way to say stop recursing through the Projection
            return Optional.of(0);
        }

        Multiplicity multiplicity = referenceProperty.getMultiplicity();
        if (multiplicity.isToOne()) {
            Classifier classifierFromContext = this.getClassifierFromPersistentInstance(nextPersistentInstance);
            Object nextResultNode = this.instantiateDTO(classifierFromContext);
            this.resultNodeStack.push(nextResultNode);
            this.setChildProperty(referenceProperty, resultNode, nextResultNode);
            return Optional.empty();
        }

        if (multiplicity.isToMany()) {
            MutableList<Object> nextResultNode = Lists.mutable.empty();
            this.setChildProperty(referenceProperty, resultNode, nextResultNode);
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

    @Nonnull
    private Classifier getClassifierFromPersistentInstance(Object nextPersistentInstance) {
        Klass klassFromContext = this.getKlassFromContext();
        Klass mostSpecificSubclass = this.dataStore.getMostSpecificSubclass(nextPersistentInstance, klassFromContext);
        if (mostSpecificSubclass.isAbstract()) {
            String detailMessage = "Cannot instantiate abstract class: " + mostSpecificSubclass;
            throw new AssertionError(detailMessage);
        }
        return mostSpecificSubclass;
    }

    @Nonnull
    private Klass getKlassFromContext() {
        Object context = this.contextStack.peek();

        if (context instanceof ReferenceProperty referenceProperty) {
            return (Klass) referenceProperty.getType();
        }

        if (context instanceof Classifier classifier) {
            return (Klass) classifier;
        }

        throw new AssertionError("Unknown context: " + context);
    }

    private void setChildProperty(ReferenceProperty referenceProperty, Object resultNode, Object nextResultNode) {
        String methodName = "set" + LOWER_TO_UPPER_CAMEL.convert(referenceProperty.getName());
        Class<?> resultNodeClass = resultNode.getClass();

        Class<?> nextResultNodeClass = this.getNextResultNodeClass(referenceProperty);
        try {
            Method method = this.reflectionCache.getMethod(resultNodeClass, methodName, nextResultNodeClass);
            method.invoke(resultNode, nextResultNode);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    private Class<?> getNextResultNodeClass(ReferenceProperty referenceProperty) {
        Multiplicity multiplicity = referenceProperty.getMultiplicity();
        if (multiplicity.isToOne()) {
            Classifier classifier = referenceProperty.getType();
            String dtoFQCN = classifier.getPackageName() + ".dto." + classifier.getName() + "DTO";
            return this.reflectionCache.classForName(dtoFQCN);
        }
        if (multiplicity.isToMany()) {
            return List.class;
        }
        throw new AssertionError("Unknown multiplicity: " + multiplicity);
    }

    @Nonnull
    private Object instantiateDTO(Classifier classifier) {
        if (classifier.isAbstract()) {
            String detailMessage = "Cannot instantiate abstract class: " + classifier;
            throw new AssertionError(detailMessage);
        }

        String dtoFQCN = classifier.getPackageName() + ".dto." + classifier.getName() + "DTO";
        try {
            Class<?> aClass = this.reflectionCache.classForName(dtoFQCN);
            return aClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not construct " + dtoFQCN, e);
        }
    }

    private record State(
        ImmutableStack<Object> contextStack,
        ImmutableStack<AbstractRelatedFinder> finderStack,
        ImmutableStack<Object> persistentInstanceStack,
        ImmutableStack<Object> resultNodeStack
    ) {}
}
