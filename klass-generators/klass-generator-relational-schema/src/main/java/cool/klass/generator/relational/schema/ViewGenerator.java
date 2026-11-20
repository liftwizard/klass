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

package cool.klass.generator.relational.schema;

import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.Derivation;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * Generates the view that computes a class's database-derived properties, like {@code numberOfVotes: Long derived = count(this.votes);}.
 * The view has the key columns of the class and one column per derived property, and one row per current row of the class.
 */
public final class ViewGenerator
{
	// The same instant as UtcInfinityTimestamp, which Reladomo stores in the 'to' column of current rows
	private static final String INFINITY = "timestamp '9999-12-01 23:59:00'";

	private ViewGenerator()
	{
		throw new AssertionError("Suppress default constructor for noninstantiability");
	}

	@Nonnull
	public static String getViewName(@Nonnull Klass klass)
	{
		return DdlGenerator.getTableName(klass) + "_DERIVED";
	}

	@Nonnull
	public static Optional<String> getView(@Nonnull Klass klass)
	{
		ImmutableList<DataTypeProperty> derivedProperties = klass.getDatabaseDerivedProperties();
		if (derivedProperties.isEmpty())
		{
			return Optional.empty();
		}

		String viewName = getViewName(klass);
		ImmutableList<String> keyColumns = klass
			.getKeyProperties()
			.collect((keyProperty) -> "    t." + getColumnName(keyProperty));
		ImmutableList<String> derivedColumns = derivedProperties.collect(ViewGenerator::getDerivedColumn);
		String columns = keyColumns.newWithAll(derivedColumns).makeString(",\n");

		// language=SQL
		var format = """
			drop view if exists %1$s;

			create view %1$s as
			select
			%2$s
			from %3$s t%4$s;
			""";
		String sourceCode = format.formatted(
			viewName,
			columns,
			DdlGenerator.getTableName(klass),
			getCurrentRowsCriteria(klass, "t", "\nwhere ")
		);
		return Optional.of(sourceCode);
	}

	private static String getDerivedColumn(@Nonnull DataTypeProperty derivedProperty)
	{
		Derivation derivation = derivedProperty.getDerivation().orElseThrow();
		Klass countedClass = derivation.getAssociationEnd().getType();

		String joinCriteria = derivation
			.getForeignKeys()
			.keyValuesView()
			.collect((pair) -> "c.%s = t.%s".formatted(getColumnName(pair.getOne()), getColumnName(pair.getTwo())))
			.makeString(" and ");

		String count = "(select count(*) from %s c where %s%s)".formatted(
			DdlGenerator.getTableName(countedClass),
			joinCriteria,
			getCurrentRowsCriteria(countedClass, "c", " and ")
		);

		boolean isInteger =
			derivedProperty instanceof PrimitiveProperty primitiveProperty &&
			primitiveProperty.getType() == PrimitiveType.INTEGER;
		String expression = isInteger ? "cast(" + count + " as integer)" : count;
		return "    %s as %s".formatted(expression, getColumnName(derivedProperty));
	}

	private static String getCurrentRowsCriteria(@Nonnull Klass klass, String alias, String prefix)
	{
		ImmutableList<String> criteria = klass
			.getDataTypeProperties()
			.select(DataTypeProperty::isTo)
			.collect((toProperty) -> "%s.%s = %s".formatted(alias, getColumnName(toProperty), INFINITY));
		return criteria.isEmpty() ? "" : prefix + criteria.makeString(" and ");
	}

	private static String getColumnName(@Nonnull DataTypeProperty dataTypeProperty)
	{
		return DdlGenerator.COLUMN_NAME_CONVERTER.convert(dataTypeProperty.getName());
	}
}
