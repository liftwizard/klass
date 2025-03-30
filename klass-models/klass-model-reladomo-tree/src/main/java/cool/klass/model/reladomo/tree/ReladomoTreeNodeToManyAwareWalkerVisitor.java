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

package cool.klass.model.reladomo.tree;

import java.util.Objects;
import java.util.Optional;

public class ReladomoTreeNodeToManyAwareWalkerVisitor implements ReladomoTreeNodeVisitor {

    private final ReladomoTreeNodeToManyAwareListener listener;

    public ReladomoTreeNodeToManyAwareWalkerVisitor(ReladomoTreeNodeToManyAwareListener listener) {
        this.listener = Objects.requireNonNull(listener);
    }

    @Override
    public void visitRoot(RootReladomoTreeNode node) {
        Object before = this.listener.getStateToAssertInvariants();
        Optional<Integer> maybeNumChildren = this.listener.enterRoot(node);
        this.visitChildren(node, maybeNumChildren);
        this.listener.exitRoot(node);
        Object after = this.listener.getStateToAssertInvariants();

        if (!Objects.equals(before, after)) {
            throw new IllegalStateException("State changed during visit");
        }
    }

    @Override
    public void visitDataTypeProperty(DataTypePropertyReladomoTreeNode node) {
        Object before = this.listener.getStateToAssertInvariants();
        this.listener.enterDataTypeProperty(node);
        node.getChildren().forEachKeyValue((name, child) -> child.visit(this));
        this.listener.exitDataTypeProperty(node);
        Object after = this.listener.getStateToAssertInvariants();

        if (!Objects.equals(before, after)) {
            throw new IllegalStateException("State changed during visit");
        }
    }

    @Override
    public void visitSuperClass(SuperClassReladomoTreeNode node) {
        Object before = this.listener.getStateToAssertInvariants();
        this.listener.enterSuperClass(node);
        node.getChildren().forEachKeyValue((name, child) -> child.visit(this));
        this.listener.exitSuperClass(node);
        Object after = this.listener.getStateToAssertInvariants();

        if (!Objects.equals(before, after)) {
            throw new IllegalStateException("State changed during visit");
        }
    }

    @Override
    public void visitSubClass(SubClassReladomoTreeNode node) {
        Object before = this.listener.getStateToAssertInvariants();
        this.listener.enterSubClass(node);
        node.getChildren().forEachKeyValue((name, child) -> child.visit(this));
        this.listener.exitSubClass(node);
        Object after = this.listener.getStateToAssertInvariants();

        if (!Objects.equals(before, after)) {
            throw new IllegalStateException("State changed during visit");
        }
    }

    @Override
    public void visitReferenceProperty(ReferencePropertyReladomoTreeNode node) {
        Object before = this.listener.getStateToAssertInvariants();
        Optional<Integer> maybeNumChildren = this.listener.enterReferenceProperty(node);
        this.visitChildren(node, maybeNumChildren);
        this.listener.exitReferenceProperty(node);
        Object after = this.listener.getStateToAssertInvariants();

        if (!Objects.equals(before, after)) {
            throw new IllegalStateException("State changed during visit");
        }
    }

    @Override
    public void visit(ReferenceReladomoTreeNode node) {
        Object before = this.listener.getStateToAssertInvariants();
        Optional<Integer> maybeNumChildren = this.listener.enterReference(node);
        this.visitChildren(node, maybeNumChildren);
        this.listener.exitReference(node);
        Object after = this.listener.getStateToAssertInvariants();
        if (!Objects.equals(before, after)) {
            throw new IllegalStateException("State changed during visit");
        }
    }

    private void visitChildren(ReladomoTreeNode node, Optional<Integer> maybeNumChildren) {
        if (maybeNumChildren.isEmpty()) {
            node.getChildren().forEachKeyValue((name, child) -> child.visit(this));
            return;
        }

        Integer numChildren = maybeNumChildren.get();
        for (int i = 0; i < numChildren; i++) {
            this.listener.enterListIndex(i);
            node.getChildren().forEachKeyValue((name, child) -> child.visit(this));
            this.listener.exitListIndex(i);
        }
    }
}
