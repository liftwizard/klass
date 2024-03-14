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

package cool.klass.model.meta.domain.api.property;

public interface PropertyVisitor
{
    void visitPrimitiveProperty(PrimitiveProperty primitiveProperty);

    void visitEnumerationProperty(EnumerationProperty enumerationProperty);

    void visitAssociationEnd(AssociationEnd associationEnd);

    void visitAssociationEndSignature(AssociationEndSignature associationEndSignature);

    void visitParameterizedProperty(ParameterizedProperty parameterizedProperty);
}
