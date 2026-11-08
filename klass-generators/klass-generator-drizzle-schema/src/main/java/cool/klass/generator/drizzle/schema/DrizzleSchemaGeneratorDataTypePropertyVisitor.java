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

import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.visitor.DataTypePropertyVisitor;

/**
 * Produces the Drizzle SQLite column builder call for a property, such as {@code text("name")}.
 * Instants are stored as epoch milliseconds in {@code integer} columns. LocalDates are stored as ISO text.
 */
public class DrizzleSchemaGeneratorDataTypePropertyVisitor
	implements DataTypePropertyVisitor
{
	private final String columnName;

	private String columnBuilder;

	public DrizzleSchemaGeneratorDataTypePropertyVisitor(String columnName)
	{
		this.columnName = Objects.requireNonNull(columnName);
	}

	public String getColumnBuilder()
	{
		return Objects.requireNonNull(this.columnBuilder);
	}

	private void simple(String function)
	{
		this.columnBuilder = function + "(\"" + this.columnName + "\")";
	}

	private void withOption(String function, String option)
	{
		this.columnBuilder =
			function +
			"(\n" +
			"\t\t\t\"" +
			this.columnName +
			"\",\n" +
			"\t\t\t{\n" +
			"\t\t\t\t" +
			option +
			",\n" +
			"\t\t\t},\n" +
			"\t\t)";
	}

	@Override
	public void visitEnumerationProperty(EnumerationProperty enumerationProperty)
	{
		String literals = enumerationProperty
			.getType()
			.getEnumerationLiterals()
			.collect(EnumerationLiteral::getPrettyName)
			.collect((prettyName) -> '"' + prettyName + '"')
			.makeString(", ");
		this.withOption("text", "enum: [" + literals + "]");
	}

	@Override
	public void visitString(PrimitiveProperty primitiveProperty)
	{
		this.simple("text");
	}

	@Override
	public void visitInteger(PrimitiveProperty primitiveProperty)
	{
		this.simple("integer");
	}

	@Override
	public void visitLong(PrimitiveProperty primitiveProperty)
	{
		this.simple("integer");
	}

	@Override
	public void visitDouble(PrimitiveProperty primitiveProperty)
	{
		this.simple("real");
	}

	@Override
	public void visitFloat(PrimitiveProperty primitiveProperty)
	{
		this.simple("real");
	}

	@Override
	public void visitBoolean(PrimitiveProperty primitiveProperty)
	{
		this.withOption("integer", "mode: \"boolean\"");
	}

	@Override
	public void visitInstant(PrimitiveProperty primitiveProperty)
	{
		this.simple("integer");
	}

	@Override
	public void visitLocalDate(PrimitiveProperty primitiveProperty)
	{
		this.simple("text");
	}

	@Override
	public void visitTemporalInstant(PrimitiveProperty primitiveProperty)
	{
		this.simple("integer");
	}

	@Override
	public void visitTemporalRange(PrimitiveProperty primitiveProperty)
	{
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".visitTemporalRange() not implemented yet"
		);
	}
}
