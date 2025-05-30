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

import java.util.Optional;

public interface ReladomoTreeNodeToManyAwareListener {
    Object getStateToAssertInvariants();

    void enterListIndex(int index);

    void exitListIndex(int index);

    Optional<Integer> enterRoot(RootReladomoTreeNode rootReladomoTreeNode);

    void exitRoot(RootReladomoTreeNode rootReladomoTreeNode);

    void enterDataTypeProperty(DataTypePropertyReladomoTreeNode dataTypePropertyReladomoTreeNode);

    void exitDataTypeProperty(DataTypePropertyReladomoTreeNode dataTypePropertyReladomoTreeNode);

    void enterSuperClass(SuperClassReladomoTreeNode superClassReladomoTreeNode);

    void exitSuperClass(SuperClassReladomoTreeNode superClassReladomoTreeNode);

    void enterSubClass(SubClassReladomoTreeNode subClassReladomoTreeNode);

    void exitSubClass(SubClassReladomoTreeNode subClassReladomoTreeNode);

    Optional<Integer> enterReferenceProperty(ReferencePropertyReladomoTreeNode referencePropertyReladomoTreeNode);

    void exitReferenceProperty(ReferencePropertyReladomoTreeNode referencePropertyReladomoTreeNode);

    Optional<Integer> enterReference(ReferenceReladomoTreeNode referenceReladomoTreeNode);

    void exitReference(ReferenceReladomoTreeNode referenceReladomoTreeNode);
}
