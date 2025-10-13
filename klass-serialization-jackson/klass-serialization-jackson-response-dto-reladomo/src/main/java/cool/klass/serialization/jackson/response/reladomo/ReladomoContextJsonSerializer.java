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

package cool.klass.serialization.jackson.response.reladomo;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.gs.fw.common.mithra.MithraList;
import com.gs.fw.common.mithra.MithraObject;
import cool.klass.data.store.DataStore;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.DataType;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Enumeration;
import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.projection.Projection;
import cool.klass.model.meta.domain.api.projection.ProjectionChild;
import cool.klass.model.meta.domain.api.projection.ProjectionDataTypeProperty;
import cool.klass.model.meta.domain.api.projection.ProjectionElement;
import cool.klass.model.meta.domain.api.projection.ProjectionParent;
import cool.klass.model.meta.domain.api.projection.ProjectionWithReferenceProperty;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import cool.klass.model.meta.domain.api.visitor.PrimitiveTypeVisitor;
import cool.klass.serialization.jackson.model.data.property.SerializeValueToJsonFieldPrimitiveTypeVisitor;
import cool.klass.serialization.jackson.response.KlassResponseMetadata;
import org.eclipse.collections.api.list.ImmutableList;

public class ReladomoContextJsonSerializer extends JsonSerializer<MithraObject> {

    @Nonnull
    private final DomainModel domainModel;

    @Nonnull
    private final DataStore dataStore;

    @Nonnull
    private final KlassResponseMetadata metadata;

    public ReladomoContextJsonSerializer(
        @Nonnull DomainModel domainModel,
        @Nonnull DataStore dataStore,
        @Nonnull KlassResponseMetadata metadata
    ) {
        this.domainModel = Objects.requireNonNull(domainModel);
        this.dataStore = Objects.requireNonNull(dataStore);
        this.metadata = Objects.requireNonNull(metadata);
    }

    @Override
    public void serialize(
        @Nonnull MithraObject mithraObject,
        @Nonnull JsonGenerator jsonGenerator,
        @Nonnull SerializerProvider serializers
    ) throws IOException {
        Class<?> activeViewClass = serializers.getActiveView();
        if (activeViewClass != null) {
            throw new IllegalStateException(activeViewClass.getCanonicalName());
        }

        Projection projection = this.metadata.getProjection();

        // This would work if we consistently used the same DomainModel everywhere (instead of sometimes compiled and sometimes code generated).
        // Projection projection = this.domainModel.getProjections().selectInstancesOf(activeView).getOnly();
        this.serialize(mithraObject, jsonGenerator, projection);
    }

    private void serialize(
        @Nonnull MithraObject mithraObject,
        @Nonnull JsonGenerator jsonGenerator,
        @Nonnull ProjectionParent projectionParent
    ) throws IOException {
        jsonGenerator.writeStartObject();
        try {
            if (projectionParent.hasPolymorphicChildren()) {
                jsonGenerator.writeStringField("__typename", mithraObject.getClass().getCanonicalName());
            }

            this.handleObjectMembers(mithraObject, jsonGenerator, projectionParent);
        } finally {
            jsonGenerator.writeEndObject();
        }
    }

    private void handleObjectMembers(
        @Nonnull MithraObject mithraObject,
        @Nonnull JsonGenerator jsonGenerator,
        @Nonnull ProjectionParent projectionParent
    ) throws IOException {
        Objects.requireNonNull(mithraObject);
        // TODO: Use listener?
        ImmutableList<? extends ProjectionChild> children = projectionParent.getChildren();
        for (ProjectionElement projectionElement : children) {
            if (projectionElement instanceof ProjectionDataTypeProperty dataTypeProperty) {
                this.handleProjectionPrimitiveMember(jsonGenerator, mithraObject, dataTypeProperty);
            } else if (projectionElement instanceof ProjectionWithReferenceProperty referenceProperty) {
                this.handleProjectionWithReferenceProperty(jsonGenerator, mithraObject, referenceProperty);
            } else {
                throw new AssertionError(projectionElement.getClass().getSimpleName());
            }
        }
    }

    private void handleProjectionPrimitiveMember(
        @Nonnull JsonGenerator jsonGenerator,
        MithraObject mithraObject,
        @Nonnull ProjectionDataTypeProperty projectionPrimitiveMember
    ) throws IOException {
        Objects.requireNonNull(mithraObject);

        if (projectionPrimitiveMember.isPolymorphic()) {
            Classifier classifier = projectionPrimitiveMember.getProperty().getOwningClassifier();
            if (!this.dataStore.isInstanceOf(mithraObject, classifier)) {
                return;
            }
        }

        DataTypeProperty property = projectionPrimitiveMember.getProperty();
        String propertyName = property.getName();
        DataType dataType = property.getType();

        Object dataTypeValue = this.dataStore.getDataTypeProperty(mithraObject, property);
        if (dataTypeValue == null) {
            // TODO: Make this configurable
            jsonGenerator.writeNullField(propertyName);
            return;
        }

        if (dataType instanceof Enumeration) {
            EnumerationLiteral enumerationLiteral = (EnumerationLiteral) dataTypeValue;
            jsonGenerator.writeStringField(propertyName, enumerationLiteral.getPrettyName());
            return;
        }

        if (dataType instanceof PrimitiveType primitiveType) {
            PrimitiveTypeVisitor visitor = new SerializeValueToJsonFieldPrimitiveTypeVisitor(
                jsonGenerator,
                propertyName,
                dataTypeValue
            );
            primitiveType.visit(visitor);
            return;
        }

        throw new AssertionError("Unhandled data type: " + dataType.getClass().getCanonicalName());
    }

    public void handleProjectionWithReferenceProperty(
        @Nonnull JsonGenerator jsonGenerator,
        MithraObject mithraObject,
        @Nonnull ProjectionWithReferenceProperty projectionWithAssociationEnd
    ) throws IOException {
        if (projectionWithAssociationEnd.isPolymorphic()) {
            Classifier classifier = projectionWithAssociationEnd.getProperty().getOwningClassifier();
            if (!this.dataStore.isInstanceOf(mithraObject, classifier)) {
                return;
            }
        }

        ReferenceProperty referenceProperty = projectionWithAssociationEnd.getProperty();
        Multiplicity multiplicity = referenceProperty.getMultiplicity();
        String associationEndName = referenceProperty.getName();

        if (multiplicity.isToMany()) {
            Object value = this.dataStore.getToMany(mithraObject, referenceProperty);
            MithraList<MithraObject> mithraList = (MithraList<MithraObject>) Objects.requireNonNull(value);

            // TODO: Add configuration to disable serialization of empty lists
            jsonGenerator.writeArrayFieldStart(associationEndName);
            try {
                mithraList.forEachWithCursor(eachChildValue ->
                    this.recurse((MithraObject) eachChildValue, jsonGenerator, projectionWithAssociationEnd)
                );
            } finally {
                jsonGenerator.writeEndArray();
            }
        } else {
            Object value = this.dataStore.getToOne(mithraObject, referenceProperty);
            // TODO: Add configuration to disable serialization of null values
            if (value == null) {
                // Should only happen for to-one optional relationships
                jsonGenerator.writeNullField(associationEndName);
                return;
            }

            jsonGenerator.writeFieldName(associationEndName);
            this.recurse((MithraObject) value, jsonGenerator, projectionWithAssociationEnd);
        }
    }

    public boolean recurse(
        @Nonnull MithraObject eachChildValue,
        @Nonnull JsonGenerator jsonGenerator,
        @Nonnull ProjectionParent projectionParent
    ) {
        try {
            this.serialize(eachChildValue, jsonGenerator, projectionParent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
