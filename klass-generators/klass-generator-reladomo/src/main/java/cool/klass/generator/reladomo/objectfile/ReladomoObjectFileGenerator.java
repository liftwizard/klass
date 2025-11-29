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

package cool.klass.generator.reladomo.objectfile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.gs.fw.common.mithra.generator.metamodel.AsOfAttributePureType;
import com.gs.fw.common.mithra.generator.metamodel.AsOfAttributeType;
import com.gs.fw.common.mithra.generator.metamodel.AttributePureType;
import com.gs.fw.common.mithra.generator.metamodel.AttributeType;
import com.gs.fw.common.mithra.generator.metamodel.MithraCommonObjectType;
import com.gs.fw.common.mithra.generator.metamodel.MithraGeneratorMarshaller;
import com.gs.fw.common.mithra.generator.metamodel.MithraObject;
import com.gs.fw.common.mithra.generator.metamodel.MithraPureObject;
import com.gs.fw.common.mithra.generator.metamodel.ObjectType;
import com.gs.fw.common.mithra.generator.metamodel.RelationshipType;
import com.gs.fw.common.mithra.generator.metamodel.TimezoneConversionType;
import cool.klass.generator.reladomo.AbstractReladomoGenerator;
import cool.klass.generator.reladomo.CriteriaToRelationshipVisitor;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Interface;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.NamedElement;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.criteria.Criteria;
import cool.klass.model.meta.domain.api.criteria.CriteriaVisitor;
import cool.klass.model.meta.domain.api.order.OrderBy;
import cool.klass.model.meta.domain.api.order.OrderByDirection;
import cool.klass.model.meta.domain.api.order.OrderByMemberReferencePath;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.validation.NumericPropertyValidation;
import cool.klass.model.meta.domain.api.value.ThisMemberReferencePath;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

// TODO: ⬆ Generate default order-bys (or infer default order-bys) and generate order-bys on association ends.

public class ReladomoObjectFileGenerator extends AbstractReladomoGenerator {

    public static final Converter<String, String> TO_LOWER = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL);
    public static final Converter<String, String> TABLE_NAME_CONVERTER = CaseFormat.UPPER_CAMEL.converterTo(
        CaseFormat.UPPER_UNDERSCORE
    );
    public static final Converter<String, String> COLUMN_NAME_CONVERTER = CaseFormat.LOWER_CAMEL.converterTo(
        CaseFormat.UPPER_UNDERSCORE
    );

    public ReladomoObjectFileGenerator(@Nonnull DomainModel domainModel) {
        super(domainModel);
    }

    public void writeObjectFiles(@Nonnull Path outputPath) throws IOException {
        for (Klass klass : this.domainModel.getClasses()) {
            this.writeObjectFile(outputPath, klass);
        }
    }

    private void writeObjectFile(@Nonnull Path outputPath, @Nonnull Klass klass) throws IOException {
        var mithraGeneratorMarshaller = new MithraGeneratorMarshaller();
        mithraGeneratorMarshaller.setIndent(true);
        var stringBuilder = new StringBuilder();

        this.convertAndMarshall(klass, mithraGeneratorMarshaller, stringBuilder);

        String xmlString = this.sanitizeXmlString(stringBuilder);

        // TODO: Arrange Reladomo xmls in relative paths
        Path fullPath = outputPath.resolve(klass.getName() + ".xml");
        this.printStringToFile(fullPath, xmlString);
    }

    private void convertAndMarshall(
        @Nonnull Klass klass,
        @Nonnull MithraGeneratorMarshaller mithraGeneratorMarshaller,
        StringBuilder stringBuilder
    ) throws IOException {
        if (klass.isTransient()) {
            MithraPureObject mithraPureObject = this.convertToMithraPureObject(klass);
            mithraGeneratorMarshaller.marshall(stringBuilder, mithraPureObject);
        } else {
            MithraObject mithraObject = this.convertToMithraObject(klass);
            mithraGeneratorMarshaller.marshall(stringBuilder, mithraObject);
        }
    }

    @Nonnull
    private MithraPureObject convertToMithraPureObject(@Nonnull Klass klass) {
        MithraPureObject mithraPureObject = new MithraPureObject();

        this.convertCommonObject(klass, mithraPureObject);

        ImmutableList<AsOfAttributePureType> asOfAttributeTypes = klass
            .getDataTypeProperties()
            .select(DataTypeProperty::isTemporalRange)
            .collect(this::convertToAsOfAttributePureType);

        ImmutableList<AttributePureType> attributeTypes = this.getDataTypeProperties(klass).collect(
            (dataTypeProperty) -> this.convertToAttributePureType(klass, dataTypeProperty)
        );

        // TODO: Test that private properties are not included in Projections
        // TODO: Add foreign keys

        mithraPureObject.setAsOfAttributes(asOfAttributeTypes.castToList());
        mithraPureObject.setAttributes(attributeTypes.castToList());

        mithraPureObject.setRelationships(this.convertRelationships(klass));

        return mithraPureObject;
    }

    @Nonnull
    private MithraObject convertToMithraObject(@Nonnull Klass klass) {
        MithraObject mithraObject = new MithraObject();
        this.convertCommonObject(klass, mithraObject);

        mithraObject.setDefaultTable(quote(TABLE_NAME_CONVERTER.convert(klass.getName())));

        ImmutableList<String> superInterfaceNames = klass.getInterfaces().collect(NamedElement::getName);
        mithraObject.setMithraInterfaces(superInterfaceNames.castToList());

        ImmutableList<AsOfAttributeType> asOfAttributeTypes = klass
            .getDataTypeProperties()
            .select(DataTypeProperty::isTemporalRange)
            .collect(this::convertToAsOfAttributeType);

        ImmutableList<AttributeType> attributeTypes = this.getDataTypeProperties(klass)
            .reject(DataTypeProperty::isTemporal)
            .reject(DataTypeProperty::isDerived)
            .collect((dataTypeProperty) -> this.convertToAttributeType(klass, dataTypeProperty));

        mithraObject.setAsOfAttributes(asOfAttributeTypes.castToList());
        mithraObject.setAttributes(attributeTypes.castToList());

        mithraObject.setRelationships(this.convertRelationships(klass));

        return mithraObject;
    }

    private ImmutableList<DataTypeProperty> getDataTypeProperties(@Nonnull Klass klass) {
        ImmutableList<String> superClassPropertyNames = klass
            .getSuperClass()
            .map(Classifier::getDataTypeProperties)
            .orElseGet(Lists.immutable::empty)
            .collect(NamedElement::getName);

        return klass
            .getDataTypeProperties()
            .select((each) -> each.isKey() || !superClassPropertyNames.contains(each.getName()));
    }

    private void convertCommonObject(@Nonnull Klass klass, @Nonnull MithraCommonObjectType mithraCommonObject) {
        ObjectType objectType = new ObjectType();
        objectType.with("transactional", mithraCommonObject);
        mithraCommonObject.setObjectType(objectType);

        mithraCommonObject.setPackageName(klass.getPackageName());
        mithraCommonObject.setClassName(klass.getName());
        mithraCommonObject.setInitializePrimitivesToNull(true);
    }

    private List<RelationshipType> convertRelationships(@Nonnull Klass klass) {
        ImmutableList<AssociationEnd> associationEnds = klass.getDeclaredAssociationEnds();

        for (AssociationEnd associationEnd : associationEnds) {
            int count = 0;
            if (this.isForwardRelationship(associationEnd)) {
                count++;
            }
            if (this.isReverseRelationship(associationEnd)) {
                count++;
            }
            if (this.isForwardRelationship(associationEnd.getOpposite())) {
                count++;
            }
            if (this.isReverseRelationship(associationEnd.getOpposite())) {
                count++;
            }
            if (count != 1) {
                throw new AssertionError("Count: " + count + " " + associationEnd);
            }
        }

        ImmutableList<AssociationEnd> forward = associationEnds.select(this::isForwardRelationship);
        ImmutableList<AssociationEnd> reverse = associationEnds.select(this::isReverseRelationship);

        ImmutableList<RelationshipType> relationshipTypes = forward.collectWith(this::convertRelationship, false);
        ImmutableList<RelationshipType> reverseAbstractRelationshipTypes = reverse.collectWith(
            this::convertRelationship,
            true
        );
        ImmutableList<RelationshipType> subClassRelationships = klass
            .getSubClasses()
            .collect(this::convertSubClassRelationship);

        return relationshipTypes
            .newWithAll(reverseAbstractRelationshipTypes)
            .newWithAll(subClassRelationships)
            .castToList();
    }

    private boolean isForwardRelationship(AssociationEnd associationEnd) {
        return this.isForwardDeclared(associationEnd) && !this.mustBeOnResultType(associationEnd.getOpposite());
    }

    private boolean isReverseRelationship(AssociationEnd associationEnd) {
        return this.isReverseDeclared(associationEnd) && this.mustBeOnResultType(associationEnd);
    }

    private boolean isForwardDeclared(AssociationEnd associationEnd) {
        return associationEnd == associationEnd.getOwningAssociation().getTargetAssociationEnd();
    }

    private boolean isReverseDeclared(AssociationEnd associationEnd) {
        return associationEnd == associationEnd.getOwningAssociation().getSourceAssociationEnd();
    }

    private boolean mustBeOnResultType(AssociationEnd associationEnd) {
        return associationEnd.isOwned();
    }

    @Nonnull
    private RelationshipType convertRelationship(@Nonnull AssociationEnd associationEnd, boolean reverse) {
        if (this.isForwardRelationship(associationEnd) && this.isReverseRelationship(associationEnd)) {
            throw new AssertionError(associationEnd);
        }

        AssociationEnd opposite = associationEnd.getOpposite();
        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setName(associationEnd.getName());
        relationshipType.setReverseRelationshipName(opposite.getName());
        relationshipType.setCardinality(this.getCardinality(associationEnd, opposite));
        relationshipType.setRelatedIsDependent(associationEnd.isOwned());
        relationshipType.setRelatedObject(associationEnd.getType().getName());
        relationshipType.setOrderBy(this.getOrderBy(associationEnd));
        String relationshipString = this.getRelationshipString(
            associationEnd.getOwningAssociation().getCriteria(),
            reverse
        );
        relationshipType._setValue(relationshipString);
        return relationshipType;
    }

    private RelationshipType convertSubClassRelationship(Klass subClass) {
        Klass superClass = subClass.getSuperClass().get();

        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setName(TO_LOWER.convert(subClass.getName()) + "SubClass");
        relationshipType.setReverseRelationshipName(TO_LOWER.convert(superClass.getName()) + "SuperClass");
        relationshipType.setCardinality(this.getCardinalityType("one-to-one"));
        relationshipType.setRelatedIsDependent(true);
        relationshipType.setRelatedObject(subClass.getName());

        String relationshipString = superClass
            .getKeyProperties()
            .collectWith(this::getRelationshipClause, subClass)
            .makeString(" and ");

        relationshipType._setValue(relationshipString);
        return relationshipType;
    }

    private String getRelationshipClause(DataTypeProperty keyProperty, Klass subClass) {
        return "this.%s = %s.%s".formatted(keyProperty.getName(), subClass.getName(), keyProperty.getName());
    }

    @Nonnull
    private String getRelationshipString(@Nonnull Criteria criteria, boolean reverse) {
        StringBuilder stringBuilder = new StringBuilder();
        CriteriaVisitor visitor = new CriteriaToRelationshipVisitor(stringBuilder, reverse);
        criteria.visit(visitor);
        return stringBuilder.toString();
    }

    @Nullable
    private String getOrderBy(@Nonnull AssociationEnd associationEnd) {
        ImmutableList<String> orderByStrings = associationEnd
            .getOrderBy()
            .map(OrderBy::getOrderByMemberReferencePaths)
            .orElseGet(Lists.immutable::empty)
            .selectWith(this::isConvertibleToOrderBy, associationEnd.getType())
            .collect(this::convertOrderByMemberReferencePath);
        return orderByStrings.isEmpty() ? null : orderByStrings.makeString();
    }

    private boolean isConvertibleToOrderBy(
        @Nonnull OrderByMemberReferencePath orderByMemberReferencePath,
        Klass klass
    ) {
        ThisMemberReferencePath thisMemberReferencePath = orderByMemberReferencePath.getThisMemberReferencePath();
        DataTypeProperty property = thisMemberReferencePath.getProperty();

        if (property.isDerived()) {
            return false;
        }

        // Reladomo only supports simple property orderBys, like (title asc), not paths like (question.title asc)
        if (orderByMemberReferencePath.getThisMemberReferencePath().getAssociationEnds().notEmpty()) {
            return false;
        }

        if (property.getOwningClassifier() == klass) {
            return true;
        }

        if (
            property.getOwningClassifier() instanceof Interface anInterface
            && klass.getInterfaces().contains(anInterface)
        ) {
            return true;
        }

        return false;
    }

    private String convertOrderByMemberReferencePath(@Nonnull OrderByMemberReferencePath orderByMemberReferencePath) {
        DataTypeProperty property = orderByMemberReferencePath.getThisMemberReferencePath().getProperty();
        String propertyName = property.getName();
        String orderByDirectionString = this.getOrderByDirectionString(orderByMemberReferencePath);
        return String.format("%s %s", propertyName, orderByDirectionString);
    }

    @Nonnull
    private String getOrderByDirectionString(@Nonnull OrderByMemberReferencePath orderByMemberReferencePath) {
        OrderByDirection orderByDirection = orderByMemberReferencePath
            .getOrderByDirectionDeclaration()
            .getOrderByDirection();
        return switch (orderByDirection) {
            case ASCENDING -> "asc";
            case DESCENDING -> "desc";
            default -> throw new AssertionError();
        };
    }

    @Nonnull
    private AsOfAttributeType convertToAsOfAttributeType(@Nonnull DataTypeProperty dataTypeProperty) {
        AsOfAttributeType asOfAttributeType = new AsOfAttributeType();
        this.convertToAsOfAttributeType(dataTypeProperty, asOfAttributeType);
        return asOfAttributeType;
    }

    private void convertToAsOfAttributeType(
        @Nonnull DataTypeProperty dataTypeProperty,
        @Nonnull AsOfAttributePureType asOfAttributeType
    ) {
        String propertyName = dataTypeProperty.getName();
        // TODO: Use actual temporal properties
        String fromName = propertyName + "From";
        String toName = propertyName + "To";
        String fromColumnName = quote(COLUMN_NAME_CONVERTER.convert(fromName));
        String toColumnName = quote(COLUMN_NAME_CONVERTER.convert(toName));

        asOfAttributeType.setName(propertyName);
        asOfAttributeType.setFromColumnName(fromColumnName);
        asOfAttributeType.setToColumnName(toColumnName);
        asOfAttributeType.setToIsInclusive(false);
        asOfAttributeType.setInfinityDate(
            "[com.gs.fw.common.mithra.util.DefaultInfinityTimestamp.getDefaultInfinity()]"
        );
        asOfAttributeType.setInfinityIsNull(false);
        // TODO: futureExpiringRowsExist is a de-optimization that allows for future times, and also makes the end dates more understandable. Add a better explanation and allow this to be customizable.
        asOfAttributeType.setFutureExpiringRowsExist(true);
        asOfAttributeType.setFinalGetter(true);

        TimezoneConversionType timezoneConversion = new TimezoneConversionType();
        timezoneConversion.with("convert-to-utc", asOfAttributeType);
        asOfAttributeType.setTimezoneConversion(timezoneConversion);

        if (propertyName.equals("valid")) {
            asOfAttributeType.setIsProcessingDate(false);
        } else if (propertyName.equals("system")) {
            asOfAttributeType.setDefaultIfNotSpecified(
                "[com.gs.fw.common.mithra.util.DefaultInfinityTimestamp.getDefaultInfinity()]"
            );
            asOfAttributeType.setIsProcessingDate(true);
        } else {
            throw new AssertionError(propertyName);
        }
    }

    @Nonnull
    private AsOfAttributePureType convertToAsOfAttributePureType(@Nonnull DataTypeProperty dataTypeProperty) {
        AsOfAttributePureType asOfAttributeType = new AsOfAttributePureType();
        this.convertToAsOfAttributeType(dataTypeProperty, asOfAttributeType);
        return asOfAttributeType;
    }

    @Nonnull
    private AttributeType convertToAttributeType(
        @Nonnull Klass owningClass,
        @Nonnull DataTypeProperty dataTypeProperty
    ) {
        AttributeType attributeType = new AttributeType();
        this.convertToAttributeType(owningClass, dataTypeProperty, attributeType);
        return attributeType;
    }

    private void convertToAttributeType(
        @Nonnull Klass owningClass,
        @Nonnull DataTypeProperty dataTypeProperty,
        @Nonnull AttributePureType attributeType
    ) {
        String propertyName = dataTypeProperty.getName();
        attributeType.setName(propertyName);
        attributeType.setColumnName(quote(COLUMN_NAME_CONVERTER.convert(propertyName)));
        attributeType.setPrimaryKey(dataTypeProperty.isKey());
        attributeType.setNullable(dataTypeProperty.isOptional());
        if (dataTypeProperty.isKey() || dataTypeProperty.isFinal()) {
            attributeType.setFinalGetter(true);
        }

        this.handleType(owningClass, dataTypeProperty, attributeType);
    }

    private void handleType(
        @Nonnull Klass owningClass,
        @Nonnull DataTypeProperty dataTypeProperty,
        @Nonnull AttributePureType attributeType
    ) {
        if (dataTypeProperty instanceof EnumerationProperty) {
            attributeType.setJavaType("String");
            attributeType.setTrim(false);

            dataTypeProperty
                .getMaxLengthPropertyValidation()
                .map(NumericPropertyValidation::getNumber)
                .ifPresent(attributeType::setMaxLength);
        }

        if (dataTypeProperty instanceof PrimitiveProperty primitiveProperty) {
            PrimitiveType primitiveType = primitiveProperty.getType();
            primitiveType.visit(new AttributeTypeVisitor(owningClass, primitiveProperty, attributeType));
        }
    }

    @Nonnull
    private AttributePureType convertToAttributePureType(
        @Nonnull Klass owningClass,
        @Nonnull DataTypeProperty dataTypeProperty
    ) {
        AttributePureType attributeType = new AttributePureType();
        this.convertToAttributeType(owningClass, dataTypeProperty, attributeType);
        return attributeType;
    }

    private static String quote(String string) {
        return "\\\"" + string + "\\\"";
    }
}
