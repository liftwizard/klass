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

package cool.klass.generator.reladomo.lens;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.base.CaseFormat;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.Property;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

/**
 * Generates Reladomo ClassLens implementations for each Klass in the domain model.
 *
 * <p>For each Klass, generates:
 * <ul>
 *     <li>A ClassLens implementation with typed lens fields</li>
 *     <li>Inner classes for each property lens (unified get/set/getProperty)</li>
 *     <li>Type conversion handling for temporal types (Timestamp ↔ Instant, Date ↔ LocalDate)</li>
 *     <li>Inheritance handling via recursive delegation to superclass lens</li>
 * </ul>
 */
public class ReladomoLensGenerator {

	private final DomainModel domainModel;
	private final String applicationName;

	public ReladomoLensGenerator(DomainModel domainModel, String applicationName) {
		this.domainModel = Objects.requireNonNull(domainModel);
		this.applicationName = Objects.requireNonNull(applicationName);
	}

	/**
	 * Pairs an inherited property with its ancestor class and the Reladomo navigation expression
	 * needed to reach that ancestor from the concrete subclass.
	 *
	 * <p>For example, if {@code Klass} extends {@code Classifier}, and {@code Classifier} declares
	 * the {@code classifierModifiers} association end, the navigation would be
	 * {@code "domainObject.getClassifierSuperClass()"} since Reladomo uses table-per-class inheritance
	 * and requires explicit navigation through the superclass relationship.
	 */
	private record InheritedProperty<T>(T property, Klass ancestor, String navigation) {}

	/**
	 * Collects inherited data type properties from the superclass chain, paired with their
	 * navigation expressions. Walks up the class hierarchy recursively.
	 */
	private ImmutableList<InheritedProperty<DataTypeProperty>> getInheritedDataTypeProperties(@Nonnull Klass klass) {
		MutableList<InheritedProperty<DataTypeProperty>> result = Lists.mutable.empty();
		this.collectInheritedDataTypeProperties(klass, klass, "domainObject", result);
		return result.toImmutable();
	}

	private void collectInheritedDataTypeProperties(
		@Nonnull Klass originalKlass,
		@Nonnull Klass currentKlass,
		String currentNavigation,
		MutableList<InheritedProperty<DataTypeProperty>> result
	) {
		Optional<Klass> optionalSuperClass = currentKlass.getSuperClass();
		if (optionalSuperClass.isEmpty()) {
			return;
		}

		Klass superClass = optionalSuperClass.get();
		String navigation = currentNavigation + ".get" + superClass.getName() + "SuperClass()";

		for (DataTypeProperty property : superClass
			.getDeclaredDataTypeProperties()
			.reject(Property::isDerived)
			.reject(this::isTemporalRange)) {
			result.add(new InheritedProperty<>(property, superClass, navigation));
		}

		this.collectInheritedDataTypeProperties(originalKlass, superClass, navigation, result);
	}

	/**
	 * Collects inherited association ends from the superclass chain, paired with their
	 * navigation expressions.
	 */
	private ImmutableList<InheritedProperty<AssociationEnd>> getInheritedAssociationEnds(@Nonnull Klass klass) {
		MutableList<InheritedProperty<AssociationEnd>> result = Lists.mutable.empty();
		this.collectInheritedAssociationEnds(klass, klass, "domainObject", result);
		return result.toImmutable();
	}

	private void collectInheritedAssociationEnds(
		@Nonnull Klass originalKlass,
		@Nonnull Klass currentKlass,
		String currentNavigation,
		MutableList<InheritedProperty<AssociationEnd>> result
	) {
		Optional<Klass> optionalSuperClass = currentKlass.getSuperClass();
		if (optionalSuperClass.isEmpty()) {
			return;
		}

		Klass superClass = optionalSuperClass.get();
		String navigation = currentNavigation + ".get" + superClass.getName() + "SuperClass()";

		for (AssociationEnd associationEnd : superClass.getDeclaredAssociationEnds()) {
			result.add(new InheritedProperty<>(associationEnd, superClass, navigation));
		}

		this.collectInheritedAssociationEnds(originalKlass, superClass, navigation, result);
	}

	public void writeClassLenses(@Nonnull Path path) {
		for (Klass klass : this.domainModel.getClasses()) {
			if (klass.isAbstract()) {
				// Skip abstract classes for now; they don't have concrete Reladomo objects
				// TODO: Consider generating lenses for abstract classes with delegation
				continue;
			}

			String packageName = klass.getPackageName() + ".lens.reladomo";
			String relativePath = packageName.replaceAll("\\.", "/");
			Path parentPath = path.resolve(relativePath);
			createDirectories(parentPath);

			String fileName = "Reladomo" + klass.getName() + "ClassLens.java";
			Path outputPath = parentPath.resolve(fileName);

			String sourceCode = this.getClassLensSourceCode(klass);
			this.printStringToFile(outputPath, sourceCode);
		}

		// Also generate the factory class that wires everything together
		this.writeLensFactory(path);
	}

	private void writeLensFactory(@Nonnull Path path) {
		ImmutableList<Klass> concreteClasses = this.domainModel.getClasses().reject(Klass::isAbstract);
		if (concreteClasses.isEmpty()) {
			return;
		}

		// Use the first class's package as the factory package
		String packageName = concreteClasses.getFirst().getPackageName() + ".lens.reladomo";
		String relativePath = packageName.replaceAll("\\.", "/");
		Path parentPath = path.resolve(relativePath);
		createDirectories(parentPath);

		String fileName = "ReladomoClassLensFactory.java";
		Path outputPath = parentPath.resolve(fileName);

		String sourceCode = this.getFactorySourceCode(concreteClasses, packageName);
		this.printStringToFile(outputPath, sourceCode);
	}

	private static void createDirectories(Path dir) {
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String getClassLensSourceCode(@Nonnull Klass klass) {
		Objects.requireNonNull(klass);

		String packageName = klass.getPackageName() + ".lens.reladomo";
		String className = klass.getName();
		String lensClassName = "Reladomo" + className + "ClassLens";

		StringBuilder sb = new StringBuilder();

		// Package declaration
		sb.append("package ").append(packageName).append(";\n\n");

		// Imports
		sb.append(this.getImports(klass));

		// Class javadoc
		sb.append("/**\n");
		sb.append(" * Auto-generated Reladomo ClassLens for {@link ").append(className).append("}.\n");
		sb.append(" *\n");
		sb.append(" * <p>Generated by {@link ").append(this.getClass().getCanonicalName()).append("}\n");
		sb.append(" */\n");

		// Class declaration
		sb.append("public class ").append(lensClassName).append("\n");
		sb.append("        implements ClassLens<").append(className).append(">\n");
		sb.append("{\n");

		// Fields
		sb.append(this.getFields(klass));

		// Constructor
		sb.append(this.getConstructor(klass, lensClassName));

		// ClassLens methods
		sb.append(this.getClassLensMethods(klass));

		// Inner lens classes
		sb.append(this.getInnerLensClasses(klass));

		sb.append("}\n");

		return sb.toString();
	}

	private String getImports(@Nonnull Klass klass) {
		StringBuilder imports = new StringBuilder();

		ImmutableList<PrimitiveProperty> primitiveProperties = klass
			.getDataTypeProperties()
			.selectInstancesOf(PrimitiveProperty.class)
			.reject(Property::isDerived)
			.reject((p) -> p.getType().isTemporalRange());

		ImmutableList<EnumerationProperty> enumerationProperties = klass
			.getDataTypeProperties()
			.selectInstancesOf(EnumerationProperty.class)
			.reject(Property::isDerived);

		ImmutableList<AssociationEnd> declaredAssociationEnds = klass.getDeclaredAssociationEnds();
		ImmutableList<InheritedProperty<AssociationEnd>> inheritedAssociationEnds = this.getInheritedAssociationEnds(
			klass
		);

		boolean hasInstant = primitiveProperties.anySatisfy(
			(p) -> p.getType() == PrimitiveType.INSTANT || p.getType() == PrimitiveType.TEMPORAL_INSTANT
		);
		boolean hasLocalDate = primitiveProperties.anySatisfy((p) -> p.getType() == PrimitiveType.LOCAL_DATE);
		boolean hasTimestamp = hasInstant; // Reladomo uses Timestamp for Instant/TemporalInstant
		boolean hasDate = hasLocalDate; // Reladomo uses Date for LocalDate
		boolean hasToMany =
			declaredAssociationEnds.anySatisfy((ae) -> ae.getMultiplicity().isToMany())
			|| inheritedAssociationEnds.anySatisfy((ip) -> ip.property().getMultiplicity().isToMany());
		boolean hasAnyAssociationEnd = declaredAssociationEnds.notEmpty() || inheritedAssociationEnds.notEmpty();

		// java.sql imports
		if (hasTimestamp || hasDate) {
			imports.append("import java.sql.Timestamp;\n");
			if (hasDate) {
				imports.append("import java.sql.Date;\n");
			}
			imports.append("\n");
		}

		// java.time imports
		if (hasInstant || hasLocalDate) {
			if (hasInstant) {
				imports.append("import java.time.Instant;\n");
			}
			if (hasLocalDate) {
				imports.append("import java.time.LocalDate;\n");
			}
			imports.append("\n");
		}

		// javax.annotation imports
		imports.append("import javax.annotation.Nonnull;\n");
		imports.append("import javax.annotation.Nullable;\n");
		imports.append("\n");

		// cool.klass.model.lens imports
		imports.append("import cool.klass.model.lens.AssociationLens;\n");
		imports.append("import cool.klass.model.lens.ClassLens;\n");
		imports.append("import cool.klass.model.lens.DataTypeLens;\n");
		imports.append("import cool.klass.model.lens.EnumerationLens;\n");
		imports.append("import cool.klass.model.lens.PrimitiveLens;\n");
		imports.append("import cool.klass.model.lens.PropertyLens;\n");
		imports.append("import cool.klass.model.lens.ReferenceLens;\n");

		// Type-specific lens imports
		for (PrimitiveProperty property : primitiveProperties) {
			String lensType = this.getLensInterfaceImport(property.getType());
			if (lensType != null && !imports.toString().contains(lensType)) {
				imports.append("import cool.klass.model.lens.").append(lensType).append(";\n");
			}
		}

		if (hasAnyAssociationEnd) {
			imports.append("import cool.klass.model.lens.ToOneLens;\n");
			if (hasToMany) {
				imports.append("import cool.klass.model.lens.ToManyLens;\n");
			}
		}
		imports.append("\n");

		// Domain model imports - using fully qualified names to avoid conflicts with domain classes
		// that have the same names (e.g., klass.model.meta.domain.Klass vs cool.klass...api.Klass)
		if (enumerationProperties.notEmpty()) {
			imports.append("import cool.klass.model.meta.domain.api.EnumerationLiteral;\n");
		}
		imports.append("\n");

		// Eclipse Collections imports
		imports.append("import org.eclipse.collections.api.list.ImmutableList;\n");
		imports.append("import org.eclipse.collections.api.map.ImmutableMap;\n");
		imports.append("import org.eclipse.collections.impl.factory.Lists;\n");
		imports.append("import org.eclipse.collections.impl.factory.Maps;\n");
		imports.append("\n");

		// Domain class import
		imports.append("import ").append(klass.getPackageName()).append(".").append(klass.getName()).append(";\n");

		// Superclass type imports for navigation (e.g., import klass.model.meta.domain.Classifier;)
		for (Klass ancestor : klass.getSuperClassChain()) {
			String ancestorImport = "import " + ancestor.getPackageName() + "." + ancestor.getName() + ";\n";
			if (!imports.toString().contains(ancestorImport)) {
				imports.append(ancestorImport);
			}
		}

		// Association end target type imports (avoid duplicates) - declared
		for (AssociationEnd associationEnd : declaredAssociationEnds) {
			Klass targetType = associationEnd.getType();
			String targetImport = "import " + targetType.getPackageName() + "." + targetType.getName() + ";\n";
			if (!imports.toString().contains(targetImport)) {
				imports.append(targetImport);
			}
		}

		// Association end target type imports - inherited
		for (InheritedProperty<AssociationEnd> inherited : inheritedAssociationEnds) {
			Klass targetType = inherited.property().getType();
			String targetImport = "import " + targetType.getPackageName() + "." + targetType.getName() + ";\n";
			if (!imports.toString().contains(targetImport)) {
				imports.append(targetImport);
			}
		}
		imports.append("\n");

		return imports.toString();
	}

	private String getLensInterfaceImport(@Nonnull PrimitiveType type) {
		return switch (type) {
			case INTEGER -> "IntegerLens";
			case LONG -> "LongLens";
			case DOUBLE -> "DoubleLens";
			case FLOAT -> "FloatLens";
			case BOOLEAN -> "BooleanLens";
			case STRING -> "StringLens";
			case INSTANT, TEMPORAL_INSTANT -> "InstantLens";
			case LOCAL_DATE -> "LocalDateLens";
			case TEMPORAL_RANGE -> null; // Should be filtered out
			default -> null;
		};
	}

	private String getFields(@Nonnull Klass klass) {
		StringBuilder sb = new StringBuilder();

		ImmutableList<InheritedProperty<DataTypeProperty>> inheritedDataTypeProperties =
			this.getInheritedDataTypeProperties(klass);
		ImmutableList<InheritedProperty<AssociationEnd>> inheritedAssociationEnds = this.getInheritedAssociationEnds(
			klass
		);

		sb.append("    private final cool.klass.model.meta.domain.api.Klass metaKlass;\n");
		sb.append("\n");

		// Public typed lens fields - declared data type properties
		for (DataTypeProperty property : klass
			.getDeclaredDataTypeProperties()
			.reject(Property::isDerived)
			.reject(this::isTemporalRange)) {
			String lensType = this.getLensFieldType(klass, property);
			String fieldName = property.getName();
			sb.append("    public final ").append(lensType).append(" ").append(fieldName).append(";\n");
		}

		// Public typed lens fields - declared association ends
		for (AssociationEnd associationEnd : klass.getDeclaredAssociationEnds()) {
			String lensType = this.getAssociationLensFieldType(associationEnd, klass);
			String fieldName = associationEnd.getName();
			sb.append("    public final ").append(lensType).append(" ").append(fieldName).append(";\n");
		}

		// Public typed lens fields - inherited data type properties
		for (InheritedProperty<DataTypeProperty> inherited : inheritedDataTypeProperties) {
			String lensType = this.getLensFieldType(klass, inherited.property());
			String fieldName = inherited.property().getName();
			sb.append("    public final ").append(lensType).append(" ").append(fieldName).append(";\n");
		}

		// Public typed lens fields - inherited association ends
		for (InheritedProperty<AssociationEnd> inherited : inheritedAssociationEnds) {
			String lensType = this.getAssociationLensFieldType(inherited.property(), klass);
			String fieldName = inherited.property().getName();
			sb.append("    public final ").append(lensType).append(" ").append(fieldName).append(";\n");
		}

		sb.append("\n");
		sb.append("    private final ImmutableList<PropertyLens<").append(klass.getName()).append(", ?>> allLenses;\n");
		sb
			.append(
				"    private final ImmutableMap<cool.klass.model.meta.domain.api.property.PrimitiveProperty, PrimitiveLens<"
			)
			.append(klass.getName())
			.append(", ?>> primitiveLenses;\n");
		sb
			.append(
				"    private final ImmutableMap<cool.klass.model.meta.domain.api.property.EnumerationProperty, EnumerationLens<"
			)
			.append(klass.getName())
			.append(">> enumerationLenses;\n");
		sb
			.append(
				"    private final ImmutableMap<cool.klass.model.meta.domain.api.property.AssociationEnd, AssociationLens<"
			)
			.append(klass.getName())
			.append(", ?>> associationLenses;\n");
		sb
			.append("    private final ImmutableMap<cool.klass.model.meta.domain.api.property.Property, PropertyLens<")
			.append(klass.getName())
			.append(", ?>> allLensesByProperty;\n");
		sb.append("\n");

		return sb.toString();
	}

	private String getLensFieldType(@Nonnull Klass klass, @Nonnull DataTypeProperty property) {
		if (property instanceof PrimitiveProperty primitiveProperty) {
			return this.getPrimitiveLensType(primitiveProperty.getType());
		} else if (property instanceof EnumerationProperty) {
			return "EnumerationLens<" + klass.getName() + ">";
		}
		throw new IllegalStateException("Unknown property type: " + property.getClass());
	}

	private String getPrimitiveLensType(@Nonnull PrimitiveType type) {
		return switch (type) {
			case INTEGER -> "IntegerLens";
			case LONG -> "LongLens";
			case DOUBLE -> "DoubleLens";
			case FLOAT -> "FloatLens";
			case BOOLEAN -> "BooleanLens";
			case STRING -> "StringLens";
			case INSTANT, TEMPORAL_INSTANT -> "InstantLens";
			case LOCAL_DATE -> "LocalDateLens";
			case TEMPORAL_RANGE -> throw new IllegalStateException(
				"TEMPORAL_RANGE properties should be filtered out before lens generation"
			);
			default -> "PrimitiveLens";
		};
	}

	private String getAssociationLensFieldType(@Nonnull AssociationEnd associationEnd, @Nonnull Klass ownerKlass) {
		String ownerType = ownerKlass.getName();
		String targetType = associationEnd.getType().getName();

		if (associationEnd.getMultiplicity().isToMany()) {
			return "ToManyLens<" + ownerType + ", " + targetType + ">";
		}
		return "ToOneLens<" + ownerType + ", " + targetType + ">";
	}

	private String getConstructor(@Nonnull Klass klass, String lensClassName) {
		StringBuilder sb = new StringBuilder();

		ImmutableList<InheritedProperty<DataTypeProperty>> inheritedDataTypeProperties =
			this.getInheritedDataTypeProperties(klass);
		ImmutableList<InheritedProperty<AssociationEnd>> inheritedAssociationEnds = this.getInheritedAssociationEnds(
			klass
		);

		sb
			.append("    public ")
			.append(lensClassName)
			.append("(@Nonnull cool.klass.model.meta.domain.api.Klass klass)\n");
		sb.append("    {\n");
		sb.append("        this.metaKlass = klass;\n");
		sb.append("\n");

		// Initialize lens fields - declared data type properties
		for (DataTypeProperty property : klass
			.getDeclaredDataTypeProperties()
			.reject(Property::isDerived)
			.reject(this::isTemporalRange)) {
			String fieldName = property.getName();
			String lensClass = this.getLensClassName(klass, property);
			String propertyLookup = this.getPropertyLookup(property);
			sb
				.append("        this.")
				.append(fieldName)
				.append(" = new ")
				.append(lensClass)
				.append("(")
				.append(propertyLookup)
				.append(");\n");
		}

		// Initialize lens fields - declared association ends
		for (AssociationEnd associationEnd : klass.getDeclaredAssociationEnds()) {
			String fieldName = associationEnd.getName();
			String lensClass = this.getAssociationLensClassName(klass, associationEnd);
			sb
				.append("        this.")
				.append(fieldName)
				.append(" = new ")
				.append(lensClass)
				.append("(klass.getAssociationEndByName(\"")
				.append(fieldName)
				.append("\"));\n");
		}

		// Initialize lens fields - inherited data type properties
		for (InheritedProperty<DataTypeProperty> inherited : inheritedDataTypeProperties) {
			String fieldName = inherited.property().getName();
			String lensClass = this.getLensClassName(klass, inherited.property());
			String propertyLookup = this.getPropertyLookup(inherited.property());
			sb
				.append("        this.")
				.append(fieldName)
				.append(" = new ")
				.append(lensClass)
				.append("(")
				.append(propertyLookup)
				.append(");\n");
		}

		// Initialize lens fields - inherited association ends
		for (InheritedProperty<AssociationEnd> inherited : inheritedAssociationEnds) {
			String fieldName = inherited.property().getName();
			String lensClass = this.getAssociationLensClassName(klass, inherited.property());
			sb
				.append("        this.")
				.append(fieldName)
				.append(" = new ")
				.append(lensClass)
				.append("(klass.getAssociationEndByName(\"")
				.append(fieldName)
				.append("\"));\n");
		}

		sb.append("\n");

		// Initialize allLenses list
		sb.append("        this.allLenses = Lists.immutable.with(\n");
		MutableList<String> lensNames = Lists.mutable.empty();
		for (DataTypeProperty property : klass
			.getDeclaredDataTypeProperties()
			.reject(Property::isDerived)
			.reject(this::isTemporalRange)) {
			lensNames.add("this." + property.getName());
		}
		for (AssociationEnd associationEnd : klass.getDeclaredAssociationEnds()) {
			lensNames.add("this." + associationEnd.getName());
		}
		for (InheritedProperty<DataTypeProperty> inherited : inheritedDataTypeProperties) {
			lensNames.add("this." + inherited.property().getName());
		}
		for (InheritedProperty<AssociationEnd> inherited : inheritedAssociationEnds) {
			lensNames.add("this." + inherited.property().getName());
		}
		sb.append("                ").append(lensNames.makeString(", ")).append(");\n");

		// Initialize primitiveLenses map
		sb.append("\n");
		this.appendTypedMapInit(
			sb,
			"primitiveLenses",
			"cool.klass.model.meta.domain.api.property.PrimitiveProperty",
			"PrimitiveLens<" + klass.getName() + ", ?>"
		);
		ImmutableList<PrimitiveProperty> declaredPrimitives = klass
			.getDeclaredDataTypeProperties()
			.selectInstancesOf(PrimitiveProperty.class)
			.reject(Property::isDerived)
			.reject((p) -> p.getType().isTemporalRange());
		for (PrimitiveProperty property : declaredPrimitives) {
			this.appendMapEntry(sb, property.getName());
		}
		for (InheritedProperty<DataTypeProperty> inherited : inheritedDataTypeProperties) {
			if (inherited.property() instanceof PrimitiveProperty) {
				this.appendMapEntry(sb, inherited.property().getName());
			}
		}
		this.finishMapInit(sb);
		sb.append("\n");

		// Initialize enumerationLenses map
		this.appendTypedMapInit(
			sb,
			"enumerationLenses",
			"cool.klass.model.meta.domain.api.property.EnumerationProperty",
			"EnumerationLens<" + klass.getName() + ">"
		);
		ImmutableList<EnumerationProperty> declaredEnumerations = klass
			.getDeclaredDataTypeProperties()
			.selectInstancesOf(EnumerationProperty.class)
			.reject(Property::isDerived);
		for (EnumerationProperty property : declaredEnumerations) {
			this.appendMapEntry(sb, property.getName());
		}
		for (InheritedProperty<DataTypeProperty> inherited : inheritedDataTypeProperties) {
			if (inherited.property() instanceof EnumerationProperty) {
				this.appendMapEntry(sb, inherited.property().getName());
			}
		}
		this.finishMapInit(sb);
		sb.append("\n");

		// Initialize associationLenses map
		this.appendTypedMapInit(
			sb,
			"associationLenses",
			"cool.klass.model.meta.domain.api.property.AssociationEnd",
			"AssociationLens<" + klass.getName() + ", ?>"
		);
		for (AssociationEnd associationEnd : klass.getDeclaredAssociationEnds()) {
			this.appendMapEntry(sb, associationEnd.getName());
		}
		for (InheritedProperty<AssociationEnd> inherited : inheritedAssociationEnds) {
			this.appendMapEntry(sb, inherited.property().getName());
		}
		this.finishMapInit(sb);
		sb.append("\n");

		// Initialize allLensesByProperty map (combined)
		sb
			.append(
				"        this.allLensesByProperty = Maps.immutable.<cool.klass.model.meta.domain.api.property.Property, PropertyLens<"
			)
			.append(klass.getName())
			.append(", ?>>empty()\n");
		for (DataTypeProperty property : klass
			.getDeclaredDataTypeProperties()
			.reject(Property::isDerived)
			.reject(this::isTemporalRange)) {
			this.appendMapEntry(sb, property.getName());
		}
		for (AssociationEnd associationEnd : klass.getDeclaredAssociationEnds()) {
			this.appendMapEntry(sb, associationEnd.getName());
		}
		for (InheritedProperty<DataTypeProperty> inherited : inheritedDataTypeProperties) {
			this.appendMapEntry(sb, inherited.property().getName());
		}
		for (InheritedProperty<AssociationEnd> inherited : inheritedAssociationEnds) {
			this.appendMapEntry(sb, inherited.property().getName());
		}
		this.finishMapInit(sb);

		sb.append("    }\n");
		sb.append("\n");

		return sb.toString();
	}

	private void appendTypedMapInit(StringBuilder sb, String fieldName, String keyType, String valueType) {
		sb
			.append("        this.")
			.append(fieldName)
			.append(" = Maps.immutable.<")
			.append(keyType)
			.append(", ")
			.append(valueType)
			.append(">empty()\n");
	}

	private void appendMapEntry(StringBuilder sb, String propertyName) {
		sb
			.append("                .newWithKeyValue(this.")
			.append(propertyName)
			.append(".getProperty(), this.")
			.append(propertyName)
			.append(")\n");
	}

	private void finishMapInit(StringBuilder sb) {
		// Remove trailing newline and add semicolon + newline
		sb.setLength(sb.length() - 1);
		sb.append(";\n");
	}

	private String getPropertyLookup(@Nonnull DataTypeProperty property) {
		if (property instanceof PrimitiveProperty) {
			return ("klass.findPrimitivePropertyByName(\"" + property.getName() + "\").orElseThrow()");
		} else if (property instanceof EnumerationProperty) {
			return ("klass.findEnumerationPropertyByName(\"" + property.getName() + "\").orElseThrow()");
		}
		throw new IllegalStateException("Unknown property type: " + property.getClass());
	}

	private String getLensClassName(@Nonnull Klass klass, @Nonnull DataTypeProperty property) {
		String className = klass.getName();
		String propName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, property.getName());
		return className + propName + "Lens";
	}

	private String getAssociationLensClassName(@Nonnull Klass klass, @Nonnull AssociationEnd associationEnd) {
		String className = klass.getName();
		String propName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, associationEnd.getName());
		return className + propName + "Lens";
	}

	private String getClassLensMethods(@Nonnull Klass klass) {
		StringBuilder sb = new StringBuilder();
		String className = klass.getName();

		// getKlass()
		sb.append("    @Override\n");
		sb.append("    @Nonnull\n");
		sb.append("    public cool.klass.model.meta.domain.api.Klass getKlass()\n");
		sb.append("    {\n");
		sb.append("        return this.metaKlass;\n");
		sb.append("    }\n");
		sb.append("\n");

		// getLenses()
		sb.append("    @Override\n");
		sb.append("    @Nonnull\n");
		sb.append("    public ImmutableList<PropertyLens<").append(className).append(", ?>> getLenses()\n");
		sb.append("    {\n");
		sb.append("        return this.allLenses;\n");
		sb.append("    }\n");
		sb.append("\n");

		// getLensByProperty(Property)
		sb.append("    @Override\n");
		sb.append("    @Nonnull\n");
		sb
			.append("    public PropertyLens<")
			.append(className)
			.append(", ?> getLensByProperty(@Nonnull cool.klass.model.meta.domain.api.property.Property property)\n");
		sb.append("    {\n");
		sb
			.append("        PropertyLens<")
			.append(className)
			.append(", ?> lens = this.allLensesByProperty.get(property);\n");
		sb.append("        if (lens == null)\n");
		sb.append("        {\n");
		sb.append(
			"            throw new IllegalArgumentException(\"No lens found for property: \" + property.getName());\n"
		);
		sb.append("        }\n");
		sb.append("        return lens;\n");
		sb.append("    }\n");
		sb.append("\n");

		// Type-specific overloads - cast-based for supertypes
		this.addCastOverload(
			sb,
			className,
			"DataTypeLens",
			"cool.klass.model.meta.domain.api.property.DataTypeProperty",
			"allLensesByProperty"
		);

		// Type-specific overloads - direct typed map lookups (no casts needed)
		this.addDirectOverload(
			sb,
			className,
			"PrimitiveLens<" + className + ", ?>",
			"cool.klass.model.meta.domain.api.property.PrimitiveProperty",
			"primitiveLenses"
		);
		this.addDirectOverload(
			sb,
			className,
			"EnumerationLens<" + className + ">",
			"cool.klass.model.meta.domain.api.property.EnumerationProperty",
			"enumerationLenses"
		);
		this.addCastOverload(
			sb,
			className,
			"ReferenceLens",
			"cool.klass.model.meta.domain.api.property.ReferenceProperty",
			"allLensesByProperty"
		);
		this.addDirectOverload(
			sb,
			className,
			"AssociationLens<" + className + ", ?>",
			"cool.klass.model.meta.domain.api.property.AssociationEnd",
			"associationLenses"
		);

		// getLensesByProperty()
		sb.append("    @Override\n");
		sb.append("    @Nonnull\n");
		sb
			.append("    public ImmutableMap<cool.klass.model.meta.domain.api.property.Property, PropertyLens<")
			.append(className)
			.append(", ?>> getLensesByProperty()\n");
		sb.append("    {\n");
		sb.append("        return this.allLensesByProperty;\n");
		sb.append("    }\n");
		sb.append("\n");

		return sb.toString();
	}

	private void addCastOverload(
		StringBuilder sb,
		String className,
		String returnLensType,
		String paramPropertyType,
		String mapName
	) {
		sb.append("    @Override\n");
		sb.append("    @Nonnull\n");
		sb
			.append("    public ")
			.append(returnLensType)
			.append("<")
			.append(className)
			.append(", ?> getLensByProperty(@Nonnull ")
			.append(paramPropertyType)
			.append(" property)\n");
		sb.append("    {\n");
		sb
			.append("        PropertyLens<")
			.append(className)
			.append(", ?> lens = this.")
			.append(mapName)
			.append(".get(property);\n");
		sb.append("        if (lens == null)\n");
		sb.append("        {\n");
		sb.append(
			"            throw new IllegalArgumentException(\"No lens found for property: \" + property.getName());\n"
		);
		sb.append("        }\n");
		sb.append("        return (").append(returnLensType).append("<").append(className).append(", ?>) lens;\n");
		sb.append("    }\n");
		sb.append("\n");
	}

	private void addDirectOverload(
		StringBuilder sb,
		String className,
		String returnType,
		String paramPropertyType,
		String mapName
	) {
		sb.append("    @Override\n");
		sb.append("    @Nonnull\n");
		sb
			.append("    public ")
			.append(returnType)
			.append(" getLensByProperty(@Nonnull ")
			.append(paramPropertyType)
			.append(" property)\n");
		sb.append("    {\n");
		sb.append("        ").append(returnType).append(" lens = this.").append(mapName).append(".get(property);\n");
		sb.append("        if (lens == null)\n");
		sb.append("        {\n");
		sb.append(
			"            throw new IllegalArgumentException(\"No lens found for property: \" + property.getName());\n"
		);
		sb.append("        }\n");
		sb.append("        return lens;\n");
		sb.append("    }\n");
		sb.append("\n");
	}

	private String getInnerLensClasses(@Nonnull Klass klass) {
		StringBuilder sb = new StringBuilder();

		// Inner classes for declared data type properties
		for (DataTypeProperty property : klass
			.getDeclaredDataTypeProperties()
			.reject(Property::isDerived)
			.reject(this::isTemporalRange)) {
			if (property instanceof PrimitiveProperty primitiveProperty) {
				sb.append(this.getPrimitiveLensClass(klass, primitiveProperty));
			} else if (property instanceof EnumerationProperty enumerationProperty) {
				sb.append(this.getEnumerationLensClass(klass, enumerationProperty));
			}
		}

		// Inner classes for declared association ends
		for (AssociationEnd associationEnd : klass.getDeclaredAssociationEnds()) {
			sb.append(this.getAssociationLensClass(klass, associationEnd));
		}

		// Inner classes for inherited data type properties (with navigation)
		for (InheritedProperty<DataTypeProperty> inherited : this.getInheritedDataTypeProperties(klass)) {
			if (inherited.property() instanceof PrimitiveProperty primitiveProperty) {
				sb.append(
					this.getInheritedPrimitiveLensClass(
						klass,
						primitiveProperty,
						inherited.ancestor(),
						inherited.navigation()
					)
				);
			} else if (inherited.property() instanceof EnumerationProperty enumerationProperty) {
				sb.append(
					this.getInheritedEnumerationLensClass(
						klass,
						enumerationProperty,
						inherited.ancestor(),
						inherited.navigation()
					)
				);
			}
		}

		// Inner classes for inherited association ends (with navigation)
		for (InheritedProperty<AssociationEnd> inherited : this.getInheritedAssociationEnds(klass)) {
			sb.append(
				this.getInheritedAssociationLensClass(
					klass,
					inherited.property(),
					inherited.ancestor(),
					inherited.navigation()
				)
			);
		}

		return sb.toString();
	}

	private String getPrimitiveLensClass(@Nonnull Klass klass, @Nonnull PrimitiveProperty property) {
		StringBuilder sb = new StringBuilder();

		String className = klass.getName();
		String propName = property.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName);
		String lensClass = className + propNameUpper + "Lens";
		String interfaceType = this.getPrimitiveLensType(property.getType()) + "<" + className + ">";
		String javaType = this.getJavaType(property.getType());
		boolean isBoolean = property.getType() == PrimitiveType.BOOLEAN;
		String getterName = (isBoolean ? "is" : "get") + propNameUpper;
		String setterName = "set" + propNameUpper;
		boolean needsConversion = this.needsTypeConversion(property.getType());

		sb.append("    private static class ").append(lensClass).append("\n");
		sb.append("            implements ").append(interfaceType).append("\n");
		sb.append("    {\n");
		sb.append("        private final cool.klass.model.meta.domain.api.property.PrimitiveProperty property;\n");
		sb.append("\n");
		sb
			.append("        ")
			.append(lensClass)
			.append("(cool.klass.model.meta.domain.api.property.PrimitiveProperty property)\n");
		sb.append("        {\n");
		sb.append("            this.property = property;\n");
		sb.append("        }\n");
		sb.append("\n");

		// get() method
		sb.append("        @Override\n");
		sb.append("        @Nullable\n");
		sb
			.append("        public ")
			.append(javaType)
			.append(" get(@Nonnull ")
			.append(className)
			.append(" domainObject)\n");
		sb.append("        {\n");
		if (needsConversion) {
			sb.append(this.getConversionGetterBody(property, getterName));
		} else {
			sb.append("            return domainObject.").append(getterName).append("();\n");
		}
		sb.append("        }\n");
		sb.append("\n");

		// set() method
		sb.append("        @Override\n");
		sb
			.append("        public void set(@Nonnull ")
			.append(className)
			.append(" domainObject, @Nullable ")
			.append(javaType)
			.append(" value)\n");
		sb.append("        {\n");
		if (needsConversion) {
			sb.append(this.getConversionSetterBody(property, setterName));
		} else {
			sb.append("            domainObject.").append(setterName).append("(value);\n");
		}
		sb.append("        }\n");

		// Unboxed methods for numeric types
		if (this.hasUnboxedMethods(property.getType())) {
			sb.append(this.getUnboxedMethods(klass, property));
		}

		sb.append("\n");
		sb.append("        @Override\n");
		sb.append("        @Nonnull\n");
		sb.append("        public cool.klass.model.meta.domain.api.property.PrimitiveProperty getProperty()\n");
		sb.append("        {\n");
		sb.append("            return this.property;\n");
		sb.append("        }\n");
		sb.append("    }\n");
		sb.append("\n");

		return sb.toString();
	}

	private String getJavaType(@Nonnull PrimitiveType type) {
		return switch (type) {
			case INTEGER -> "Integer";
			case LONG -> "Long";
			case DOUBLE -> "Double";
			case FLOAT -> "Float";
			case BOOLEAN -> "Boolean";
			case STRING -> "String";
			case INSTANT, TEMPORAL_INSTANT -> "Instant";
			case LOCAL_DATE -> "LocalDate";
			case TEMPORAL_RANGE -> throw new IllegalStateException(
				"TEMPORAL_RANGE properties should be filtered out before lens generation"
			);
			default -> "Object";
		};
	}

	private boolean isTemporalRange(@Nonnull DataTypeProperty property) {
		if (property instanceof PrimitiveProperty primitiveProperty) {
			return primitiveProperty.getType().isTemporalRange();
		}
		return false;
	}

	private boolean needsTypeConversion(@Nonnull PrimitiveType type) {
		return (
			type == PrimitiveType.INSTANT || type == PrimitiveType.TEMPORAL_INSTANT || type == PrimitiveType.LOCAL_DATE
		);
	}

	private String getConversionGetterBody(@Nonnull PrimitiveProperty property, String getterName) {
		PrimitiveType type = property.getType();
		if (type == PrimitiveType.INSTANT || type == PrimitiveType.TEMPORAL_INSTANT) {
			return (
				"            Timestamp ts = domainObject."
				+ getterName
				+ "();\n"
				+ "            return ts == null ? null : ts.toInstant();\n"
			);
		} else if (type == PrimitiveType.LOCAL_DATE) {
			// Reladomo declares return type as java.util.Date but stores java.sql.Date
			return (
				"            java.util.Date date = domainObject."
				+ getterName
				+ "();\n"
				+ "            return date == null ? null : ((java.sql.Date) date).toLocalDate();\n"
			);
		}
		throw new IllegalStateException("Unexpected type needing conversion: " + type);
	}

	private String getConversionSetterBody(@Nonnull PrimitiveProperty property, String setterName) {
		PrimitiveType type = property.getType();
		if (type == PrimitiveType.INSTANT || type == PrimitiveType.TEMPORAL_INSTANT) {
			return "            domainObject." + setterName + "(value == null ? null : Timestamp.from(value));\n";
		} else if (type == PrimitiveType.LOCAL_DATE) {
			return "            domainObject." + setterName + "(value == null ? null : Date.valueOf(value));\n";
		}
		throw new IllegalStateException("Unexpected type needing conversion: " + type);
	}

	private boolean hasUnboxedMethods(@Nonnull PrimitiveType type) {
		return switch (type) {
			case INTEGER, LONG, DOUBLE, FLOAT, BOOLEAN -> true;
			default -> false;
		};
	}

	private String getUnboxedMethods(@Nonnull Klass klass, @Nonnull PrimitiveProperty property) {
		StringBuilder sb = new StringBuilder();

		String className = klass.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, property.getName());
		boolean isBoolean = property.getType() == PrimitiveType.BOOLEAN;
		String getterName = (isBoolean ? "is" : "get") + propNameUpper;
		String setterName = "set" + propNameUpper;
		String primitiveType = this.getPrimitiveTypeName(property.getType());
		String methodSuffix = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, primitiveType);

		sb.append("\n");
		sb.append("        @Override\n");
		sb
			.append("        public ")
			.append(primitiveType)
			.append(" get")
			.append(methodSuffix)
			.append("(@Nonnull ")
			.append(className)
			.append(" domainObject)\n");
		sb.append("        {\n");
		sb.append("            return domainObject.").append(getterName).append("();\n");
		sb.append("        }\n");
		sb.append("\n");
		sb.append("        @Override\n");
		sb
			.append("        public void set")
			.append(methodSuffix)
			.append("(@Nonnull ")
			.append(className)
			.append(" domainObject, ")
			.append(primitiveType)
			.append(" value)\n");
		sb.append("        {\n");
		sb.append("            domainObject.").append(setterName).append("(value);\n");
		sb.append("        }\n");

		return sb.toString();
	}

	private String getPrimitiveTypeName(@Nonnull PrimitiveType type) {
		return switch (type) {
			case INTEGER -> "int";
			case LONG -> "long";
			case DOUBLE -> "double";
			case FLOAT -> "float";
			case BOOLEAN -> "boolean";
			default -> throw new IllegalStateException("Not a primitive type: " + type);
		};
	}

	private String getEnumerationLensClass(@Nonnull Klass klass, @Nonnull EnumerationProperty property) {
		StringBuilder sb = new StringBuilder();

		String className = klass.getName();
		String propName = property.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName);
		String lensClass = className + propNameUpper + "Lens";
		String enumTypeName = property.getType().getName();

		sb.append("    private static class ").append(lensClass).append("\n");
		sb.append("            implements EnumerationLens<").append(className).append(">\n");
		sb.append("    {\n");
		sb.append("        private final cool.klass.model.meta.domain.api.property.EnumerationProperty property;\n");
		sb.append("\n");
		sb
			.append("        ")
			.append(lensClass)
			.append("(cool.klass.model.meta.domain.api.property.EnumerationProperty property)\n");
		sb.append("        {\n");
		sb.append("            this.property = property;\n");
		sb.append("        }\n");
		sb.append("\n");

		// get() method - returns EnumerationLiteral
		sb.append("        @Override\n");
		sb.append("        @Nullable\n");
		sb.append("        public EnumerationLiteral get(@Nonnull ").append(className).append(" domainObject)\n");
		sb.append("        {\n");
		sb.append("            String prettyName = domainObject.get").append(propNameUpper).append("();\n");
		sb.append("            if (prettyName == null)\n");
		sb.append("            {\n");
		sb.append("                return null;\n");
		sb.append("            }\n");
		sb.append("            return this.property.getType().getEnumerationLiterals()\n");
		sb.append("                    .detect(el -> el.getPrettyName().equals(prettyName));\n");
		sb.append("        }\n");
		sb.append("\n");

		// set() method - takes EnumerationLiteral
		sb.append("        @Override\n");
		sb
			.append("        public void set(@Nonnull ")
			.append(className)
			.append(" domainObject, @Nullable EnumerationLiteral value)\n");
		sb.append("        {\n");
		sb
			.append("            domainObject.set")
			.append(propNameUpper)
			.append("(value == null ? null : value.getPrettyName());\n");
		sb.append("        }\n");
		sb.append("\n");

		sb.append("        @Override\n");
		sb.append("        @Nonnull\n");
		sb.append("        public cool.klass.model.meta.domain.api.property.EnumerationProperty getProperty()\n");
		sb.append("        {\n");
		sb.append("            return this.property;\n");
		sb.append("        }\n");
		sb.append("    }\n");
		sb.append("\n");

		return sb.toString();
	}

	private String getAssociationLensClass(@Nonnull Klass klass, @Nonnull AssociationEnd associationEnd) {
		StringBuilder sb = new StringBuilder();

		String className = klass.getName();
		String propName = associationEnd.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName);
		String lensClass = className + propNameUpper + "Lens";
		String targetType = associationEnd.getType().getName();
		boolean isToMany = associationEnd.getMultiplicity().isToMany();

		String interfaceType = isToMany
			? "ToManyLens<" + className + ", " + targetType + ">"
			: "ToOneLens<" + className + ", " + targetType + ">";

		String returnType = isToMany ? "ImmutableList<" + targetType + ">" : targetType;

		sb.append("    private static class ").append(lensClass).append("\n");
		sb.append("            implements ").append(interfaceType).append("\n");
		sb.append("    {\n");
		sb.append("        private final cool.klass.model.meta.domain.api.property.AssociationEnd property;\n");
		sb.append("\n");
		sb
			.append("        ")
			.append(lensClass)
			.append("(cool.klass.model.meta.domain.api.property.AssociationEnd property)\n");
		sb.append("        {\n");
		sb.append("            this.property = property;\n");
		sb.append("        }\n");
		sb.append("\n");

		// get() method
		sb.append("        @Override\n");
		sb.append("        @Nullable\n");
		sb
			.append("        public ")
			.append(returnType)
			.append(" get(@Nonnull ")
			.append(className)
			.append(" domainObject)\n");
		sb.append("        {\n");
		if (isToMany) {
			sb
				.append("            return Lists.immutable.withAll(domainObject.get")
				.append(propNameUpper)
				.append("());\n");
		} else {
			sb.append("            return domainObject.get").append(propNameUpper).append("();\n");
		}
		sb.append("        }\n");
		sb.append("\n");

		// set() method
		sb.append("        @Override\n");
		sb
			.append("        public void set(@Nonnull ")
			.append(className)
			.append(" domainObject, @Nullable ")
			.append(returnType)
			.append(" value)\n");
		sb.append("        {\n");
		if (isToMany) {
			sb.append("            if (value == null)\n");
			sb.append("            {\n");
			sb.append("                domainObject.get").append(propNameUpper).append("().clear();\n");
			sb.append("            }\n");
			sb.append("            else\n");
			sb.append("            {\n");
			sb
				.append("                domainObject.get")
				.append(propNameUpper)
				.append("().merge(value.castToList());\n");
			sb.append("            }\n");
		} else {
			sb.append("            domainObject.set").append(propNameUpper).append("(value);\n");
		}
		sb.append("        }\n");
		sb.append("\n");

		sb.append("        @Override\n");
		sb.append("        @Nonnull\n");
		sb.append("        public cool.klass.model.meta.domain.api.property.AssociationEnd getProperty()\n");
		sb.append("        {\n");
		sb.append("            return this.property;\n");
		sb.append("        }\n");
		sb.append("    }\n");
		sb.append("\n");

		return sb.toString();
	}

	private String getInheritedPrimitiveLensClass(
		@Nonnull Klass klass,
		@Nonnull PrimitiveProperty property,
		@Nonnull Klass ancestor,
		@Nonnull String navigation
	) {
		StringBuilder sb = new StringBuilder();

		String className = klass.getName();
		String ancestorName = ancestor.getName();
		String propName = property.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName);
		String lensClass = className + propNameUpper + "Lens";
		String interfaceType = this.getPrimitiveLensType(property.getType()) + "<" + className + ">";
		String javaType = this.getJavaType(property.getType());
		boolean isBoolean = property.getType() == PrimitiveType.BOOLEAN;
		String getterName = (isBoolean ? "is" : "get") + propNameUpper;
		String setterName = "set" + propNameUpper;
		boolean needsConversion = this.needsTypeConversion(property.getType());

		sb.append("    // Inherited from ").append(ancestorName).append("\n");
		sb.append("    private static class ").append(lensClass).append("\n");
		sb.append("            implements ").append(interfaceType).append("\n");
		sb.append("    {\n");
		sb.append("        private final cool.klass.model.meta.domain.api.property.PrimitiveProperty property;\n");
		sb.append("\n");
		sb
			.append("        ")
			.append(lensClass)
			.append("(cool.klass.model.meta.domain.api.property.PrimitiveProperty property)\n");
		sb.append("        {\n");
		sb.append("            this.property = property;\n");
		sb.append("        }\n");
		sb.append("\n");

		// get() method - navigates through superclass
		sb.append("        @Override\n");
		sb.append("        @Nullable\n");
		sb
			.append("        public ")
			.append(javaType)
			.append(" get(@Nonnull ")
			.append(className)
			.append(" domainObject)\n");
		sb.append("        {\n");
		sb.append("            ").append(ancestorName).append(" ancestor = ").append(navigation).append(";\n");
		sb.append("            if (ancestor == null)\n");
		sb.append("            {\n");
		sb.append("                return null;\n");
		sb.append("            }\n");
		if (needsConversion) {
			sb.append(this.getInheritedConversionGetterBody(property, getterName));
		} else {
			sb.append("            return ancestor.").append(getterName).append("();\n");
		}
		sb.append("        }\n");
		sb.append("\n");

		// set() method - navigates through superclass
		sb.append("        @Override\n");
		sb
			.append("        public void set(@Nonnull ")
			.append(className)
			.append(" domainObject, @Nullable ")
			.append(javaType)
			.append(" value)\n");
		sb.append("        {\n");
		sb.append("            ").append(ancestorName).append(" ancestor = ").append(navigation).append(";\n");
		sb.append("            if (ancestor == null)\n");
		sb.append("            {\n");
		sb.append("                return;\n");
		sb.append("            }\n");
		if (needsConversion) {
			sb.append(this.getInheritedConversionSetterBody(property, setterName));
		} else {
			sb.append("            ancestor.").append(setterName).append("(value);\n");
		}
		sb.append("        }\n");

		// Unboxed methods for numeric types
		if (this.hasUnboxedMethods(property.getType())) {
			sb.append(this.getInheritedUnboxedMethods(klass, property, ancestor, navigation));
		}

		sb.append("\n");
		sb.append("        @Override\n");
		sb.append("        @Nonnull\n");
		sb.append("        public cool.klass.model.meta.domain.api.property.PrimitiveProperty getProperty()\n");
		sb.append("        {\n");
		sb.append("            return this.property;\n");
		sb.append("        }\n");
		sb.append("    }\n");
		sb.append("\n");

		return sb.toString();
	}

	private String getInheritedConversionGetterBody(@Nonnull PrimitiveProperty property, String getterName) {
		PrimitiveType type = property.getType();
		if (type == PrimitiveType.INSTANT || type == PrimitiveType.TEMPORAL_INSTANT) {
			return (
				"            Timestamp ts = ancestor."
				+ getterName
				+ "();\n"
				+ "            return ts == null ? null : ts.toInstant();\n"
			);
		} else if (type == PrimitiveType.LOCAL_DATE) {
			return (
				"            java.util.Date date = ancestor."
				+ getterName
				+ "();\n"
				+ "            return date == null ? null : ((java.sql.Date) date).toLocalDate();\n"
			);
		}
		throw new IllegalStateException("Unexpected type needing conversion: " + type);
	}

	private String getInheritedConversionSetterBody(@Nonnull PrimitiveProperty property, String setterName) {
		PrimitiveType type = property.getType();
		if (type == PrimitiveType.INSTANT || type == PrimitiveType.TEMPORAL_INSTANT) {
			return "            ancestor." + setterName + "(value == null ? null : Timestamp.from(value));\n";
		} else if (type == PrimitiveType.LOCAL_DATE) {
			return "            ancestor." + setterName + "(value == null ? null : Date.valueOf(value));\n";
		}
		throw new IllegalStateException("Unexpected type needing conversion: " + type);
	}

	private String getInheritedUnboxedMethods(
		@Nonnull Klass klass,
		@Nonnull PrimitiveProperty property,
		@Nonnull Klass ancestor,
		@Nonnull String navigation
	) {
		StringBuilder sb = new StringBuilder();

		String className = klass.getName();
		String ancestorName = ancestor.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, property.getName());
		boolean isBoolean = property.getType() == PrimitiveType.BOOLEAN;
		String getterName = (isBoolean ? "is" : "get") + propNameUpper;
		String setterName = "set" + propNameUpper;
		String primitiveType = this.getPrimitiveTypeName(property.getType());
		String methodSuffix = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, primitiveType);

		sb.append("\n");
		sb.append("        @Override\n");
		sb
			.append("        public ")
			.append(primitiveType)
			.append(" get")
			.append(methodSuffix)
			.append("(@Nonnull ")
			.append(className)
			.append(" domainObject)\n");
		sb.append("        {\n");
		sb.append("            ").append(ancestorName).append(" ancestor = ").append(navigation).append(";\n");
		sb.append("            return ancestor.").append(getterName).append("();\n");
		sb.append("        }\n");
		sb.append("\n");
		sb.append("        @Override\n");
		sb
			.append("        public void set")
			.append(methodSuffix)
			.append("(@Nonnull ")
			.append(className)
			.append(" domainObject, ")
			.append(primitiveType)
			.append(" value)\n");
		sb.append("        {\n");
		sb.append("            ").append(ancestorName).append(" ancestor = ").append(navigation).append(";\n");
		sb.append("            ancestor.").append(setterName).append("(value);\n");
		sb.append("        }\n");

		return sb.toString();
	}

	private String getInheritedEnumerationLensClass(
		@Nonnull Klass klass,
		@Nonnull EnumerationProperty property,
		@Nonnull Klass ancestor,
		@Nonnull String navigation
	) {
		StringBuilder sb = new StringBuilder();

		String className = klass.getName();
		String ancestorName = ancestor.getName();
		String propName = property.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName);
		String lensClass = className + propNameUpper + "Lens";

		sb.append("    // Inherited from ").append(ancestorName).append("\n");
		sb.append("    private static class ").append(lensClass).append("\n");
		sb.append("            implements EnumerationLens<").append(className).append(">\n");
		sb.append("    {\n");
		sb.append("        private final cool.klass.model.meta.domain.api.property.EnumerationProperty property;\n");
		sb.append("\n");
		sb
			.append("        ")
			.append(lensClass)
			.append("(cool.klass.model.meta.domain.api.property.EnumerationProperty property)\n");
		sb.append("        {\n");
		sb.append("            this.property = property;\n");
		sb.append("        }\n");
		sb.append("\n");

		// get() method - navigates through superclass
		sb.append("        @Override\n");
		sb.append("        @Nullable\n");
		sb.append("        public EnumerationLiteral get(@Nonnull ").append(className).append(" domainObject)\n");
		sb.append("        {\n");
		sb.append("            ").append(ancestorName).append(" ancestor = ").append(navigation).append(";\n");
		sb.append("            if (ancestor == null)\n");
		sb.append("            {\n");
		sb.append("                return null;\n");
		sb.append("            }\n");
		sb.append("            String prettyName = ancestor.get").append(propNameUpper).append("();\n");
		sb.append("            if (prettyName == null)\n");
		sb.append("            {\n");
		sb.append("                return null;\n");
		sb.append("            }\n");
		sb.append("            return this.property.getType().getEnumerationLiterals()\n");
		sb.append("                    .detect(el -> el.getPrettyName().equals(prettyName));\n");
		sb.append("        }\n");
		sb.append("\n");

		// set() method - navigates through superclass
		sb.append("        @Override\n");
		sb
			.append("        public void set(@Nonnull ")
			.append(className)
			.append(" domainObject, @Nullable EnumerationLiteral value)\n");
		sb.append("        {\n");
		sb.append("            ").append(ancestorName).append(" ancestor = ").append(navigation).append(";\n");
		sb.append("            if (ancestor == null)\n");
		sb.append("            {\n");
		sb.append("                return;\n");
		sb.append("            }\n");
		sb
			.append("            ancestor.set")
			.append(propNameUpper)
			.append("(value == null ? null : value.getPrettyName());\n");
		sb.append("        }\n");
		sb.append("\n");

		sb.append("        @Override\n");
		sb.append("        @Nonnull\n");
		sb.append("        public cool.klass.model.meta.domain.api.property.EnumerationProperty getProperty()\n");
		sb.append("        {\n");
		sb.append("            return this.property;\n");
		sb.append("        }\n");
		sb.append("    }\n");
		sb.append("\n");

		return sb.toString();
	}

	private String getInheritedAssociationLensClass(
		@Nonnull Klass klass,
		@Nonnull AssociationEnd associationEnd,
		@Nonnull Klass ancestor,
		@Nonnull String navigation
	) {
		StringBuilder sb = new StringBuilder();

		String className = klass.getName();
		String ancestorName = ancestor.getName();
		String propName = associationEnd.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName);
		String lensClass = className + propNameUpper + "Lens";
		String targetType = associationEnd.getType().getName();
		boolean isToMany = associationEnd.getMultiplicity().isToMany();

		String interfaceType = isToMany
			? "ToManyLens<" + className + ", " + targetType + ">"
			: "ToOneLens<" + className + ", " + targetType + ">";

		String returnType = isToMany ? "ImmutableList<" + targetType + ">" : targetType;

		sb.append("    // Inherited from ").append(ancestorName).append("\n");
		sb.append("    private static class ").append(lensClass).append("\n");
		sb.append("            implements ").append(interfaceType).append("\n");
		sb.append("    {\n");
		sb.append("        private final cool.klass.model.meta.domain.api.property.AssociationEnd property;\n");
		sb.append("\n");
		sb
			.append("        ")
			.append(lensClass)
			.append("(cool.klass.model.meta.domain.api.property.AssociationEnd property)\n");
		sb.append("        {\n");
		sb.append("            this.property = property;\n");
		sb.append("        }\n");
		sb.append("\n");

		// get() method - navigates through superclass
		sb.append("        @Override\n");
		sb.append("        @Nullable\n");
		sb
			.append("        public ")
			.append(returnType)
			.append(" get(@Nonnull ")
			.append(className)
			.append(" domainObject)\n");
		sb.append("        {\n");
		sb.append("            ").append(ancestorName).append(" ancestor = ").append(navigation).append(";\n");
		sb.append("            if (ancestor == null)\n");
		sb.append("            {\n");
		sb.append("                return null;\n");
		sb.append("            }\n");
		if (isToMany) {
			sb.append("            return Lists.immutable.withAll(ancestor.get").append(propNameUpper).append("());\n");
		} else {
			sb.append("            return ancestor.get").append(propNameUpper).append("();\n");
		}
		sb.append("        }\n");
		sb.append("\n");

		// set() method - navigates through superclass
		sb.append("        @Override\n");
		sb
			.append("        public void set(@Nonnull ")
			.append(className)
			.append(" domainObject, @Nullable ")
			.append(returnType)
			.append(" value)\n");
		sb.append("        {\n");
		sb.append("            ").append(ancestorName).append(" ancestor = ").append(navigation).append(";\n");
		sb.append("            if (ancestor == null)\n");
		sb.append("            {\n");
		sb.append("                return;\n");
		sb.append("            }\n");
		if (isToMany) {
			sb.append("            if (value == null)\n");
			sb.append("            {\n");
			sb.append("                ancestor.get").append(propNameUpper).append("().clear();\n");
			sb.append("            }\n");
			sb.append("            else\n");
			sb.append("            {\n");
			sb.append("                ancestor.get").append(propNameUpper).append("().merge(value.castToList());\n");
			sb.append("            }\n");
		} else {
			sb.append("            ancestor.set").append(propNameUpper).append("(value);\n");
		}
		sb.append("        }\n");
		sb.append("\n");

		sb.append("        @Override\n");
		sb.append("        @Nonnull\n");
		sb.append("        public cool.klass.model.meta.domain.api.property.AssociationEnd getProperty()\n");
		sb.append("        {\n");
		sb.append("            return this.property;\n");
		sb.append("        }\n");
		sb.append("    }\n");
		sb.append("\n");

		return sb.toString();
	}

	private String getFactorySourceCode(@Nonnull ImmutableList<Klass> concreteClasses, String packageName) {
		StringBuilder sb = new StringBuilder();

		// Package declaration
		sb.append("package ").append(packageName).append(";\n\n");

		// Imports
		sb.append("import javax.annotation.Nonnull;\n");
		sb.append("\n");
		sb.append("import cool.klass.model.lens.ClassLens;\n");
		sb.append("import cool.klass.model.meta.domain.api.DomainModel;\n");
		sb.append("import cool.klass.model.meta.domain.api.Klass;\n");
		sb.append("\n");
		sb.append("import org.eclipse.collections.api.map.ImmutableMap;\n");
		sb.append("import org.eclipse.collections.impl.factory.Maps;\n");
		sb.append("\n");

		// Class javadoc
		sb.append("/**\n");
		sb.append(" * Auto-generated factory that creates and wires together all Reladomo ClassLenses.\n");
		sb.append(" *\n");
		sb.append(" * <p>Generated by {@link ").append(this.getClass().getCanonicalName()).append("}\n");
		sb.append(" */\n");

		// Class declaration
		sb.append("public class ReladomoClassLensFactory\n");
		sb.append("{\n");

		// Fields for each lens
		for (Klass klass : concreteClasses) {
			String fieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, klass.getName()) + "Lens";
			sb
				.append("    private final Reladomo")
				.append(klass.getName())
				.append("ClassLens ")
				.append(fieldName)
				.append(";\n");
		}

		sb.append("\n");
		sb.append("    private final ImmutableMap<Klass, ClassLens<?>> lensesByKlass;\n");
		sb.append("\n");

		// Constructor
		sb.append("    public ReladomoClassLensFactory(@Nonnull DomainModel domainModel)\n");
		sb.append("    {\n");
		for (Klass klass : concreteClasses) {
			String fieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, klass.getName()) + "Lens";
			sb
				.append("        this.")
				.append(fieldName)
				.append(" = new Reladomo")
				.append(klass.getName())
				.append("ClassLens(domainModel.getClassByName(\"")
				.append(klass.getName())
				.append("\"));\n");
		}

		sb.append("\n");
		sb.append("        this.lensesByKlass = Maps.immutable.<Klass, ClassLens<?>>empty()\n");
		for (Klass klass : concreteClasses) {
			String fieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, klass.getName()) + "Lens";
			sb
				.append("                .newWithKeyValue(this.")
				.append(fieldName)
				.append(".getKlass(), this.")
				.append(fieldName)
				.append(")\n");
		}
		sb.setLength(sb.length() - 1);
		sb.append(";\n");
		sb.append("    }\n");
		sb.append("\n");

		// Getter methods for each lens
		for (Klass klass : concreteClasses) {
			String fieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, klass.getName()) + "Lens";
			sb.append("    @Nonnull\n");
			sb
				.append("    public Reladomo")
				.append(klass.getName())
				.append("ClassLens get")
				.append(klass.getName())
				.append("Lens()\n");
			sb.append("    {\n");
			sb.append("        return this.").append(fieldName).append(";\n");
			sb.append("    }\n");
			sb.append("\n");
		}

		// Generic getter by Klass
		sb.append("    @Nonnull\n");
		sb.append("    public ClassLens<?> getLensByKlass(@Nonnull Klass klass)\n");
		sb.append("    {\n");
		sb.append("        ClassLens<?> lens = this.lensesByKlass.get(klass);\n");
		sb.append("        if (lens == null)\n");
		sb.append("        {\n");
		sb.append("            throw new IllegalArgumentException(\"No lens found for Klass: \" + klass.getName());\n");
		sb.append("        }\n");
		sb.append("        return lens;\n");
		sb.append("    }\n");
		sb.append("\n");

		// Get all lenses
		sb.append("    @Nonnull\n");
		sb.append("    public ImmutableMap<Klass, ClassLens<?>> getLensesByKlass()\n");
		sb.append("    {\n");
		sb.append("        return this.lensesByKlass;\n");
		sb.append("    }\n");

		sb.append("}\n");

		return sb.toString();
	}

	private void printStringToFile(@Nonnull Path path, String contents) {
		try (
			PrintStream printStream = new PrintStream(new FileOutputStream(path.toFile()), true, StandardCharsets.UTF_8)
		) {
			printStream.print(contents);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
