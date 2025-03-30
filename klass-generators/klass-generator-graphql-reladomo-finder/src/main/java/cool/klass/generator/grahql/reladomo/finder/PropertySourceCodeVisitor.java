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

package cool.klass.generator.grahql.reladomo.finder;

import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.AssociationEndSignature;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.ParameterizedProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.PropertyVisitor;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;

public class PropertySourceCodeVisitor implements PropertyVisitor {

    private String sourceCode;

    public String getSourceCode() {
        return this.sourceCode;
    }

    @Override
    public void visitPrimitiveProperty(PrimitiveProperty primitiveProperty) {
        var visitor = new PrimitiveTypeSourceCodeVisitor();
        primitiveProperty.getType().visit(visitor);
        this.sourceCode = String.format("    %s: _%sAttribute\n", primitiveProperty.getName(), visitor.getSourceCode());
    }

    @Override
    public void visitEnumerationProperty(EnumerationProperty enumerationProperty) {
        this.sourceCode = String.format("    %s: _StringAttribute\n", enumerationProperty.getName());
    }

    @Override
    public void visitAssociationEnd(AssociationEnd associationEnd) {
        this.visitReferenceProperty(associationEnd);
    }

    @Override
    public void visitAssociationEndSignature(AssociationEndSignature associationEndSignature) {
        this.visitReferenceProperty(associationEndSignature);
    }

    @Override
    public void visitParameterizedProperty(ParameterizedProperty parameterizedProperty) {
        this.visitReferenceProperty(parameterizedProperty);
    }

    public void visitReferenceProperty(ReferenceProperty referenceProperty) {
        this.sourceCode = String.format(
            "    %s: _%sFinder\n",
            referenceProperty.getName(),
            referenceProperty.getType().getName()
        );
    }
}
