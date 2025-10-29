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

package cool.klass.model.converter.compiler.state;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.state.order.AntlrOrderBy;
import cool.klass.model.converter.compiler.state.order.AntlrOrderByMemberReferencePath;
import cool.klass.model.converter.compiler.state.order.AntlrOrderByVisitor;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEnd;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.value.AntlrThisMemberReferencePath;

public class ReferencedPropertiesOrderByVisitor implements AntlrOrderByVisitor {

    private final Set<AntlrAssociationEnd> associationEndsReferencedByOrderBy = new LinkedHashSet<>();
    private final Set<AntlrDataTypeProperty<?>> dataTypePropertiesReferencedByOrderBy = new LinkedHashSet<>();
    private final ReferencedPropertiesExpressionValueVisitor expressionValueVisitor =
        new ReferencedPropertiesExpressionValueVisitor();

    public Set<AntlrAssociationEnd> getAssociationEndsReferencedByOrderBy() {
        return this.associationEndsReferencedByOrderBy;
    }

    public Set<AntlrDataTypeProperty<?>> getDataTypePropertiesReferencedByOrderBy() {
        return this.dataTypePropertiesReferencedByOrderBy;
    }

    @Override
    public void visit(@Nonnull AntlrOrderBy orderBy) {
        for (AntlrOrderByMemberReferencePath memberReferencePath : orderBy.getOrderByMemberReferencePaths()) {
            this.visit(memberReferencePath);
        }
    }

    private void visit(@Nonnull AntlrOrderByMemberReferencePath memberReferencePath) {
        AntlrThisMemberReferencePath thisMemberReferencePath = memberReferencePath.getThisMemberReferencePath();
        thisMemberReferencePath.visit(this.expressionValueVisitor);

        this.associationEndsReferencedByOrderBy.addAll(
            this.expressionValueVisitor.getAssociationEndsReferencedByCriteria()
        );
        this.dataTypePropertiesReferencedByOrderBy.addAll(
            this.expressionValueVisitor.getDataTypePropertiesReferencedByCriteria()
        );
    }
}
