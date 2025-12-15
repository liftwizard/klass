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

package cool.klass.generator.relational.schema;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.NamedElement;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableOrderedMap;

public final class IdxGenerator {

    private IdxGenerator() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static String getIdx(Klass klass) {
        String tableName = DdlGenerator.getTableName(klass);
        String constraintName = tableName + "_PK";

        String primaryKeyColumnNames = getPrimaryKeyColumnNames(klass);

        String primaryKeyIndex = "alter table %s add constraint %s primary key (%s);%n".formatted(
            tableName,
            constraintName,
            primaryKeyColumnNames
        );

        MutableOrderedMap<AssociationEnd, MutableOrderedMap<DataTypeProperty, DataTypeProperty>> foreignKeyConstraints =
            klass.getForeignKeys();
        String foreignKeyIndexes = foreignKeyConstraints
            .keyValuesView()
            .collect((keyValuePair) ->
                getForeignKeyIndex(keyValuePair.getOne(), keyValuePair.getTwo(), klass, tableName)
            )
            .makeString("");

        return primaryKeyIndex + foreignKeyIndexes;
    }

    private static String getForeignKeyIndex(
        AssociationEnd associationEnd,
        MutableOrderedMap<DataTypeProperty, DataTypeProperty> dataTypeProperties,
        Klass klass,
        String tableName
    ) {
        String constraintName =
            tableName + "_IDX_" + DdlGenerator.TABLE_NAME_CONVERTER.convert(associationEnd.getName());

        ImmutableList<DataTypeProperty> toProperties = klass.getDataTypeProperties().select(DataTypeProperty::isTo);

        ImmutableList<DataTypeProperty> allKeyProperties = dataTypeProperties
            .keysView()
            .asLazy()
            .concatenate(toProperties)
            .toImmutableList();

        if (isPrefixList(allKeyProperties, getAllKeyProperties(klass))) {
            return "";
        }

        String foreignKeyColumnNames = allKeyProperties
            .collect(DataTypeProperty::getName)
            .collect(DdlGenerator.COLUMN_NAME_CONVERTER::convert)
            .makeString(", ");

        return "create index %s on %s(%s);%n".formatted(constraintName, tableName, foreignKeyColumnNames);
    }

    private static boolean isPrefixList(ImmutableList<DataTypeProperty> list1, ImmutableList<DataTypeProperty> list2) {
        if (list1.size() > list2.size()) {
            return false;
        }

        return list1.equals(list2.subList(0, list1.size()));
    }

    @Nonnull
    private static String getPrimaryKeyColumnNames(Klass klass) {
        return getAllKeyProperties(klass)
            .collect(NamedElement::getName)
            .collect(DdlGenerator.COLUMN_NAME_CONVERTER::convert)
            .makeString(", ");
    }

    private static ImmutableList<DataTypeProperty> getAllKeyProperties(Klass klass) {
        ImmutableList<DataTypeProperty> toProperties = klass.getDataTypeProperties().select(DataTypeProperty::isTo);
        return klass.getKeyProperties().newWithAll(toProperties);
    }
}
