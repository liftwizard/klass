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

package cool.klass.model.reladomo.projection;

import java.util.Objects;

import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Klass;

public class SubClassReladomoNode extends AbstractProjectionElementReladomoNode {

    private final Klass klass;
    private final Klass subClass;

    public SubClassReladomoNode(String name, Klass klass, Klass subClass) {
        super(name);
        this.klass = Objects.requireNonNull(klass);
        this.subClass = Objects.requireNonNull(subClass);
    }

    @Override
    public Classifier getOwningClassifier() {
        return this.klass;
    }

    @Override
    public Classifier getType() {
        return this.subClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        SubClassReladomoNode that = (SubClassReladomoNode) o;

        return this.klass.equals(that.klass) && this.subClass.equals(that.subClass);
    }

    @Override
    public int hashCode() {
        int result = this.klass.hashCode();
        result = 31 * result + this.subClass.hashCode();
        return result;
    }
}
