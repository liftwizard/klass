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

package cool.klass.model.reladomo.tree;

import java.util.Objects;

import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Klass;

public class RootReladomoTreeNode extends AbstractReladomoTreeNode {

    private final Klass klass;

    public RootReladomoTreeNode(String name, Klass klass) {
        super(name);
        this.klass = Objects.requireNonNull(klass);
    }

    @Override
    public void visit(ReladomoTreeNodeVisitor visitor) {
        visitor.visitRoot(this);
    }

    @Override
    public Classifier getOwningClassifier() {
        return this.klass;
    }

    @Override
    public Classifier getType() {
        return this.klass;
    }

    @Override
    public String getShortString() {
        return this.getType().getName();
    }

    @Override
    public String getNodeString(String indent) {
        return indent + this.getShortString() + ": " + this.getType().getName() + "\n";
    }
}
