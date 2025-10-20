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

package cool.klass.generator.reladomo.interfacefile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nonnull;

import com.gs.fw.common.mithra.generator.metamodel.AsOfAttributeInterfaceType;
import com.gs.fw.common.mithra.generator.metamodel.AttributeInterfaceType;
import com.gs.fw.common.mithra.generator.metamodel.MithraGeneratorMarshaller;
import com.gs.fw.common.mithra.generator.metamodel.MithraInterface;
import com.gs.fw.common.mithra.generator.metamodel.RelationshipInterfaceType;
import com.gs.fw.common.mithra.generator.metamodel.TimezoneConversionType;
import cool.klass.generator.reladomo.AbstractReladomoGenerator;
import cool.klass.generator.reladomo.CriteriaToRelationshipVisitor;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Interface;
import cool.klass.model.meta.domain.api.NamedElement;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.criteria.Criteria;
import cool.klass.model.meta.domain.api.criteria.CriteriaVisitor;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import org.eclipse.collections.api.list.ImmutableList;

public class ReladomoInterfaceFileGenerator extends AbstractReladomoGenerator {

	public ReladomoInterfaceFileGenerator(@Nonnull DomainModel domainModel) {
		super(domainModel);
	}

	public void writeObjectFiles(@Nonnull Path outputPath) throws IOException {
		for (Interface anInterface : this.domainModel.getInterfaces()) {
			this.writeObjectFile(outputPath, anInterface);
		}
	}

	private void writeObjectFile(@Nonnull Path outputPath, @Nonnull Interface anInterface) throws IOException {
		var mithraGeneratorMarshaller = new MithraGeneratorMarshaller();
		mithraGeneratorMarshaller.setIndent(true);
		var stringBuilder = new StringBuilder();

		this.convertAndMarshall(anInterface, mithraGeneratorMarshaller, stringBuilder);

		String xmlString = this.sanitizeXmlString(stringBuilder);

		// TODO: Arrange Reladomo xmls in relative paths
		Path fullPath = outputPath.resolve(anInterface.getName() + ".xml");
		this.printStringToFile(fullPath, xmlString);
	}

	private void convertAndMarshall(
		@Nonnull Interface anInterface,
		@Nonnull MithraGeneratorMarshaller mithraGeneratorMarshaller,
		StringBuilder stringBuilder
	) throws IOException {
		MithraInterface mithraInterface = this.convertToMithraInterface(anInterface);
		mithraGeneratorMarshaller.marshall(stringBuilder, mithraInterface);
	}

	@Nonnull
	private MithraInterface convertToMithraInterface(@Nonnull Interface anInterface) {
		MithraInterface mithraInterface = new MithraInterface();
		mithraInterface.setPackageName(anInterface.getPackageName());
		mithraInterface.setClassName(anInterface.getName());

		ImmutableList<String> superInterfaceNames = anInterface.getInterfaces().collect(NamedElement::getName);
		mithraInterface.setSuperInterfaces(superInterfaceNames.castToList());

		ImmutableList<AsOfAttributeInterfaceType> asOfAttributeTypes = anInterface
			.getDataTypeProperties()
			.select(DataTypeProperty::isTemporalRange)
			.collect(this::convertToAsOfAttributeType);

		ImmutableList<AttributeInterfaceType> attributeTypes = anInterface
			.getDataTypeProperties()
			.reject(DataTypeProperty::isTemporal)
			.collect(this::convertToAttributeType);

		// TODO: Test that private properties are not included in Projections
		// TODO: Add foreign keys

		mithraInterface.setAsOfAttributes(asOfAttributeTypes.castToList());
		mithraInterface.setAttributes(attributeTypes.castToList());

		// TODO: Relationships
		// mithraInterface.setRelationships(this.convertRelationships(anInterface.getAssociationEnds()));

		return mithraInterface;
	}

	private List<RelationshipInterfaceType> convertRelationships(
		@Nonnull ImmutableList<AssociationEnd> associationEnds
	) {
		return associationEnds
			.select(
				(associationEnd) -> associationEnd == associationEnd.getOwningAssociation().getTargetAssociationEnd()
			)
			.collectWith(this::convertRelationship, true)
			.castToList();
	}

	@Nonnull
	private RelationshipInterfaceType convertRelationship(@Nonnull AssociationEnd associationEnd, boolean reverse) {
		AssociationEnd opposite = associationEnd.getOpposite();
		RelationshipInterfaceType relationshipType = new RelationshipInterfaceType();
		relationshipType.setName(associationEnd.getName());
		relationshipType.setCardinality(this.getCardinality(associationEnd, opposite));
		relationshipType.setRelatedObject(associationEnd.getType().getName());
		String relationshipString = this.getRelationshipString(
			associationEnd.getOwningAssociation().getCriteria(),
			reverse
		);
		relationshipType._setValue(relationshipString);
		return relationshipType;
	}

	@Nonnull
	private String getRelationshipString(@Nonnull Criteria criteria, boolean reverse) {
		StringBuilder stringBuilder = new StringBuilder();
		CriteriaVisitor visitor = new CriteriaToRelationshipVisitor(stringBuilder, reverse);
		criteria.visit(visitor);
		return stringBuilder.toString();
	}

	@Nonnull
	private AsOfAttributeInterfaceType convertToAsOfAttributeType(@Nonnull DataTypeProperty dataTypeProperty) {
		AsOfAttributeInterfaceType asOfAttributeType = new AsOfAttributeInterfaceType();
		this.convertToAsOfAttributeType(dataTypeProperty, asOfAttributeType);
		return asOfAttributeType;
	}

	private void convertToAsOfAttributeType(
		@Nonnull DataTypeProperty dataTypeProperty,
		@Nonnull AsOfAttributeInterfaceType asOfAttributeType
	) {
		String propertyName = dataTypeProperty.getName();

		asOfAttributeType.setName(propertyName);
		asOfAttributeType.setToIsInclusive(false);
		asOfAttributeType.setInfinityDate("[cool.klass.data.store.reladomo.UtcInfinityTimestamp.getDefaultInfinity()]");
		asOfAttributeType.setInfinityIsNull(false);
		// TODO: futureExpiringRowsExist is a de-optimization that allows for future times, and also makes the end dates more understandable. Add a better explanation and allow this to be customizable.

		TimezoneConversionType timezoneConversion = new TimezoneConversionType();
		timezoneConversion.with("convert-to-utc", asOfAttributeType);
		asOfAttributeType.setTimezoneConversion(timezoneConversion);

		if (dataTypeProperty.isValid()) {
			asOfAttributeType.setIsProcessingDate(false);
		} else if (dataTypeProperty.isSystem()) {
			asOfAttributeType.setIsProcessingDate(true);
		} else {
			throw new AssertionError(propertyName);
		}
	}

	@Nonnull
	private AttributeInterfaceType convertToAttributeType(@Nonnull DataTypeProperty dataTypeProperty) {
		AttributeInterfaceType attributeType = new AttributeInterfaceType();
		this.convertToAttributeType(dataTypeProperty, attributeType);
		return attributeType;
	}

	private void convertToAttributeType(
		@Nonnull DataTypeProperty dataTypeProperty,
		@Nonnull AttributeInterfaceType attributeType
	) {
		String propertyName = dataTypeProperty.getName();
		attributeType.setName(propertyName);
		this.handleType(attributeType, dataTypeProperty);
	}

	private void handleType(@Nonnull AttributeInterfaceType attributeType, DataTypeProperty dataTypeProperty) {
		if (dataTypeProperty instanceof EnumerationProperty) {
			attributeType.setJavaType("String");
		}

		if (dataTypeProperty instanceof PrimitiveProperty primitiveProperty) {
			PrimitiveType primitiveType = primitiveProperty.getType();
			primitiveType.visit(new AttributeInterfaceTypeVisitor(attributeType));
		}
	}
}
