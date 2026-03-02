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

package cool.klass.generator.liquibase.schema;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
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
			.reject(ForeignKeyGenerator::shouldSuppressForeignKey)
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

	private static boolean shouldSuppressForeignKey(
		Pair<AssociationEnd, MutableOrderedMap<DataTypeProperty, DataTypeProperty>> pair
	) {
		AssociationEnd associationEnd = pair.getOne();
		if (!(associationEnd.getOwningClassifier() instanceof Klass sourceKlass)) {
			return false;
		}
		Klass targetKlass = associationEnd.getType();

		// Check 1: Target transitively owns source — direct ownership cycle.
		// Example: ProjectionProjectionReference has FK to NamedProjection,
		// but NamedProjection transitively owns ProjectionProjectionReference.
		if (transitivelyOwns(targetKlass, sourceKlass, new HashSet<>())) {
			return true;
		}

		// Check 2: Target is in a cascade tree, source is NOT in the same tree.
		// Example: PropertyModifier has FK to DataTypeProperty. DTP is owned by
		// Classifier, but PropertyModifier is unowned. Cascade-deleting Classifier
		// deletes DTP, but PropertyModifier still references it.
		if (isInDifferentCascadeTree(sourceKlass, targetKlass)) {
			return true;
		}

		// Check 3: Source and target are in the SAME cascade tree but different
		// branches. Cascade delete order is non-deterministic across branches,
		// so the FK could block if the target's branch is processed before the
		// source's branch.
		// Example: MemberReferencePathAssociationEndMapping has FK to AssociationEnd.
		// Both are transitively owned by Association, but in different branches:
		//   Association → AssociationEnd (branch 1)
		//   Association → Criteria → OperatorCriteria → ExpressionValue → MRP → MRPAM (branch 2)
		// If branch 1 is processed first, AE deletion is blocked by MRPAM's FK.
		return isInSameTreeDifferentBranch(sourceKlass, targetKlass);
	}

	private static boolean isInDifferentCascadeTree(Klass sourceKlass, Klass targetKlass) {
		Klass targetRoot = findOwnershipRoot(targetKlass);
		if (targetRoot == targetKlass) {
			return false;
		}
		Klass sourceRoot = findOwnershipRoot(sourceKlass);
		return sourceRoot != targetRoot;
	}

	private static boolean isInSameTreeDifferentBranch(Klass sourceKlass, Klass targetKlass) {
		Klass targetRoot = findOwnershipRoot(targetKlass);
		if (targetRoot == targetKlass) {
			return false;
		}
		Klass sourceRoot = findOwnershipRoot(sourceKlass);
		if (sourceRoot != targetRoot) {
			return false;
		}
		return !transitivelyOwns(sourceKlass, targetKlass, new HashSet<>());
	}

	private static Klass findOwnershipRoot(Klass klass) {
		Set<Klass> visited = new HashSet<>();
		Klass current = klass;
		while (visited.add(current)) {
			Klass owner = findOwner(current);
			if (owner == null) {
				return current;
			}
			current = owner;
		}
		return current;
	}

	private static Klass findOwner(Klass klass) {
		for (AssociationEnd ae : klass.getAssociationEnds()) {
			if (ae.getOpposite().isOwned()) {
				return ae.getType();
			}
		}
		return null;
	}

	private static boolean transitivelyOwns(Klass owner, Klass target, Set<Klass> visited) {
		if (target.isSubTypeOf(owner)) {
			return true;
		}
		if (!visited.add(owner)) {
			return false;
		}
		return owner.getAssociationEnds()
			.select(ReferenceProperty::isOwned)
			.anySatisfy(ae -> transitivelyOwns(ae.getType(), target, visited));
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
