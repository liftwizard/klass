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

import java.util.Optional;

import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import org.eclipse.collections.api.map.MutableOrderedMap;

public final class FkGenerator {

    private FkGenerator() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static Optional<String> getFk(Klass klass) {
        MutableOrderedMap<AssociationEnd, MutableOrderedMap<DataTypeProperty, DataTypeProperty>> foreignKeys =
            klass.getForeignKeys();
        if (foreignKeys.isEmpty()) {
            return Optional.empty();
        }

        if (klass.isTemporal()) {
            return Optional.empty();
        }

        String result = foreignKeys
            .keyValuesView()
            .collect((keyValuePair) -> getFk(klass, keyValuePair.getOne(), keyValuePair.getTwo()))
            .makeString("");
        return Optional.of(result);
    }

    private static String getFk(
        Klass klass,
        AssociationEnd associationEnd,
        MutableOrderedMap<DataTypeProperty, DataTypeProperty> dataTypeProperties
    ) {
        String tableName = DdlGenerator.TABLE_NAME_CONVERTER.convert(klass.getName());
        String constraintName =
            tableName + "_FK_" + DdlGenerator.TABLE_NAME_CONVERTER.convert(associationEnd.getName());

        String foreignKeyColumnNames = dataTypeProperties
            .keysView()
            .collect(DataTypeProperty::getName)
            .collect(DdlGenerator.COLUMN_NAME_CONVERTER::convert)
            .makeString(", ");

        String referencedTableName = DdlGenerator.TABLE_NAME_CONVERTER.convert(associationEnd.getType().getName());

        String referencedKeyColumnNames = dataTypeProperties
            .valuesView()
            .collect(DataTypeProperty::getName)
            .collect(DdlGenerator.COLUMN_NAME_CONVERTER::convert)
            .makeString(", ");

        // language=SQL
        String format = """
            alter table %s add constraint %s foreign key (
                %s
            )
            references %s(
                %s
            );

            """;

        return format.formatted(
            tableName,
            constraintName,
            foreignKeyColumnNames,
            referencedTableName,
            referencedKeyColumnNames
        );
    }
}
