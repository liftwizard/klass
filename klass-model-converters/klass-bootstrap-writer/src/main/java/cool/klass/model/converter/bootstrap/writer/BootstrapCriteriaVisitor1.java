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

package cool.klass.model.converter.bootstrap.writer;

import java.util.LinkedHashMap;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.criteria.AllCriteria;
import cool.klass.model.meta.domain.api.criteria.AndCriteria;
import cool.klass.model.meta.domain.api.criteria.Criteria;
import cool.klass.model.meta.domain.api.criteria.CriteriaVisitor;
import cool.klass.model.meta.domain.api.criteria.EdgePointCriteria;
import cool.klass.model.meta.domain.api.criteria.OperatorCriteria;
import cool.klass.model.meta.domain.api.criteria.OrCriteria;
import klass.model.meta.domain.CriteriaList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.MapAdapter;

public class BootstrapCriteriaVisitor1 implements CriteriaVisitor {

    private final MutableMap<Criteria, klass.model.meta.domain.Criteria> criteriaByCriteria = MapAdapter.adapt(
        new LinkedHashMap<>()
    );

    private final CriteriaList bootstrappedCriteria = new CriteriaList();

    public ImmutableMap<Criteria, klass.model.meta.domain.Criteria> getCriteriaByCriteria() {
        return this.criteriaByCriteria.toImmutable();
    }

    public CriteriaList getBootstrappedCriteria() {
        return this.bootstrappedCriteria;
    }

    @Override
    public void visitAll(@Nonnull AllCriteria allCriteria) {
        var bootstrappedCriteria = new klass.model.meta.domain.Criteria();
        this.criteriaByCriteria.put(allCriteria, bootstrappedCriteria);
        this.bootstrappedCriteria.add(bootstrappedCriteria);
    }

    @Override
    public void visitOperator(@Nonnull OperatorCriteria operatorCriteria) {
        var bootstrappedCriteria = new klass.model.meta.domain.Criteria();
        this.criteriaByCriteria.put(operatorCriteria, bootstrappedCriteria);
        this.bootstrappedCriteria.add(bootstrappedCriteria);
    }

    @Override
    public void visitEdgePoint(@Nonnull EdgePointCriteria edgePointCriteria) {
        var bootstrappedCriteria = new klass.model.meta.domain.Criteria();
        this.criteriaByCriteria.put(edgePointCriteria, bootstrappedCriteria);
        this.bootstrappedCriteria.add(bootstrappedCriteria);
    }

    @Override
    public void visitAnd(@Nonnull AndCriteria andCriteria) {
        var bootstrappedCriteria = new klass.model.meta.domain.Criteria();
        this.criteriaByCriteria.put(andCriteria, bootstrappedCriteria);
        this.bootstrappedCriteria.add(bootstrappedCriteria);

        andCriteria.getLeft().visit(this);
        andCriteria.getRight().visit(this);
    }

    @Override
    public void visitOr(@Nonnull OrCriteria orCriteria) {
        var bootstrappedCriteria = new klass.model.meta.domain.Criteria();
        this.criteriaByCriteria.put(orCriteria, bootstrappedCriteria);
        this.bootstrappedCriteria.add(bootstrappedCriteria);

        orCriteria.getLeft().visit(this);
        orCriteria.getRight().visit(this);
    }
}
