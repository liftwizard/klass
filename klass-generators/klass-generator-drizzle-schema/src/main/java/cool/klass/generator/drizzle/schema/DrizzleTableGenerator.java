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

package cool.klass.generator.drizzle.schema;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.NamedElement;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.api.tuple.Pair;

/**
 * Generates one Drizzle {@code sqliteTable} definition per Klass class.
 *
 * <p>Columns, primary keys, and indexes follow the Liquibase schema generator, with SQLite types. Temporal instants
 * are epoch milliseconds, the open-ended {@code to} sentinel is {@value #FAR_FUTURE_EPOCH_MILLIS}
 * (9999-12-31 23:59:59.999), and every temporal instant gets a VIRTUAL generated {@code _text} column that renders it
 * for humans. Foreign keys are emitted as comments only, never as constraints.
 */
public final class DrizzleTableGenerator
{
	public static final long FAR_FUTURE_EPOCH_MILLIS = 253_402_300_799_999L;

	public static final Converter<String, String> TABLE_NAME_CONVERTER = CaseFormat.UPPER_CAMEL.converterTo(
		CaseFormat.LOWER_UNDERSCORE
	);

	public static final Converter<String, String> TABLE_IDENTIFIER_CONVERTER = CaseFormat.UPPER_CAMEL.converterTo(
		CaseFormat.LOWER_CAMEL
	);

	public static final Converter<String, String> COLUMN_NAME_CONVERTER = CaseFormat.LOWER_CAMEL.converterTo(
		CaseFormat.LOWER_UNDERSCORE
	);

	private DrizzleTableGenerator()
	{
		throw new AssertionError("Suppress default constructor for noninstantiability");
	}

	public static String getTable(@Nonnull Klass klass, @Nonnull MutableSortedSet<String> sqliteCoreImports)
	{
		Objects.requireNonNull(klass);

		String tableName = TABLE_NAME_CONVERTER.convert(klass.getName());
		String tableIdentifier = TABLE_IDENTIFIER_CONVERTER.convert(klass.getName());

		ImmutableList<DataTypeProperty> properties = getColumnProperties(klass);

		String columns = properties.collect((property) -> getColumn(property, sqliteCoreImports)).makeString("");
		String generatedColumns = properties
			.select(DataTypeProperty::isTemporalInstant)
			.collect((property) -> getGeneratedTextColumn(property, sqliteCoreImports))
			.makeString("");

		ImmutableList<String> constraints = getConstraints(klass, tableName, sqliteCoreImports);
		String constraintsSourceCode = constraints.isEmpty() ?
			"" :
			"\t(table) => [\n" + constraints.collect((each) -> each + ",\n").makeString("") + "\t],\n";

		return (
			getRelationshipComments(klass, tableName) +
			"export const " +
			tableIdentifier +
			" = sqliteTable(\n" +
			"\t\"" +
			tableName +
			"\",\n" +
			"\t{\n" +
			columns +
			generatedColumns +
			"\t},\n" +
			constraintsSourceCode +
			");\n"
		);
	}

	private static ImmutableList<DataTypeProperty> getColumnProperties(@Nonnull Klass klass)
	{
		ImmutableList<String> superClassPropertyNames = klass
			.getSuperClass()
			.map(Classifier::getDataTypeProperties)
			.orElseGet(Lists.immutable::empty)
			.collect(NamedElement::getName);

		return klass
			.getDataTypeProperties()
			.select((each) -> each.isKey() || !superClassPropertyNames.contains(each.getName()))
			.reject(DataTypeProperty::isDerived)
			.reject(DataTypeProperty::isTemporalRange);
	}

	private static String getColumn(DataTypeProperty property, MutableSortedSet<String> sqliteCoreImports)
	{
		String columnName = COLUMN_NAME_CONVERTER.convert(property.getName());
		var visitor = new DrizzleSchemaGeneratorDataTypePropertyVisitor(columnName);
		property.visit(visitor);
		String columnBuilder = visitor.getColumnBuilder();
		sqliteCoreImports.add(columnBuilder.substring(0, columnBuilder.indexOf('(')));

		boolean notNull = property.isTemporalInstant() || property.isRequired();
		String nullability = notNull ? ".notNull()" : "";
		String defaultValue = property.isTo() ? ".default(" + FAR_FUTURE_EPOCH_MILLIS + ")" : "";

		return "\t\t" + property.getName() + ": " + columnBuilder + nullability + defaultValue + ",\n";
	}

	private static String getGeneratedTextColumn(DataTypeProperty property, MutableSortedSet<String> sqliteCoreImports)
	{
		sqliteCoreImports.add("text");
		String sourceColumnName = COLUMN_NAME_CONVERTER.convert(property.getName());
		return (
			"\t\t" +
			property.getName() +
			"Text: text(\"" +
			sourceColumnName +
			"_text\").generatedAlwaysAs(\n" +
			"\t\t\tsql`strftime('%Y-%m-%d %H:%M:%f', " +
			sourceColumnName +
			" / 1000.0, 'unixepoch')`,\n" +
			"\t\t\t{\n" +
			"\t\t\t\tmode: \"virtual\",\n" +
			"\t\t\t},\n" +
			"\t\t),\n"
		);
	}

	private static ImmutableList<String> getConstraints(
		Klass klass,
		String tableName,
		MutableSortedSet<String> sqliteCoreImports
	)
	{
		MutableList<String> result = Lists.mutable.empty();

		ImmutableList<DataTypeProperty> toProperties = klass.getDataTypeProperties().select(DataTypeProperty::isTo);
		ImmutableList<DataTypeProperty> primaryKeyProperties = klass.getKeyProperties().newWithAll(toProperties);
		if (primaryKeyProperties.notEmpty())
		{
			sqliteCoreImports.add("primaryKey");
			result.add(
				"\t\tprimaryKey(\n" +
					"\t\t\t{\n" +
					"\t\t\t\tcolumns: [" +
					getColumnReferences(primaryKeyProperties) +
					"],\n" +
					"\t\t\t},\n" +
					"\t\t)"
			);
		}

		ImmutableList<DataTypeProperty> fromProperties = klass.getDataTypeProperties().select(DataTypeProperty::isFrom);
		if (fromProperties.notEmpty())
		{
			sqliteCoreImports.add("uniqueIndex");
			String fromColumnNames = fromProperties
				.collect(DataTypeProperty::getName)
				.collect(COLUMN_NAME_CONVERTER::convert)
				.makeString("_");
			String indexName = tableName + "_" + fromColumnNames + "_idx";
			ImmutableList<DataTypeProperty> uniqueProperties = klass.getKeyProperties().newWithAll(fromProperties);
			result.add("\t\tuniqueIndex(\"" + indexName + "\").on(" + getColumnReferences(uniqueProperties) + ")");
		}

		ImmutableList<String> foreignKeyIndexes = getForeignKeyIndexes(
			klass,
			tableName,
			toProperties,
			primaryKeyProperties
		);
		if (foreignKeyIndexes.notEmpty())
		{
			sqliteCoreImports.add("index");
		}
		result.addAllIterable(foreignKeyIndexes);

		return result.toImmutable();
	}

	/**
	 * Mirrors the Liquibase IndexGenerator: one index per foreign key, on the foreign key columns followed by the
	 * {@code to} columns, skipped when it is a prefix of the primary key.
	 */
	private static ImmutableList<String> getForeignKeyIndexes(
		Klass klass,
		String tableName,
		ImmutableList<DataTypeProperty> toProperties,
		ImmutableList<DataTypeProperty> primaryKeyProperties
	)
	{
		MutableList<String> result = Lists.mutable.empty();
		for (Pair<AssociationEnd, MutableOrderedMap<DataTypeProperty, DataTypeProperty>> pair : klass
			.getForeignKeys()
			.keyValuesView())
		{
			ImmutableList<DataTypeProperty> indexProperties = pair
				.getTwo()
				.keysView()
				.asLazy()
				.concatenate(toProperties)
				.toImmutableList();
			if (isPrefixList(indexProperties, primaryKeyProperties))
			{
				continue;
			}
			String indexName = tableName + "_idx_" + COLUMN_NAME_CONVERTER.convert(pair.getOne().getName());
			result.add("\t\tindex(\"" + indexName + "\").on(" + getColumnReferences(indexProperties) + ")");
		}

		if (!result.equals(result.distinct()))
		{
			throw new AssertionError("Duplicate foreign key index detected for " + tableName + ". Indexes: " + result);
		}

		return result.toImmutable();
	}

	private static boolean isPrefixList(ImmutableList<DataTypeProperty> list1, ImmutableList<DataTypeProperty> list2)
	{
		if (list1.size() > list2.size())
		{
			return false;
		}

		return list1.equals(list2.subList(0, list1.size()));
	}

	private static String getColumnReferences(ImmutableList<DataTypeProperty> properties)
	{
		return properties.collect((property) -> "table." + property.getName()).makeString(", ");
	}

	private static String getRelationshipComments(Klass klass, String tableName)
	{
		return klass
			.getForeignKeys()
			.keyValuesView()
			.collect((pair) -> getRelationshipComment(pair.getOne(), pair.getTwo(), tableName))
			.makeString("");
	}

	private static String getRelationshipComment(
		AssociationEnd associationEnd,
		MutableOrderedMap<DataTypeProperty, DataTypeProperty> foreignKeyProperties,
		String tableName
	)
	{
		String columns = foreignKeyProperties
			.keysView()
			.collect(DataTypeProperty::getName)
			.collect(COLUMN_NAME_CONVERTER::convert)
			.makeString(", ");
		String referencedColumns = foreignKeyProperties
			.valuesView()
			.collect(DataTypeProperty::getName)
			.collect(COLUMN_NAME_CONVERTER::convert)
			.makeString(", ");
		String referencedTableName = TABLE_NAME_CONVERTER.convert(associationEnd.getType().getName());
		return (
			"// Relationship " +
			associationEnd.getName() +
			": " +
			tableName +
			" (" +
			columns +
			") -> " +
			referencedTableName +
			" (" +
			referencedColumns +
			")\n"
		);
	}
}
