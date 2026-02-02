/*
 * Copyright 2026 Craig Motlin
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

package cool.klass.serialization.jackson.jsonview.reladomo;

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
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.projection.Projection;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import cool.klass.model.meta.domain.api.visitor.PrimitiveTypeVisitor;
import cool.klass.model.reladomo.projection.ProjectionDataTypePropertyReladomoNode;
import cool.klass.model.reladomo.projection.ProjectionElementReladomoNode;
import cool.klass.model.reladomo.projection.ProjectionWithReferencePropertyReladomoNode;
import cool.klass.model.reladomo.projection.ReladomoProjectionConverter;
import cool.klass.model.reladomo.projection.RootReladomoNode;
import cool.klass.model.reladomo.projection.SubClassReladomoNode;
import cool.klass.model.reladomo.projection.SuperClassReladomoNode;
import cool.klass.serialization.jackson.jsonview.KlassJsonView;
import cool.klass.serialization.jackson.model.data.property.SerializeValueToJsonFieldPrimitiveTypeVisitor;
import org.eclipse.collections.api.map.MutableMap;

public class ReladomoJsonViewSerializer extends JsonSerializer<MithraObject> {

	@Nonnull
	private final DomainModel domainModel;

	@Nonnull
	private final DataStore dataStore;

	public ReladomoJsonViewSerializer(@Nonnull DomainModel domainModel, @Nonnull DataStore dataStore) {
		this.domainModel = Objects.requireNonNull(domainModel);
		this.dataStore = Objects.requireNonNull(dataStore);
	}

	@Override
	public void serialize(
		@Nonnull MithraObject mithraObject,
		@Nonnull JsonGenerator jsonGenerator,
		@Nonnull SerializerProvider serializers
	) throws IOException {
		Class<?> activeViewClass = serializers.getActiveView();
		Objects.requireNonNull(activeViewClass, () ->
			String.format(
				"Could not find json serializer for %s. Usually this is caused by a missing @JsonView() annotation.",
				mithraObject.getClass().getCanonicalName()
			)
		);

		if (!KlassJsonView.class.isAssignableFrom(activeViewClass)) {
			throw new IllegalStateException(activeViewClass.getCanonicalName());
		}

		KlassJsonView klassJsonView = this.instantiate(activeViewClass);
		String projectionName = klassJsonView.getProjectionName();
		Projection projection = this.domainModel.getProjectionByName(projectionName);
		Objects.requireNonNull(projection);

		Klass klass = this.domainModel.getClassByName(mithraObject.getClass().getSimpleName());

		var reladomoProjectionConverter = new ReladomoProjectionConverter();
		RootReladomoNode projectionReladomoNode = reladomoProjectionConverter.getRootReladomoNode(klass, projection);

		// This would work if we consistently used the same DomainModel everywhere (instead of sometimes compiled and sometimes code generated).
		// Projection projection = this.domainModel.getProjections().selectInstancesOf(activeView).getOnly();
		this.serialize(mithraObject, jsonGenerator, projectionReladomoNode);
	}

	private void serialize(
		@Nonnull MithraObject mithraObject,
		@Nonnull JsonGenerator jsonGenerator,
		@Nonnull ProjectionElementReladomoNode projectionReladomoNode
	) throws IOException {
		jsonGenerator.writeStartObject();
		try {
			boolean hasPolymorphicChildren = projectionReladomoNode.hasPolymorphicChildren();
			if (hasPolymorphicChildren) {
				Klass type = (Klass) projectionReladomoNode.getType();
				Klass mostSpecificSubclass = this.dataStore.getMostSpecificSubclass(mithraObject, type);
				jsonGenerator.writeStringField("__typename", mostSpecificSubclass.getFullyQualifiedName());
			}

			this.handleObjectMembers(mithraObject, jsonGenerator, projectionReladomoNode);
		} finally {
			jsonGenerator.writeEndObject();
		}
	}

	private void handleObjectMembers(
		@Nonnull MithraObject mithraObject,
		@Nonnull JsonGenerator jsonGenerator,
		@Nonnull ProjectionElementReladomoNode projectionReladomoNode
	) throws IOException {
		Objects.requireNonNull(mithraObject);

		// TODO: Use listener?
		MutableMap<String, ProjectionElementReladomoNode> children = projectionReladomoNode.getChildren();
		for (ProjectionElementReladomoNode projectionElementReladomoNode : children) {
			if (
				projectionElementReladomoNode
				instanceof ProjectionDataTypePropertyReladomoNode projectionDataTypePropertyReladomoNode
			) {
				this.handleProjectionPrimitiveMember(
					jsonGenerator,
					mithraObject,
					projectionDataTypePropertyReladomoNode
				);
			} else if (
				projectionElementReladomoNode
				instanceof ProjectionWithReferencePropertyReladomoNode projectionWithReferencePropertyReladomoNode
			) {
				this.handleProjectionWithReferenceProperty(
					jsonGenerator,
					mithraObject,
					projectionWithReferencePropertyReladomoNode
				);
			} else if (projectionElementReladomoNode instanceof SuperClassReladomoNode superClassReladomoNode) {
				Classifier owningClassifier = superClassReladomoNode.getOwningClassifier();
				Classifier type = superClassReladomoNode.getType();
				if (((Klass) owningClassifier).getSuperClass().get() != type) {
					throw new AssertionError("Expected superclass of " + owningClassifier + " to be " + type);
				}
				Object superClass = this.dataStore.getSuperClass(mithraObject, (Klass) owningClassifier);
				this.handleObjectMembers((MithraObject) superClass, jsonGenerator, superClassReladomoNode);
			} else if (projectionElementReladomoNode instanceof SubClassReladomoNode subClassReladomoNode) {
				Classifier owningClassifier = subClassReladomoNode.getOwningClassifier();
				Classifier type = subClassReladomoNode.getType();
				if (((Klass) type).getSuperClass().get() != owningClassifier) {
					throw new AssertionError("Expected subclass of " + owningClassifier + " to be " + type);
				}
				Object subClass = this.dataStore.getSubClass(mithraObject, (Klass) owningClassifier, (Klass) type);
				// TODO: There are two separate concepts of definite subclasses and polymorphic projections.
				// In other words, we should know in advance whether null is ok here.
				if (subClass != null) {
					this.handleObjectMembers((MithraObject) subClass, jsonGenerator, subClassReladomoNode);
				}
			} else {
				throw new AssertionError(projectionElementReladomoNode.getClass().getSimpleName());
			}
		}
	}

	@Nonnull
	private KlassJsonView instantiate(@Nonnull Class<?> activeViewClass) {
		try {
			return activeViewClass.asSubclass(KlassJsonView.class).newInstance();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private void handleProjectionPrimitiveMember(
		@Nonnull JsonGenerator jsonGenerator,
		@Nonnull MithraObject mithraObject,
		@Nonnull ProjectionDataTypePropertyReladomoNode projectionDataTypePropertyReladomoNode
	) throws IOException {
		Objects.requireNonNull(mithraObject);

		DataTypeProperty property = projectionDataTypePropertyReladomoNode.getProperty();
		String propertyName = projectionDataTypePropertyReladomoNode.getName();
		DataType dataType = projectionDataTypePropertyReladomoNode.getType();

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
		@Nonnull ProjectionWithReferencePropertyReladomoNode projectionWithReferencePropertyReladomoNode
	) throws IOException {
		ReferenceProperty referenceProperty = projectionWithReferencePropertyReladomoNode.getReferenceProperty();
		Multiplicity multiplicity = referenceProperty.getMultiplicity();
		String associationEndName = referenceProperty.getName();

		if (multiplicity.isToMany()) {
			Object value = this.dataStore.getToMany(mithraObject, referenceProperty);
			MithraList<MithraObject> mithraList = (MithraList<MithraObject>) Objects.requireNonNull(value);

			// TODO: Add configuration to disable serialization of empty lists
			jsonGenerator.writeArrayFieldStart(associationEndName);
			try {
				mithraList.forEachWithCursor((eachChildValue) ->
					this.recurse(
						(MithraObject) eachChildValue,
						jsonGenerator,
						projectionWithReferencePropertyReladomoNode
					)
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
			this.recurse((MithraObject) value, jsonGenerator, projectionWithReferencePropertyReladomoNode);
		}
	}

	public boolean recurse(
		@Nonnull MithraObject eachChildValue,
		@Nonnull JsonGenerator jsonGenerator,
		@Nonnull ProjectionElementReladomoNode projectionReladomoNode
	) {
		try {
			this.serialize(eachChildValue, jsonGenerator, projectionReladomoNode);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return true;
	}
}
