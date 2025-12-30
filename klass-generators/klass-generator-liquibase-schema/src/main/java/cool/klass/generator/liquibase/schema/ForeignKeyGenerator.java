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

import java.util.Optional;

import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.api.tuple.Pair;

public final class ForeignKeyGenerator {

	private ForeignKeyGenerator() {
		throw new AssertionError("Suppress default constructor for noninstantiability");
	}

	public static Optional<String> getForeignKeys(Klass klass, int ordinal) {
		MutableOrderedMap<AssociationEnd, MutableOrderedMap<DataTypeProperty, DataTypeProperty>> foreignKeys = klass
			.getForeignKeys()
			.reject((key, value) -> key.getOwningClassifier().isTemporal() || key.getType().isTemporal());
		if (foreignKeys.isEmpty()) {
			return Optional.empty();
		}

		ImmutableList<String> foreignKeyStrings = foreignKeys
			.keyValuesView()
			.reject(ForeignKeyGenerator::isSelfToOneOptional)
			.collect((keyValuePair) -> getForeignKey(keyValuePair.getOne(), keyValuePair.getTwo(), ordinal))
			.toImmutableList();
		String result = foreignKeyStrings.makeString("");
		return Optional.of(result);
	}

	private static boolean isSelfToOneOptional(
		Pair<AssociationEnd, MutableOrderedMap<DataTypeProperty, DataTypeProperty>> pair
	) {
		AssociationEnd associationEnd = pair.getOne();
		boolean result =
			associationEnd.isToSelf()
			&& associationEnd.getMultiplicity().isToOne()
			&& !associationEnd.getMultiplicity().isRequired();
		return result;
	}

	private static String getForeignKey(
		AssociationEnd associationEnd,
		MutableOrderedMap<DataTypeProperty, DataTypeProperty> dataTypeProperties,
		int ordinal
	) {
		String tableName = TableGenerator.TABLE_NAME_CONVERTER.convert(associationEnd.getOwningClassifier().getName());
		String constraintName =
			tableName + "_FK_" + TableGenerator.COLUMN_NAME_CONVERTER.convert(associationEnd.getName());

		String foreignKeyColumnNames = dataTypeProperties
			.keysView()
			.collect(DataTypeProperty::getName)
			.collect(TableGenerator.COLUMN_NAME_CONVERTER::convert)
			.makeString(", ");

		String name = associationEnd.getType().getName();
		String referencedTableName = TableGenerator.TABLE_NAME_CONVERTER.convert(name);

		String referencedKeyColumnNames = dataTypeProperties
			.valuesView()
			.collect(DataTypeProperty::getName)
			.collect(TableGenerator.COLUMN_NAME_CONVERTER::convert)
			.makeString(", ");

		// language=XML
		String format = """
			    <changeSet author="Klass" id="initial-foreign-key-%d-%s">
			        <addForeignKeyConstraint
			                constraintName="%s"
			                baseTableName="%s"
			                baseColumnNames="%s"
			                referencedTableName="%s"
			                referencedColumnNames="%s"
			        />
			    </changeSet>

			""";

		return format.formatted(
			ordinal,
			constraintName,
			constraintName,
			tableName,
			foreignKeyColumnNames,
			referencedTableName,
			referencedKeyColumnNames
		);
	}
}
