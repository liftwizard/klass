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

package cool.klass.reladomo.tree.deep.fetcher;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.gs.fw.common.mithra.finder.RelatedFinder;
import com.gs.fw.finder.DomainList;
import com.gs.fw.finder.Navigation;
import cool.klass.data.store.reladomo.ReladomoDataStore;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.NamedElement;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import cool.klass.model.reladomo.tree.DataTypePropertyReladomoTreeNode;
import cool.klass.model.reladomo.tree.ReferencePropertyReladomoTreeNode;
import cool.klass.model.reladomo.tree.ReferenceReladomoTreeNode;
import cool.klass.model.reladomo.tree.ReladomoTreeNodeListener;
import cool.klass.model.reladomo.tree.RootReladomoTreeNode;
import cool.klass.model.reladomo.tree.SubClassReladomoTreeNode;
import cool.klass.model.reladomo.tree.SuperClassReladomoTreeNode;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.stack.mutable.ArrayStack;

public class ReladomoTreeNodeDeepFetcherListener implements ReladomoTreeNodeListener {

    private static final Converter<String, String> UPPER_TO_LOWER_CAMEL = CaseFormat.UPPER_CAMEL.converterTo(
        CaseFormat.LOWER_CAMEL
    );

    private final MutableStack<RelatedFinder<?>> stack = new ArrayStack<>();
    private final MutableStack<NamedElement> contextStack = new ArrayStack<>();

    private final ReladomoDataStore dataStore;
    private final DomainList domainList;
    private final Klass klass;

    public ReladomoTreeNodeDeepFetcherListener(ReladomoDataStore dataStore, DomainList domainList, Klass klass) {
        this.dataStore = dataStore;
        this.domainList = domainList;
        this.klass = klass;
    }

    @Override
    public void assertInvariants() {
        if (this.stack.size() == this.contextStack.size()) {
            return;
        }

        String detailMessage = "Expected " + this.stack.size() + " but got " + this.contextStack.size();
        throw new AssertionError(detailMessage);
    }

    @Override
    public void enterRoot(RootReladomoTreeNode rootReladomoTreeNode) {
        if (this.klass != rootReladomoTreeNode.getOwningClassifier()) {
            String detailMessage = "Expected " + this.klass + " but got " + rootReladomoTreeNode.getOwningClassifier();
            throw new AssertionError(detailMessage);
        }
        RelatedFinder<?> relatedFinder = this.dataStore.getRelatedFinder(rootReladomoTreeNode.getOwningClassifier());
        this.stack.push(relatedFinder);
        this.contextStack.push(rootReladomoTreeNode.getOwningClassifier());
    }

    @Override
    public void exitRoot(RootReladomoTreeNode rootReladomoTreeNode) {
        this.stack.pop();
        this.contextStack.pop();
    }

    @Override
    public void enterDataTypeProperty(DataTypePropertyReladomoTreeNode dataTypePropertyReladomoTreeNode) {}

    @Override
    public void exitDataTypeProperty(DataTypePropertyReladomoTreeNode dataTypePropertyReladomoTreeNode) {}

    @Override
    public void enterSuperClass(SuperClassReladomoTreeNode superClassReladomoTreeNode) {
        Klass superClass = superClassReladomoTreeNode.getType();
        String relationshipName = UPPER_TO_LOWER_CAMEL.convert(superClass.getName()) + "SuperClass";
        RelatedFinder<?> relatedFinder = this.stack.peek();
        RelatedFinder<?> nextFinder = relatedFinder.getRelationshipFinderByName(relationshipName);
        this.stack.push(nextFinder);
        this.contextStack.push(superClass);
    }

    @Override
    public void exitSuperClass(SuperClassReladomoTreeNode superClassReladomoTreeNode) {
        RelatedFinder<?> relatedFinder = this.stack.peek();
        Navigation<?> navigation = (Navigation<?>) relatedFinder;
        this.domainList.deepFetch(navigation);
        this.stack.pop();
        this.contextStack.pop();
    }

    @Override
    public void enterSubClass(SubClassReladomoTreeNode subClassReladomoTreeNode) {
        Klass subClass = subClassReladomoTreeNode.getType();
        String relationshipName = UPPER_TO_LOWER_CAMEL.convert(subClass.getName()) + "SubClass";
        RelatedFinder<?> relatedFinder = this.stack.peek();
        RelatedFinder<?> nextFinder = relatedFinder.getRelationshipFinderByName(relationshipName);
        this.stack.push(nextFinder);
        this.contextStack.push(subClass);
    }

    @Override
    public void exitSubClass(SubClassReladomoTreeNode subClassReladomoTreeNode) {
        RelatedFinder<?> relatedFinder = this.stack.peek();
        Navigation<?> navigation = (Navigation<?>) relatedFinder;
        this.domainList.deepFetch(navigation);
        this.stack.pop();
        this.contextStack.pop();
    }

    @Override
    public void enterReferenceProperty(ReferencePropertyReladomoTreeNode referencePropertyReladomoTreeNode) {
        ReferenceProperty referenceProperty = referencePropertyReladomoTreeNode.getReferenceProperty();
        String propertyName = referenceProperty.getName();
        RelatedFinder<?> relatedFinder = this.stack.peek();
        RelatedFinder<?> nextFinder = relatedFinder.getRelationshipFinderByName(propertyName);
        this.stack.push(nextFinder);
        this.contextStack.push(referenceProperty);
    }

    @Override
    public void exitReferenceProperty(ReferencePropertyReladomoTreeNode referencePropertyReladomoTreeNode) {
        RelatedFinder<?> relatedFinder = this.stack.peek();
        Navigation<?> navigation = (Navigation<?>) relatedFinder;
        this.domainList.deepFetch(navigation);
        this.stack.pop();
        this.contextStack.pop();
    }

    @Override
    public void enterReference(ReferenceReladomoTreeNode referenceReladomoTreeNode) {
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
}
