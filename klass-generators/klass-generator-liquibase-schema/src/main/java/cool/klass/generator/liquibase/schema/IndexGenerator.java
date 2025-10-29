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

package cool.klass.generator.liquibase.schema;

import java.util.Objects;

import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableOrderedMap;

public final class IndexGenerator {

    private IndexGenerator() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static String getIndex(Klass klass, int ordinal) {
        String tableName = TableGenerator.TABLE_NAME_CONVERTER.convert(klass.getName());

        MutableOrderedMap<AssociationEnd, MutableOrderedMap<DataTypeProperty, DataTypeProperty>> foreignKeyConstraints =
            klass.getForeignKeys();

        MutableList<String> result = foreignKeyConstraints
            .keyValuesView()
            .collect(keyValuePair ->
                getForeignKeyIndex(keyValuePair.getOne(), keyValuePair.getTwo(), klass, tableName, ordinal)
            )
            .reject(String::isEmpty)
            .toList();

        if (!Objects.equals(result, result.distinct())) {
            throw new AssertionError(
                "Duplicate foreign key index detected for " +
                tableName +
                " in " +
                klass.getName() +
                ". Indexes: " +
                result
            );
        }

        return result.makeString("");
    }

    private static String getForeignKeyIndex(
        AssociationEnd associationEnd,
        MutableOrderedMap<DataTypeProperty, DataTypeProperty> dataTypeProperties,
        Klass klass,
        String tableName,
        int ordinal
    ) {
        String constraintName =
            tableName + "_IDX_" + TableGenerator.COLUMN_NAME_CONVERTER.convert(associationEnd.getName());

        ImmutableList<DataTypeProperty> toProperties = klass.getDataTypeProperties().select(DataTypeProperty::isTo);

        ImmutableList<DataTypeProperty> allKeyProperties = dataTypeProperties
            .keysView()
            .asLazy()
            .concatenate(toProperties)
            .toImmutableList();

        if (isPrefixList(allKeyProperties, getAllKeyProperties(klass))) {
            return "";
        }

        ImmutableList<String> foreignKeyColumns = allKeyProperties
            .collect(DataTypeProperty::getName)
            .collect(TableGenerator.COLUMN_NAME_CONVERTER::convert)
            .collect(columnName -> "            <column name=\"" + columnName + "\" />\n");

        // language=XML
        return (
            "    <changeSet author=\"Klass\" id=\"initial-indices-" +
            ordinal +
            "-" +
            constraintName +
            "\">\n" +
            "        <createIndex\n" +
            "                indexName=\"" +
            constraintName +
            "\"\n" +
            "                tableName=\"" +
            tableName +
            "\">\n" +
            foreignKeyColumns.makeString("") +
            "        </createIndex>\n" +
            "    </changeSet>\n\n"
        );
    }

    private static boolean isPrefixList(ImmutableList<DataTypeProperty> list1, ImmutableList<DataTypeProperty> list2) {
        if (list1.size() > list2.size()) {
            return false;
        }

        return list1.equals(list2.subList(0, list1.size()));
    }

    private static ImmutableList<DataTypeProperty> getAllKeyProperties(Klass klass) {
        ImmutableList<DataTypeProperty> toProperties = klass.getDataTypeProperties().select(DataTypeProperty::isTo);
        return klass.getKeyProperties().newWithAll(toProperties);
    }
}
