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

package cool.klass.deserializer.json;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.AssociationEndSignature;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.ParameterizedProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.PropertyVisitor;

public class JsonDataTypeValueVisitor implements PropertyVisitor {

    private final JsonNode jsonDataTypeValue;

    private Object result;

    public JsonDataTypeValueVisitor(JsonNode jsonDataTypeValue) {
        this.jsonDataTypeValue = Objects.requireNonNull(jsonDataTypeValue);
    }

    public static boolean dataTypePropertyIsNullInJson(
        @Nonnull DataTypeProperty dataTypeProperty,
        @Nonnull ObjectNode incomingJson
    ) {
        JsonNode jsonDataTypeValue = incomingJson.path(dataTypeProperty.getName());
        return jsonDataTypeValue.isMissingNode() || jsonDataTypeValue.isNull();
    }

    // TODO: Needs temporal context
    @Nullable
    public static Object extractDataTypePropertyFromJson(
        @Nonnull DataTypeProperty dataTypeProperty,
        @Nonnull ObjectNode incomingJson
    ) {
        JsonNode jsonDataTypeValue = incomingJson.path(dataTypeProperty.getName());
        if (jsonDataTypeValue.isMissingNode() || jsonDataTypeValue.isNull()) {
            return null;
        }

        var visitor = new JsonDataTypeValueVisitor(jsonDataTypeValue);
        dataTypeProperty.visit(visitor);
        return visitor.getResult();
    }

    public Object getResult() {
        return this.result;
    }

    @Override
    public void visitPrimitiveProperty(@Nonnull PrimitiveProperty primitiveProperty) {
        var visitor = new JsonPrimitiveTypeValueVisitor(this.jsonDataTypeValue);
        primitiveProperty.getType().visit(visitor);
        this.result = visitor.getResult();
    }

    @Override
    public void visitEnumerationProperty(@Nonnull EnumerationProperty enumerationProperty) {
        if (!this.jsonDataTypeValue.isTextual()) {
            throw new AssertionError();
        }
        String textValue = this.jsonDataTypeValue.textValue();
        Optional<EnumerationLiteral> enumerationLiteral = enumerationProperty
            .getType()
            .getEnumerationLiterals()
            .detectOptional(each -> each.getPrettyName().equals(textValue));
        this.result = enumerationLiteral.get();
    }

    @Override
    public void visitAssociationEndSignature(AssociationEndSignature associationEndSignature) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitAssociationEndSignature() not implemented yet"
        );
    }

    @Override
    public void visitAssociationEnd(@Nonnull AssociationEnd associationEnd) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitAssociationEnd() not implemented yet"
        );
    }

    @Override
    public void visitParameterizedProperty(@Nonnull ParameterizedProperty parameterizedProperty) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitParameterizedProperty() not implemented yet"
        );
    }
}
