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

package cool.klass.generator.reladomo.lens;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Lists;

public class ReladomoLensGenerator {

	private static final Set<String> JAVA_KEYWORDS = Set.of(
		"abstract",
		"assert",
		"boolean",
		"break",
		"byte",
		"case",
		"catch",
		"char",
		"class",
		"const",
		"continue",
		"default",
		"do",
		"double",
		"else",
		"enum",
		"extends",
		"final",
		"finally",
		"float",
		"for",
		"goto",
		"if",
		"implements",
		"import",
		"instanceof",
		"int",
		"interface",
		"long",
		"native",
		"new",
		"package",
		"private",
		"protected",
		"public",
		"return",
		"short",
		"static",
		"strictfp",
		"super",
		"switch",
		"synchronized",
		"this",
		"throw",
		"throws",
		"transient",
		"try",
		"void",
		"volatile",
		"while"
	);

	private final DomainModel domainModel;
	private final String applicationName;

	public ReladomoLensGenerator(DomainModel domainModel, String applicationName) {
		this.domainModel = Objects.requireNonNull(domainModel);
		this.applicationName = Objects.requireNonNull(applicationName);
	}

	private static String getLowerCamelIdentifier(@Nonnull String upperCamelName) {
		String lowerCamel = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, upperCamelName);
		if (JAVA_KEYWORDS.contains(lowerCamel)) {
			return lowerCamel + "_";
		}
		return lowerCamel;
	}

	private ImmutableList<InheritedProperty<DataTypeProperty>> getInheritedDataTypeProperties(@Nonnull Klass klass) {
		String lowerCamelName = getLowerCamelIdentifier(klass.getName());
		MutableList<InheritedProperty<DataTypeProperty>> result = Lists.mutable.empty();
		this.collectInheritedDataTypeProperties(klass, klass, lowerCamelName, result);
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

		Set<String> alreadyCollected = new LinkedHashSet<>();
		for (DataTypeProperty p : originalKlass.getDeclaredDataTypeProperties()) {
			alreadyCollected.add(p.getName());
		}
		for (InheritedProperty<DataTypeProperty> inherited : result) {
			alreadyCollected.add(inherited.property().getName());
		}

		Set<String> superClassPropertyNames = superClass
			.getSuperClass()
			.map((sc) -> sc.getDataTypeProperties().collect(Property::getName).toSet())
			.orElseGet(org.eclipse.collections.impl.factory.Sets.mutable::empty);

		for (DataTypeProperty property : superClass
			.getDataTypeProperties()
			.reject(Property::isDerived)
			.reject(this::isTemporalRange)
			.reject((p) -> superClassPropertyNames.contains(p.getName()))
			.reject((p) -> alreadyCollected.contains(p.getName()))) {
			result.add(new InheritedProperty<>(property, superClass, navigation));
		}

		this.collectInheritedDataTypeProperties(originalKlass, superClass, navigation, result);
	}

	private ImmutableList<InheritedProperty<AssociationEnd>> getInheritedAssociationEnds(@Nonnull Klass klass) {
		String lowerCamelName = getLowerCamelIdentifier(klass.getName());
		MutableList<InheritedProperty<AssociationEnd>> result = Lists.mutable.empty();
		this.collectInheritedAssociationEnds(klass, lowerCamelName, result);
		return result.toImmutable();
	}

	private void collectInheritedAssociationEnds(
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

		this.collectInheritedAssociationEnds(superClass, navigation, result);
	}

	public void writeClassLenses(@Nonnull Path path) throws IOException {
		for (Klass klass : this.domainModel.getClasses()) {
			String packageName = klass.getPackageName() + ".lens.reladomo";
			String relativePath = packageName.replaceAll("\\.", "/");
			Path parentPath = path.resolve(relativePath);
			Files.createDirectories(parentPath);

			String fileName = "Reladomo_" + klass.getName() + "_ClassLens.java";
			Path outputPath = parentPath.resolve(fileName);

			String sourceCode = this.getClassLensSourceCode(klass);
			this.printStringToFile(outputPath, sourceCode);
		}

		// Also generate the factory class that wires everything together
		this.writeLensFactory(path);
		this.writeRegistryFactory(path);
	}

	private void writeLensFactory(@Nonnull Path path) throws IOException {
		ImmutableList<Klass> concreteClasses = this.domainModel.getClasses().reject(Klass::isAbstract);
		if (concreteClasses.isEmpty()) {
			return;
		}

		ImmutableList<Klass> allClasses = this.domainModel.getClasses();

		// TODO 2026-02-18: Should use the DomainModel's root package instead
		// Use the first class's package as the factory package
		String packageName = concreteClasses.getFirst().getPackageName() + ".lens.reladomo";
		String relativePath = packageName.replaceAll("\\.", "/");
		Path parentPath = path.resolve(relativePath);
		Files.createDirectories(parentPath);

		String factoryClassName = this.applicationName + "ReladomoLensFactory";
		String fileName = factoryClassName + ".java";
		Path outputPath = parentPath.resolve(fileName);

		String sourceCode = this.getFactorySourceCode(allClasses, packageName, factoryClassName);
		this.printStringToFile(outputPath, sourceCode);
	}

	private void writeRegistryFactory(@Nonnull Path path) throws IOException {
		ImmutableList<Klass> concreteClasses = this.domainModel.getClasses().reject(Klass::isAbstract);
		if (concreteClasses.isEmpty()) {
			return;
		}

		String packageName = concreteClasses.getFirst().getPackageName() + ".lens.reladomo";
		String relativePath = packageName.replaceAll("\\.", "/");
		Path parentPath = path.resolve(relativePath);
		Files.createDirectories(parentPath);

		String fileName = "ReladomoLensRegistryFactory.java";
		Path outputPath = parentPath.resolve(fileName);

		String sourceCode = this.getRegistryFactorySourceCode(packageName);
		this.printStringToFile(outputPath, sourceCode);
	}

	private String getRegistryFactorySourceCode(String packageName) {
		String factoryClassName = this.applicationName + "ReladomoLensFactory";

		// @formatter:off
		// language=JAVA
		return ""
				+ "package " + packageName + ";\n"
				+ "\n"
				+ "import javax.annotation.Nonnull;\n"
				+ "import javax.annotation.processing.Generated;\n"
				+ "\n"
				+ "import com.fasterxml.jackson.annotation.JsonTypeName;\n"
				+ "import com.google.auto.service.AutoService;\n"
				+ "import cool.klass.model.lens.LensRegistry;\n"
				+ "import cool.klass.model.lens.LensRegistryFactory;\n"
				+ "import cool.klass.model.meta.domain.api.DomainModel;\n"
				+ "\n"
				+ "/**\n"
				+ " * Auto-generated factory for creating " + factoryClassName + " from config.\n"
				+ " */\n"
				+ "@Generated(\"" + this.getClass().getCanonicalName() + "\")\n"
				+ "@JsonTypeName(\"" + this.applicationName + "\")\n"
				+ "@AutoService(LensRegistryFactory.class)\n"
				+ "public class ReladomoLensRegistryFactory\n"
				+ "        implements LensRegistryFactory\n"
				+ "{\n"
				+ "    @Override\n"
				+ "    public LensRegistry createLensRegistry(@Nonnull DomainModel domainModel)\n"
				+ "    {\n"
				+ "        return new " + factoryClassName + "(domainModel);\n"
				+ "    }\n"
				+ "}\n";
		// @formatter:on
	}

	public String getClassLensSourceCode(@Nonnull Klass klass) {
		Objects.requireNonNull(klass);

		String packageName = klass.getPackageName() + ".lens.reladomo";
		String className = klass.getName();
		String lensClassName = "Reladomo_" + className + "_ClassLens";

		// @formatter:off
		// language=JAVA
		String sourceCode = ""
				+ "package " + packageName + ";\n"
				+ "\n"
				+ this.getImports(klass)
				+ "/**\n"
				+ " * Auto-generated Reladomo ClassLens for {@link " + className + "}.\n"
				+ " */\n"
				+ "@Generated(\"" + this.getClass().getCanonicalName() + "\")\n"
				+ "public class " + lensClassName + "\n"
				+ "        implements ReladomoClassLens<" + className + ">\n"
				+ "{\n"
				+ this.getFields(klass)
				+ this.getConstructor(klass, lensClassName)
				+ this.getClassLensMethods(klass)
				+ this.getInnerLensClasses(klass)
				+ "}\n";
		// @formatter:on

		return sourceCode;
	}

	private boolean shouldIncludeDerived(@Nonnull Klass klass) {
		return !klass.isAbstract();
	}

	private String getImports(@Nonnull Klass klass) {
		ImmutableList<PrimitiveProperty> primitiveProperties = klass
			.getDataTypeProperties()
			.selectInstancesOf(PrimitiveProperty.class)
			.reject((p) -> !this.shouldIncludeDerived(klass) && p.isDerived())
			.reject((p) -> p.getType().isTemporalRange());

		ImmutableList<EnumerationProperty> enumerationProperties = klass
			.getDataTypeProperties()
			.selectInstancesOf(EnumerationProperty.class)
			.reject((p) -> !this.shouldIncludeDerived(klass) && p.isDerived());

		ImmutableList<AssociationEnd> declaredAssociationEnds = klass.getDeclaredAssociationEnds();
		ImmutableList<InheritedProperty<AssociationEnd>> inheritedAssociationEnds = this.getInheritedAssociationEnds(
			klass
		);

		boolean hasInstant = primitiveProperties.anySatisfy(
			(p) -> p.getType() == PrimitiveType.INSTANT || p.getType() == PrimitiveType.TEMPORAL_INSTANT
		);
		boolean hasLocalDate = primitiveProperties.anySatisfy((p) -> p.getType() == PrimitiveType.LOCAL_DATE);
		boolean hasNonDerivedInstant = primitiveProperties.anySatisfy(
			(p) -> !p.isDerived()
				&& (p.getType() == PrimitiveType.INSTANT || p.getType() == PrimitiveType.TEMPORAL_INSTANT)
		);
		boolean hasNonDerivedLocalDate = primitiveProperties.anySatisfy(
			(p) -> !p.isDerived() && p.getType() == PrimitiveType.LOCAL_DATE
		);
		boolean hasTimestamp = hasNonDerivedInstant; // Reladomo uses Timestamp for Instant/TemporalInstant
		boolean hasDate = hasNonDerivedLocalDate; // Reladomo uses Date for LocalDate
		boolean hasToOne =
			declaredAssociationEnds.anySatisfy((ae) -> !ae.getMultiplicity().isToMany())
			|| inheritedAssociationEnds.anySatisfy((ip) -> !ip.property().getMultiplicity().isToMany());
		boolean hasToMany =
			declaredAssociationEnds.anySatisfy((ae) -> ae.getMultiplicity().isToMany())
			|| inheritedAssociationEnds.anySatisfy((ip) -> ip.property().getMultiplicity().isToMany());
		boolean hasAnyAssociationEnd = declaredAssociationEnds.notEmpty() || inheritedAssociationEnds.notEmpty();

		Set<String> javaSqlImports = new LinkedHashSet<>();
		Set<String> javaTimeImports = new LinkedHashSet<>();
		Set<String> javaUtilImports = new LinkedHashSet<>();
		Set<String> javaxImports = new LinkedHashSet<>();
		Set<String> lensImports = new LinkedHashSet<>();
		Set<String> metaImports = new LinkedHashSet<>();
		Set<String> eclipseCollectionsImports = new LinkedHashSet<>();
		Set<String> reladomoTypeImports = new LinkedHashSet<>();
		Set<String> associationTypeImports = new LinkedHashSet<>();

		// java.sql imports
		if (hasTimestamp || hasDate) {
			javaSqlImports.add("java.sql.Timestamp");
			if (hasDate) {
				javaSqlImports.add("java.sql.Date");
			}
		}

		// java.time imports
		if (hasInstant) {
			javaTimeImports.add("java.time.Instant");
		}
		if (hasLocalDate) {
			javaTimeImports.add("java.time.LocalDate");
		}

		// java.util imports
		javaUtilImports.add("java.util.Objects");

		// javax imports
		javaxImports.add("javax.annotation.processing.Generated");
		javaxImports.add("javax.annotation.Nonnull");
		javaxImports.add("javax.annotation.Nullable");

		// Lens imports
		lensImports.add("cool.klass.model.lens.AssociationLens");
		lensImports.add("cool.klass.model.lens.DataTypeLens");
		lensImports.add("cool.klass.model.lens.EnumerationLens");
		lensImports.add("cool.klass.model.lens.PrimitiveLens");
		lensImports.add("cool.klass.model.lens.PropertyLens");
		lensImports.add("cool.klass.model.lens.ReferenceLens");
		lensImports.add("cool.klass.model.lens.reladomo.ReladomoClassLens");

		// Type-specific lens imports
		for (PrimitiveProperty property : primitiveProperties) {
			String lensType = this.getLensInterfaceImport(property.getType());
			if (lensType != null) {
				lensImports.add(lensType);
			}
		}

		if (hasAnyAssociationEnd) {
			lensImports.add("cool.klass.model.lens.reladomo.ReladomoAssociationLens");
			if (hasToOne) {
				lensImports.add("cool.klass.model.lens.ToOneLens");
			}
			if (hasToMany) {
				lensImports.add("cool.klass.model.lens.ToManyLens");
			}
		}

		// Domain model imports
		if (enumerationProperties.notEmpty()) {
			metaImports.add("cool.klass.model.meta.domain.api.EnumerationLiteral");
		}

		// Eclipse Collections imports
		eclipseCollectionsImports.add("org.eclipse.collections.api.list.ImmutableList");
		eclipseCollectionsImports.add("org.eclipse.collections.api.map.ImmutableMap");
		eclipseCollectionsImports.add("org.eclipse.collections.impl.factory.Lists");

		// Reladomo type imports
		reladomoTypeImports.add("com.gs.fw.common.mithra.attribute.Attribute");
		reladomoTypeImports.add(klass.getPackageName() + "." + klass.getName());
		reladomoTypeImports.add(klass.getPackageName() + "." + klass.getName() + "Finder");

		if (hasToMany) {
			reladomoTypeImports.add("com.gs.fw.common.mithra.list.merge.TopLevelMergeOptions");
		}

		// Superclass type imports for navigation and getAttribute()
		for (Klass ancestor : klass.getSuperClassChain()) {
			reladomoTypeImports.add(ancestor.getPackageName() + "." + ancestor.getName());
			reladomoTypeImports.add(ancestor.getPackageName() + "." + ancestor.getName() + "Finder");
		}

		if (klass.isSystemTemporal()) {
			reladomoTypeImports.add("cool.klass.reladomo.utc.infinity.timestamp.UtcInfinityTimestamp");
		}

		if (hasAnyAssociationEnd) {
			reladomoTypeImports.add("com.gs.fw.common.mithra.finder.AbstractRelatedFinder");
		}

		// Association end target type imports - declared
		for (AssociationEnd associationEnd : declaredAssociationEnds) {
			Klass targetType = associationEnd.getType();
			associationTypeImports.add(targetType.getPackageName() + "." + targetType.getName());
			if (associationEnd.getMultiplicity().isToMany()) {
				associationTypeImports.add(targetType.getPackageName() + "." + targetType.getName() + "Finder");
				associationTypeImports.add(targetType.getPackageName() + "." + targetType.getName() + "List");
			}
		}

		// Association end target type imports - inherited
		for (InheritedProperty<AssociationEnd> inherited : inheritedAssociationEnds) {
			Klass targetType = inherited.property().getType();
			associationTypeImports.add(targetType.getPackageName() + "." + targetType.getName());
			if (inherited.property().getMultiplicity().isToMany()) {
				associationTypeImports.add(targetType.getPackageName() + "." + targetType.getName() + "Finder");
				associationTypeImports.add(targetType.getPackageName() + "." + targetType.getName() + "List");
			}
		}

		associationTypeImports.removeAll(reladomoTypeImports);

		return this.renderImportGroups(
			javaSqlImports,
			javaTimeImports,
			javaUtilImports,
			javaxImports,
			lensImports,
			metaImports,
			eclipseCollectionsImports,
			reladomoTypeImports,
			associationTypeImports
		);
	}

	private String getLensInterfaceImport(@Nonnull PrimitiveType type) {
		return switch (type) {
			case INTEGER -> "cool.klass.model.lens.primitive.IntegerLens";
			case LONG -> "cool.klass.model.lens.primitive.LongLens";
			case DOUBLE -> "cool.klass.model.lens.primitive.DoubleLens";
			case FLOAT -> "cool.klass.model.lens.primitive.FloatLens";
			case BOOLEAN -> "cool.klass.model.lens.primitive.BooleanLens";
			case STRING -> "cool.klass.model.lens.primitive.StringLens";
			case INSTANT, TEMPORAL_INSTANT -> "cool.klass.model.lens.primitive.InstantLens";
			case LOCAL_DATE -> "cool.klass.model.lens.primitive.LocalDateLens";
			case TEMPORAL_RANGE -> null; // Should be filtered out
			default -> null;
		};
	}

	@SafeVarargs
	private String renderImportGroups(Set<String>... groups) {
		return Lists.mutable
			.with(groups)
			.reject(Set::isEmpty)
			.collect((group) ->
				Lists.mutable
					.withAll(group)
					.collect((fqcn) -> "import " + fqcn + ";\n")
					.makeString("")
			)
			.makeString("\n");
	}

	private String getFields(@Nonnull Klass klass) {
		ImmutableList<DataTypeProperty> dataTypeProperties = this.getIncludedDataTypeProperties(klass);
		ImmutableList<AssociationEnd> associationEnds = this.getIncludedAssociationEnds(klass);

		String dataTypeFields = dataTypeProperties
			.collect(
				(property) ->
					"    public final " + this.getLensFieldType(klass, property) + " " + property.getName() + ";\n"
			)
			.makeString("");

		String associationFields = associationEnds
			.collect(
				(associationEnd) ->
					"    public final "
					+ this.getAssociationLensFieldType(associationEnd, klass)
					+ " "
					+ associationEnd.getName()
					+ ";\n"
			)
			.makeString("");

		String className = klass.getName();

		// @formatter:off
		return ""
				+ "    private final cool.klass.model.meta.domain.api.Klass metaKlass;\n"
				+ "\n"
				+ dataTypeFields
				+ associationFields
				+ "\n"
				+ "    private final ImmutableMap<cool.klass.model.meta.domain.api.property.DataTypeProperty, DataTypeLens<" + className + ", ?>> dataTypePropertyLenses;\n"
				+ "    private final ImmutableMap<cool.klass.model.meta.domain.api.property.PrimitiveProperty, PrimitiveLens<" + className + ", ?>> primitivePropertyLenses;\n"
				+ "    private final ImmutableMap<cool.klass.model.meta.domain.api.property.EnumerationProperty, EnumerationLens<" + className + ">> enumerationPropertyLenses;\n"
				+ "    private final ImmutableMap<cool.klass.model.meta.domain.api.property.AssociationEnd, AssociationLens<" + className + ", ?>> associationEndLenses;\n"
				+ "    private final ImmutableMap<cool.klass.model.meta.domain.api.property.Property, PropertyLens<" + className + ", ?>> propertyLenses;\n"
				+ "\n";
		// @formatter:on
	}

	private ImmutableList<DataTypeProperty> getIncludedDataTypeProperties(@Nonnull Klass klass) {
		Set<String> declaredAndInheritedNames = new LinkedHashSet<>();
		klass.getDeclaredDataTypeProperties().forEach((p) -> declaredAndInheritedNames.add(p.getName()));
		this.getInheritedDataTypeProperties(klass).forEach((p) ->
			declaredAndInheritedNames.add(p.property().getName())
		);

		return klass
			.getDataTypeProperties()
			.reject((property) -> !this.shouldIncludeDerived(klass) && property.isDerived())
			.reject((property) -> property.isDerived() && !declaredAndInheritedNames.contains(property.getName()))
			.reject(this::isTemporalRange);
	}

	private ImmutableList<AssociationEnd> getIncludedAssociationEnds(@Nonnull Klass klass) {
		ImmutableList<InheritedProperty<AssociationEnd>> inheritedAssociationEnds = this.getInheritedAssociationEnds(
			klass
		);
		return klass
			.getDeclaredAssociationEnds()
			.newWithAll(inheritedAssociationEnds.collect(InheritedProperty::property));
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
		ImmutableList<DataTypeProperty> dataTypeProperties = this.getIncludedDataTypeProperties(klass);
		ImmutableList<AssociationEnd> associationEnds = this.getIncludedAssociationEnds(klass);

		String className = klass.getName();

		// Initialize data type lens fields
		String initDataType = dataTypeProperties
			.collect((property) ->
				MessageFormat.format(
					"        this.{0} = new {1}({2});\n",
					property.getName(),
					this.getLensClassName(klass, property),
					this.getPropertyLookup(property)
				)
			)
			.makeString("");

		// Initialize association end lens fields
		String initAssociation = associationEnds
			.collect((associationEnd) ->
				MessageFormat.format(
					"        this.{0} = new {1}(klass.getAssociationEndByName(\"{0}\"));\n",
					associationEnd.getName(),
					this.getAssociationLensClassName(klass, associationEnd)
				)
			)
			.makeString("");

		// Build typed sublists for groupByUniqueKey — no unchecked casts needed
		ImmutableList<PrimitiveProperty> primitiveProperties = dataTypeProperties.selectInstancesOf(
			PrimitiveProperty.class
		);
		ImmutableList<EnumerationProperty> enumerationProperties = dataTypeProperties.selectInstancesOf(
			EnumerationProperty.class
		);

		String primitiveListEntries = primitiveProperties
			.collect((property) -> "this." + property.getName())
			.makeString(", ");
		String enumerationListEntries = enumerationProperties
			.collect((property) -> "this." + property.getName())
			.makeString(", ");
		String dataTypeListEntries = dataTypeProperties
			.collect((property) -> "this." + property.getName())
			.makeString(", ");
		String associationListEntries = associationEnds
			.collect((associationEnd) -> "this." + associationEnd.getName())
			.makeString(", ");

		// Combine all property names for the propertyLenses map
		MutableList<String> allPropertyNames = Lists.mutable.empty();
		allPropertyNames.addAllIterable(dataTypeProperties.collect((property) -> "this." + property.getName()));
		allPropertyNames.addAllIterable(
			associationEnds.collect((associationEnd) -> "this." + associationEnd.getName())
		);
		String allListEntries = allPropertyNames.makeString(", ");

		// @formatter:off
		return ""
				+ "    public " + lensClassName + "(@Nonnull cool.klass.model.meta.domain.api.Klass klass)\n"
				+ "    {\n"
				+ "        this.metaKlass = Objects.requireNonNull(klass);\n"
				+ "\n"
				+ initDataType
				+ initAssociation
				+ "\n"
				+ "        ImmutableList<PrimitiveLens<" + className + ", ?>> primitiveList = Lists.immutable.with(" + primitiveListEntries + ");\n"
				+ "        ImmutableList<EnumerationLens<" + className + ">> enumerationList = Lists.immutable.with(" + enumerationListEntries + ");\n"
				+ "        ImmutableList<AssociationLens<" + className + ", ?>> associationList = Lists.immutable.with(" + associationListEntries + ");\n"
				+ "\n"
				+ "        this.primitivePropertyLenses = primitiveList.groupByUniqueKey(PrimitiveLens::getProperty);\n"
				+ "        this.enumerationPropertyLenses = enumerationList.groupByUniqueKey(EnumerationLens::getProperty);\n"
				+ "        this.dataTypePropertyLenses = Lists.immutable.<DataTypeLens<" + className + ", ?>>empty().newWithAll(primitiveList).newWithAll(enumerationList).groupByUniqueKey(DataTypeLens::getProperty);\n"
				+ "        this.associationEndLenses = associationList.groupByUniqueKey(AssociationLens::getProperty);\n"
				+ "        this.propertyLenses = Lists.immutable.<PropertyLens<" + className + ", ?>>withAll(primitiveList).newWithAll(enumerationList).newWithAll(associationList).groupByUniqueKey(PropertyLens::getProperty);\n"
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	private String getPropertyLookup(@Nonnull DataTypeProperty property) {
		if (property instanceof PrimitiveProperty) {
			return "klass.getPrimitivePropertyByName(\"" + property.getName() + "\")";
		}
		if (property instanceof EnumerationProperty) {
			return "klass.getEnumerationPropertyByName(\"" + property.getName() + "\")";
		}
		throw new IllegalStateException("Unknown property type: " + property.getClass());
	}

	private String getLensClassName(@Nonnull Klass klass, @Nonnull DataTypeProperty property) {
		String className = klass.getName();
		String propName = property.getName();
		String suffix = property instanceof PrimitiveProperty ? "PrimitivePropertyLens" : "EnumerationPropertyLens";
		return "Reladomo_" + className + "_" + propName + "_" + suffix;
	}

	private String getAssociationLensClassName(@Nonnull Klass klass, @Nonnull AssociationEnd associationEnd) {
		String className = klass.getName();
		String propName = associationEnd.getName();
		return "Reladomo_" + className + "_" + propName + "_AssociationEndLens";
	}

	private String getClassLensMethods(@Nonnull Klass klass) {
		String className = klass.getName();

		// @formatter:off
		String result = ""
				+ "    @Override\n"
				+ "    @Nonnull\n"
				+ "    public cool.klass.model.meta.domain.api.Klass getKlass()\n"
				+ "    {\n"
				+ "        return this.metaKlass;\n"
				+ "    }\n"
				+ "\n"
				+ "    @Override\n"
				+ "    @Nonnull\n"
				+ "    public PropertyLens<" + className + ", ?> getLensByProperty(@Nonnull cool.klass.model.meta.domain.api.property.Property property)\n"
				+ "    {\n"
				+ "        return Objects.requireNonNull(\n"
				+ "                this.propertyLenses.get(property),\n"
				+ "                () -> \"No lens found for property: \" + property.getName());\n"
				+ "    }\n"
				+ "\n"
				+ this.getDirectOverload("DataTypeLens<" + className + ", ?>", "cool.klass.model.meta.domain.api.property.DataTypeProperty", "dataTypePropertyLenses")
				+ this.getDirectOverload("PrimitiveLens<" + className + ", ?>", "cool.klass.model.meta.domain.api.property.PrimitiveProperty", "primitivePropertyLenses")
				+ this.getDirectOverload("EnumerationLens<" + className + ">", "cool.klass.model.meta.domain.api.property.EnumerationProperty", "enumerationPropertyLenses")
				+ this.getCastOverload(className, "ReferenceLens", "cool.klass.model.meta.domain.api.property.ReferenceProperty", "propertyLenses")
				+ this.getDirectOverload("AssociationLens<" + className + ", ?>", "cool.klass.model.meta.domain.api.property.AssociationEnd", "associationEndLenses")
				+ "    @Override\n"
				+ "    @Nonnull\n"
				+ "    public com.gs.fw.common.mithra.finder.RelatedFinder<" + className + "> getRelatedFinder()\n"
				+ "    {\n"
				+ "        return " + className + "Finder.getFinderInstance();\n"
				+ "    }\n"
				+ "\n"
				+ this.getInstantiateMethod(klass)
			+ this.getGetJavaClassMethod(klass)
			+ this.getGenerateAndSetIdMethod(klass)
			+ this.getHierarchyMethods(klass)
			+ this.getClassLensToString("Reladomo_" + className + "_ClassLens");
		// @formatter:on

		return result;
	}

	private String getInstantiateMethod(@Nonnull Klass klass) {
		String className = klass.getName();

		if (klass.isAbstract()) {
			// @formatter:off
			return ""
					+ "    @Override\n"
					+ "    @Nonnull\n"
					+ "    public " + className + " instantiate()\n"
					+ "    {\n"
					+ "        throw new UnsupportedOperationException(\"Cannot instantiate abstract class: " + className + "\");\n"
					+ "    }\n"
					+ "\n";
			// @formatter:on
		}

		if (klass.isSystemTemporal()) {
			// @formatter:off
			return ""
					+ "    @Override\n"
					+ "    @Nonnull\n"
					+ "    public " + className + " instantiate()\n"
					+ "    {\n"
					+ "        return new " + className + "(UtcInfinityTimestamp.getDefaultInfinity());\n"
					+ "    }\n"
					+ "\n";
			// @formatter:on
		}

		// @formatter:off
		return ""
				+ "    @Override\n"
				+ "    @Nonnull\n"
				+ "    public " + className + " instantiate()\n"
				+ "    {\n"
				+ "        return new " + className + "();\n"
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	private String getGetJavaClassMethod(@Nonnull Klass klass) {
		String className = klass.getName();

		// @formatter:off
		return ""
				+ "    @Override\n"
				+ "    @Nonnull\n"
				+ "    public Class<" + className + "> getJavaClass()\n"
				+ "    {\n"
				+ "        return " + className + ".class;\n"
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	private String getGenerateAndSetIdMethod(@Nonnull Klass klass) {
		String className = klass.getName();

		ImmutableList<DataTypeProperty> idProperties = klass.getDataTypeProperties().select(DataTypeProperty::isID);
		if (idProperties.isEmpty()) {
			return "";
		}

		PrimitiveProperty idProperty = (PrimitiveProperty) idProperties.getOnly();

		// Non-numeric IDs (e.g., String) are handled by the data store via UUID supplier
		if (!idProperty.getType().isNumeric()) {
			return "";
		}

		String idPropNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, idProperty.getName());

		// Navigate to the owning class if the ID is inherited
		Klass owningKlass = (Klass) idProperty.getOwningClassifier();
		String navigation = "instance";
		if (!owningKlass.equals(klass)) {
			// Build navigation chain to the owning superclass
			Klass current = klass;
			while (!current.equals(owningKlass)) {
				Optional<Klass> superClass = current.getSuperClass();
				if (superClass.isEmpty()) {
					throw new AssertionError(
						"Could not find owning class " + owningKlass.getName() + " in superclass chain of " + className
					);
				}
				navigation = navigation + ".get" + superClass.get().getName() + "SuperClass()";
				current = superClass.get();
			}
		}

		// @formatter:off
		return ""
				+ "    @Override\n"
				+ "    public void generateAndSetId(@Nonnull " + className + " instance)\n"
				+ "    {\n"
				+ "        " + navigation + ".generateAndSet" + idPropNameUpper + "();\n"
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	private String getHierarchyMethods(@Nonnull Klass klass) {
		return this.getSuperClassInstanceMethod(klass) + this.getSubClassInstanceMethod(klass);
	}

	private String getSuperClassInstanceMethod(@Nonnull Klass klass) {
		Optional<Klass> optionalSuperClass = klass.getSuperClass();
		if (optionalSuperClass.isEmpty()) {
			return "";
		}

		String className = klass.getName();
		String superClassName = optionalSuperClass.get().getName();

		// @formatter:off
		return ""
				+ "    @Override\n"
				+ "    @Nullable\n"
				+ "    public " + superClassName + " getSuperClassInstance(@Nonnull " + className + " instance)\n"
				+ "    {\n"
				+ "        return instance.get" + superClassName + "SuperClass();\n"
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	private String getSubClassInstanceMethod(@Nonnull Klass klass) {
		ImmutableList<Klass> subClasses = klass.getSubClasses();
		if (subClasses.isEmpty()) {
			return "";
		}

		String className = klass.getName();

		String switchCases = subClasses
			.collect((subClass) -> {
				String subName = subClass.getName();
				return "            case \"" + subName + "\" -> instance.get" + subName + "SubClass();\n";
			})
			.makeString("");

		// @formatter:off
		return ""
				+ "    @Override\n"
				+ "    @Nullable\n"
				+ "    public Object getSubClassInstance(@Nonnull " + className + " instance, @Nonnull cool.klass.model.meta.domain.api.Klass subClass)\n"
				+ "    {\n"
				+ "        return switch (subClass.getName())\n"
				+ "        {\n"
				+ switchCases
				+ "            default -> throw new AssertionError(\"Unknown subclass of " + className + ": \" + subClass.getName());\n"
				+ "        };\n"
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	private String getClassLensToString(String lensClassName) {
		// @formatter:off
		return ""
				+ "    @Override\n"
				+ "    public String toString()\n"
				+ "    {\n"
				+ "        return \"" + lensClassName + "\";\n"
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	private String getCastOverload(String className, String returnLensType, String paramPropertyType, String mapName) {
		// @formatter:off
		return ""
				+ "    @Override\n"
				+ "    @Nonnull\n"
				+ "    public " + returnLensType + "<" + className + ", ?> getLensByProperty(@Nonnull " + paramPropertyType + " property)\n"
				+ "    {\n"
				+ "        return (" + returnLensType + "<" + className + ", ?>) Objects.requireNonNull(\n"
				+ "                this." + mapName + ".get(property),\n"
				+ "                () -> \"No lens found for property: \" + property.getName());\n"
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	private String getDirectOverload(String returnType, String paramPropertyType, String mapName) {
		// @formatter:off
		return ""
				+ "    @Override\n"
				+ "    @Nonnull\n"
				+ "    public " + returnType + " getLensByProperty(@Nonnull " + paramPropertyType + " property)\n"
				+ "    {\n"
				+ "        return Objects.requireNonNull(\n"
				+ "                this." + mapName + ".get(property),\n"
				+ "                () -> \"No lens found for property: \" + property.getName());\n"
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	private String getInnerLensClasses(@Nonnull Klass klass) {
		ImmutableList<DataTypeProperty> dataTypeProperties = this.getIncludedDataTypeProperties(klass);
		ImmutableList<AssociationEnd> associationEnds = this.getIncludedAssociationEnds(klass);

		ImmutableMap<String, InheritedProperty<DataTypeProperty>> inheritedDataTypeByName =
			this.getInheritedDataTypeProperties(klass).groupByUniqueKey((inherited) -> inherited.property().getName());
		ImmutableMap<String, InheritedProperty<AssociationEnd>> inheritedAssociationByName =
			this.getInheritedAssociationEnds(klass).groupByUniqueKey((inherited) -> inherited.property().getName());

		String dataTypeLensClasses = dataTypeProperties
			.collect((property) -> {
				InheritedProperty<DataTypeProperty> inherited = inheritedDataTypeByName.get(property.getName());
				if (inherited != null) {
					return this.getInheritedDataTypeLensClass(klass, inherited);
				}
				return this.getDataTypeLensClass(klass, property);
			})
			.makeString("");

		String associationLensClasses = associationEnds
			.collect((associationEnd) -> {
				InheritedProperty<AssociationEnd> inherited = inheritedAssociationByName.get(associationEnd.getName());
				if (inherited != null) {
					return this.getInheritedAssociationLensClass(
						klass,
						inherited.property(),
						inherited.ancestor(),
						inherited.navigation()
					);
				}
				return this.getAssociationLensClass(klass, associationEnd);
			})
			.makeString("");

		return dataTypeLensClasses + associationLensClasses;
	}

	private String getDataTypeLensClass(@Nonnull Klass klass, @Nonnull DataTypeProperty property) {
		if (property instanceof PrimitiveProperty primitiveProperty) {
			return this.getPrimitiveLensClass(klass, primitiveProperty);
		}
		if (property instanceof EnumerationProperty enumerationProperty) {
			return this.getEnumerationLensClass(klass, enumerationProperty);
		}
		throw new IllegalStateException("Unknown property type: " + property.getClass());
	}

	private String getInheritedDataTypeLensClass(
		@Nonnull Klass klass,
		@Nonnull InheritedProperty<DataTypeProperty> inherited
	) {
		if (inherited.property() instanceof PrimitiveProperty primitiveProperty) {
			return this.getInheritedPrimitiveLensClass(
				klass,
				primitiveProperty,
				inherited.ancestor(),
				inherited.navigation()
			);
		}
		if (inherited.property() instanceof EnumerationProperty enumerationProperty) {
			return this.getInheritedEnumerationLensClass(
				klass,
				enumerationProperty,
				inherited.ancestor(),
				inherited.navigation()
			);
		}
		throw new IllegalStateException("Unknown property type: " + inherited.property().getClass());
	}

	private String getPrimitiveLensClass(@Nonnull Klass klass, @Nonnull PrimitiveProperty property) {
		String className = klass.getName();
		String lowerCamelName = getLowerCamelIdentifier(className);
		String propName = property.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName);
		String lensClass = "Reladomo_" + className + "_" + propName + "_PrimitivePropertyLens";
		String interfaceType = this.getPrimitiveLensType(property.getType()) + "<" + className + ">";
		String javaType = this.getJavaType(property.getType());
		boolean isBoolean = property.getType() == PrimitiveType.BOOLEAN;
		String getterName = (isBoolean ? "is" : "get") + propNameUpper;
		String setterName = "set" + propNameUpper;
		boolean needsConversion = this.needsTypeConversion(property.getType());
		// Derived properties don't have Reladomo-generated isXxxNull() methods
		boolean needsNullCheck =
			!property.isDerived() && property.isOptional() && this.hasUnboxedMethods(property.getType());

		// @formatter:off
		String getBody;
		if (property.isDerived() && needsConversion) {
			getBody = "            return " + lowerCamelName + "." + getterName + "();\n";
		} else if (needsConversion) {
			getBody = this.getConversionGetterBody(lowerCamelName, property, getterName);
		} else if (needsNullCheck) {
			getBody = ""
				+ "            if (" + lowerCamelName + ".is" + propNameUpper + "Null())\n"
				+ "            {\n"
				+ "                return null;\n"
				+ "            }\n"
				+ "            return " + lowerCamelName + "." + getterName + "();\n";
		} else {
			getBody = "            return " + lowerCamelName + "." + getterName + "();\n";
		}

		String setBody;
		if (property.isDerived()) {
			setBody = "            throw new UnsupportedOperationException(\"Cannot set derived property: " + propName + "\");\n";
		} else if (needsConversion) {
			setBody = this.getConversionSetterBody(lowerCamelName, property, setterName);
		} else {
			setBody = "            " + lowerCamelName + "." + setterName + "(value);\n";
		}
		// @formatter:on

		String isNullBody;
		String setNullBody;
		if (needsNullCheck) {
			// Optional primitive types with isXxxNull()/setXxxNull() on the Reladomo class
			isNullBody = "            return " + lowerCamelName + ".is" + propNameUpper + "Null();\n";
			setNullBody = "            " + lowerCamelName + ".set" + propNameUpper + "Null();\n";
		} else if (property.isDerived()) {
			isNullBody = "            return this.get(" + lowerCamelName + ") == null;\n";
			setNullBody =
				"            throw new UnsupportedOperationException(\"Cannot set derived property: "
				+ propName
				+ "\");\n";
		} else if (this.hasUnboxedMethods(property.getType()) && property.isRequired()) {
			// Required primitive types — cannot be null
			isNullBody = "            return false;\n";
			setNullBody =
				"            throw new UnsupportedOperationException(\"Cannot set required primitive property to null: "
				+ propName
				+ "\");\n";
		} else {
			// Reference types (String, Instant, LocalDate) — null is a valid value
			isNullBody = "            return this.get(" + lowerCamelName + ") == null;\n";
			setNullBody = "            " + lowerCamelName + "." + setterName + "(null);\n";
		}

		String unboxed = this.hasUnboxedMethods(property.getType())
			? (property.isDerived()
					? this.getDerivedUnboxedMethods(klass, property)
					: this.getUnboxedMethods(klass, property))
			: "";

		String getAttributeMethod = property.isDerived()
			? ""
			: ""
			+ "\n"
			+ "        @Nonnull\n"
			+ "        public Attribute getAttribute()\n"
			+ "        {\n"
			+ "            return "
			+ className
			+ "Finder."
			+ propName
			+ "();\n"
			+ "        }\n";

		// @formatter:off
		// language=JAVA
		return ""
				+ "    private static class " + lensClass + "\n"
				+ "            implements " + interfaceType + "\n"
				+ "    {\n"
				+ "        private final cool.klass.model.meta.domain.api.property.PrimitiveProperty property;\n"
				+ "\n"
				+ "        " + lensClass + "(cool.klass.model.meta.domain.api.property.PrimitiveProperty property)\n"
				+ "        {\n"
				+ "            this.property = Objects.requireNonNull(property);\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        @Nullable\n"
				+ "        public " + javaType + " get(@Nonnull " + className + " " + lowerCamelName + ")\n"
				+ "        {\n"
				+ getBody
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void set(@Nonnull " + className + " " + lowerCamelName + ", @Nullable " + javaType + " value)\n"
				+ "        {\n"
				+ setBody
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public boolean isNull(@Nonnull " + className + " " + lowerCamelName + ")\n"
				+ "        {\n"
				+ isNullBody
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void setNull(@Nonnull " + className + " " + lowerCamelName + ")\n"
				+ "        {\n"
				+ setNullBody
				+ "        }\n"
				+ unboxed
				+ "\n"
				+ "        @Override\n"
				+ "        @Nonnull\n"
				+ "        public cool.klass.model.meta.domain.api.property.PrimitiveProperty getPrimitiveProperty()\n"
				+ "        {\n"
				+ "            return this.property;\n"
				+ "        }\n"
				+ getAttributeMethod
				+ "\n"
				+ "        @Override\n"
				+ "        public String toString()\n"
				+ "        {\n"
				+ "            return \"" + lensClass + "\";\n"
				+ "        }\n"
				+ "    }\n"
				+ "\n";
		// @formatter:on
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

	// @formatter:off
	private String getConversionGetterBody(String varName, @Nonnull PrimitiveProperty property, String getterName) {
		PrimitiveType type = property.getType();
		if (type == PrimitiveType.INSTANT || type == PrimitiveType.TEMPORAL_INSTANT) {
			return ""
				+ "            Timestamp ts = " + varName + "." + getterName + "();\n"
				+ "            return ts == null ? null : ts.toInstant();\n";
		}
		if (type == PrimitiveType.LOCAL_DATE) {
			return ""
				+ "            java.util.Date date = " + varName + "." + getterName + "();\n"
				+ "            return date == null ? null : ((java.sql.Date) date).toLocalDate();\n";
		}
		throw new IllegalStateException("Unexpected type needing conversion: " + type);
	}

	private String getConversionSetterBody(String varName, @Nonnull PrimitiveProperty property, String setterName) {
		PrimitiveType type = property.getType();
		if (type == PrimitiveType.INSTANT || type == PrimitiveType.TEMPORAL_INSTANT) {
			return "            " + varName + "." + setterName + "(value == null ? null : Timestamp.from(value));\n";
		}
		if (type == PrimitiveType.LOCAL_DATE) {
			return "            " + varName + "." + setterName + "(value == null ? null : Date.valueOf(value));\n";
		}
		throw new IllegalStateException("Unexpected type needing conversion: " + type);
	}
	// @formatter:on

	private boolean hasUnboxedMethods(@Nonnull PrimitiveType type) {
		return switch (type) {
			case INTEGER, LONG, DOUBLE, FLOAT, BOOLEAN -> true;
			default -> false;
		};
	}

	private String getUnboxedMethods(@Nonnull Klass klass, @Nonnull PrimitiveProperty property) {
		String className = klass.getName();
		String lowerCamelName = getLowerCamelIdentifier(className);
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, property.getName());
		boolean isBoolean = property.getType() == PrimitiveType.BOOLEAN;
		String getterName = (isBoolean ? "is" : "get") + propNameUpper;
		String setterName = "set" + propNameUpper;
		String primitiveType = this.getPrimitiveTypeName(property.getType());
		String methodSuffix = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, primitiveType);

		// @formatter:off
		return ""
				+ "\n"
				+ "        @Override\n"
				+ "        public " + primitiveType + " get" + methodSuffix + "(@Nonnull " + className + " " + lowerCamelName + ")\n"
				+ "        {\n"
				+ "            return " + lowerCamelName + "." + getterName + "();\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void set" + methodSuffix + "(@Nonnull " + className + " " + lowerCamelName + ", " + primitiveType + " value)\n"
				+ "        {\n"
				+ "            " + lowerCamelName + "." + setterName + "(value);\n"
				+ "        }\n";
		// @formatter:on
	}

	private String getDerivedUnboxedMethods(@Nonnull Klass klass, @Nonnull PrimitiveProperty property) {
		String className = klass.getName();
		String lowerCamelName = getLowerCamelIdentifier(className);
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, property.getName());
		boolean isBoolean = property.getType() == PrimitiveType.BOOLEAN;
		String getterName = (isBoolean ? "is" : "get") + propNameUpper;
		String primitiveType = this.getPrimitiveTypeName(property.getType());
		String methodSuffix = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, primitiveType);

		// @formatter:off
		return ""
				+ "\n"
				+ "        @Override\n"
				+ "        public " + primitiveType + " get" + methodSuffix + "(@Nonnull " + className + " " + lowerCamelName + ")\n"
				+ "        {\n"
				+ "            return " + lowerCamelName + "." + getterName + "();\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void set" + methodSuffix + "(@Nonnull " + className + " " + lowerCamelName + ", " + primitiveType + " value)\n"
				+ "        {\n"
				+ "            throw new UnsupportedOperationException(\"Cannot set derived property: " + property.getName() + "\");\n"
				+ "        }\n";
		// @formatter:on
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
		String className = klass.getName();
		String lowerCamelName = getLowerCamelIdentifier(className);
		String propName = property.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName);
		String lensClass = "Reladomo_" + className + "_" + propName + "_EnumerationPropertyLens";

		// @formatter:off
		// language=JAVA
		return ""
				+ "    private static class " + lensClass + "\n"
				+ "            implements EnumerationLens<" + className + ">\n"
				+ "    {\n"
				+ "        private final cool.klass.model.meta.domain.api.property.EnumerationProperty property;\n"
				+ "\n"
				+ "        " + lensClass + "(cool.klass.model.meta.domain.api.property.EnumerationProperty property)\n"
				+ "        {\n"
				+ "            this.property = Objects.requireNonNull(property);\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        @Nullable\n"
				+ "        public EnumerationLiteral get(@Nonnull " + className + " " + lowerCamelName + ")\n"
				+ "        {\n"
				+ "            String prettyName = " + lowerCamelName + ".get" + propNameUpper + "();\n"
				+ "            if (prettyName == null)\n"
				+ "            {\n"
				+ "                return null;\n"
				+ "            }\n"
				+ "            return this.property.getType().getEnumerationLiterals()\n"
				+ "                    .detect(el -> el.getPrettyName().equals(prettyName));\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void set(@Nonnull " + className + " " + lowerCamelName + ", @Nullable EnumerationLiteral value)\n"
				+ "        {\n"
				+ "            " + lowerCamelName + ".set" + propNameUpper + "(value == null ? null : value.getPrettyName());\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public boolean isNull(@Nonnull " + className + " " + lowerCamelName + ")\n"
				+ "        {\n"
				+ "            return this.get(" + lowerCamelName + ") == null;\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void setNull(@Nonnull " + className + " " + lowerCamelName + ")\n"
				+ "        {\n"
				+ "            this.set(" + lowerCamelName + ", null);\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        @Nonnull\n"
				+ "        public cool.klass.model.meta.domain.api.property.EnumerationProperty getEnumerationProperty()\n"
				+ "        {\n"
				+ "            return this.property;\n"
				+ "        }\n"
				+ "\n"
				+ "        @Nonnull\n"
				+ "        public Attribute getAttribute()\n"
				+ "        {\n"
				+ "            return " + className + "Finder." + propName + "();\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public String toString()\n"
				+ "        {\n"
				+ "            return \"" + lensClass + "\";\n"
				+ "        }\n"
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	private String getAssociationLensClass(@Nonnull Klass klass, @Nonnull AssociationEnd associationEnd) {
		String className = klass.getName();
		String lowerCamelName = getLowerCamelIdentifier(className);
		String propName = associationEnd.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName);
		String lensClass = "Reladomo_" + className + "_" + propName + "_AssociationEndLens";
		String targetType = associationEnd.getType().getName();
		boolean isToMany = associationEnd.getMultiplicity().isToMany();

		String reladomoAssociationLensType = isToMany
			? "ReladomoAssociationLens<" + className + ", ImmutableList<" + targetType + ">>"
			: "ReladomoAssociationLens<" + className + ", " + targetType + ">";
		String interfaceType = isToMany
			? "ToManyLens<" + className + ", " + targetType + ">, " + reladomoAssociationLensType
			: "ToOneLens<" + className + ", " + targetType + ">, " + reladomoAssociationLensType;

		String returnType = isToMany ? "ImmutableList<" + targetType + ">" : targetType;

		String getterBody = isToMany
			? "            return Lists.immutable.withAll(" + lowerCamelName + ".get" + propNameUpper + "());\n"
			: "            return " + lowerCamelName + ".get" + propNameUpper + "();\n";

		String setterBody;
		if (isToMany) {
			// @formatter:off
			// language=JAVA
			setterBody = ""
					+ "            if (value == null)\n"
					+ "            {\n"
					+ "                " + lowerCamelName + ".get" + propNameUpper + "().clear();\n"
					+ "            }\n"
					+ "            else\n"
					+ "            {\n"
					+ "                " + targetType + "List incoming = new " + targetType + "List(value.castToList());\n"
					+ "                " + lowerCamelName + ".get" + propNameUpper + "().merge(incoming, new TopLevelMergeOptions<>(" + targetType + "Finder.getFinderInstance()));\n"
					+ "            }\n";
			// @formatter:on
		} else {
			setterBody = "            " + lowerCamelName + ".set" + propNameUpper + "(value);\n";
		}

		// @formatter:off
		// language=JAVA
		return ""
				+ "    private static class " + lensClass + "\n"
				+ "            implements " + interfaceType + "\n"
				+ "    {\n"
				+ "        private final cool.klass.model.meta.domain.api.property.AssociationEnd property;\n"
				+ "\n"
				+ "        " + lensClass + "(cool.klass.model.meta.domain.api.property.AssociationEnd property)\n"
				+ "        {\n"
				+ "            this.property = Objects.requireNonNull(property);\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        @Nullable\n"
				+ "        public " + returnType + " get(@Nonnull " + className + " " + lowerCamelName + ")\n"
				+ "        {\n"
				+ getterBody
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void set(@Nonnull " + className + " " + lowerCamelName + ", @Nullable " + returnType + " value)\n"
				+ "        {\n"
				+ setterBody
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        @Nonnull\n"
				+ "        public cool.klass.model.meta.domain.api.property.AssociationEnd getAssociationEnd()\n"
				+ "        {\n"
				+ "            return this.property;\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        @Nonnull\n"
				+ "        public AbstractRelatedFinder getRelationshipFinder()\n"
				+ "        {\n"
				+ "            return " + className + "Finder." + propName + "();\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public String toString()\n"
				+ "        {\n"
				+ "            return \"" + lensClass + "\";\n"
				+ "        }\n"
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	private String getInheritedPrimitiveLensClass(
		@Nonnull Klass klass,
		@Nonnull PrimitiveProperty property,
		@Nonnull Klass ancestor,
		@Nonnull String navigation
	) {
		String className = klass.getName();
		String lowerCamelName = getLowerCamelIdentifier(className);
		String ancestorName = ancestor.getName();
		String propName = property.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName);
		String lensClass = "Reladomo_" + className + "_" + propName + "_PrimitivePropertyLens";
		String interfaceType = this.getPrimitiveLensType(property.getType()) + "<" + className + ">";
		String javaType = this.getJavaType(property.getType());
		boolean isBoolean = property.getType() == PrimitiveType.BOOLEAN;
		String getterName = (isBoolean ? "is" : "get") + propNameUpper;
		String setterName = "set" + propNameUpper;
		boolean needsConversion = this.needsTypeConversion(property.getType());

		boolean needsNullCheck = property.isOptional() && this.hasUnboxedMethods(property.getType());

		// @formatter:off
		String getBody;
		if (needsConversion) {
			getBody = this.getInheritedConversionGetterBody(property, getterName);
		} else if (needsNullCheck) {
			getBody = ""
				+ "            if (ancestor.is" + propNameUpper + "Null())\n"
				+ "            {\n"
				+ "                return null;\n"
				+ "            }\n"
				+ "            return ancestor." + getterName + "();\n";
		} else {
			getBody = "            return ancestor." + getterName + "();\n";
		}

		String setBody = needsConversion
			? this.getInheritedConversionSetterBody(property, setterName)
			: "            ancestor." + setterName + "(value);\n";
		// @formatter:on

		String unboxed = this.hasUnboxedMethods(property.getType())
			? this.getInheritedUnboxedMethods(klass, property, ancestor, navigation)
			: "";

		// @formatter:off
		// language=JAVA
		return ""
				+ "    // Inherited from " + ancestorName + "\n"
				+ "    private static class " + lensClass + "\n"
				+ "            implements " + interfaceType + "\n"
				+ "    {\n"
				+ "        private final cool.klass.model.meta.domain.api.property.PrimitiveProperty property;\n"
				+ "\n"
				+ "        " + lensClass + "(cool.klass.model.meta.domain.api.property.PrimitiveProperty property)\n"
				+ "        {\n"
				+ "            this.property = Objects.requireNonNull(property);\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        @Nullable\n"
				+ "        public " + javaType + " get(@Nonnull " + className + " " + lowerCamelName + ")\n"
				+ "        {\n"
				+ "            " + ancestorName + " ancestor = " + navigation + ";\n"
				+ "            if (ancestor == null)\n"
				+ "            {\n"
				+ "                return null;\n"
				+ "            }\n"
				+ getBody
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void set(@Nonnull " + className + " " + lowerCamelName + ", @Nullable " + javaType + " value)\n"
				+ "        {\n"
				+ "            " + ancestorName + " ancestor = " + navigation + ";\n"
				+ "            if (ancestor == null)\n"
				+ "            {\n"
				+ "                return;\n"
				+ "            }\n"
				+ setBody
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public boolean isNull(@Nonnull " + className + " " + lowerCamelName + ")\n"
				+ "        {\n"
				+ (needsNullCheck
					? "            " + ancestorName + " ancestor = " + navigation + ";\n"
					+ "            return ancestor == null || ancestor.is" + propNameUpper + "Null();\n"
					: "            return this.get(" + lowerCamelName + ") == null;\n")
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void setNull(@Nonnull " + className + " " + lowerCamelName + ")\n"
				+ "        {\n"
				+ (needsNullCheck
					? "            " + ancestorName + " ancestor = " + navigation + ";\n"
					+ "            if (ancestor != null)\n"
					+ "            {\n"
					+ "                ancestor.set" + propNameUpper + "Null();\n"
					+ "            }\n"
					: "            this.set(" + lowerCamelName + ", null);\n")
				+ "        }\n"
				+ unboxed
				+ "\n"
				+ "        @Override\n"
				+ "        @Nonnull\n"
				+ "        public cool.klass.model.meta.domain.api.property.PrimitiveProperty getPrimitiveProperty()\n"
				+ "        {\n"
				+ "            return this.property;\n"
				+ "        }\n"
				+ "\n"
				+ "        @Nonnull\n"
				+ "        public Attribute getAttribute()\n"
				+ "        {\n"
				+ "            return " + ancestorName + "Finder." + propName + "();\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public String toString()\n"
				+ "        {\n"
				+ "            return \"" + lensClass + "\";\n"
				+ "        }\n"
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	// @formatter:off
	private String getInheritedConversionGetterBody(@Nonnull PrimitiveProperty property, String getterName) {
		PrimitiveType type = property.getType();
		if (type == PrimitiveType.INSTANT || type == PrimitiveType.TEMPORAL_INSTANT) {
			return ""
				+ "            Timestamp ts = ancestor." + getterName + "();\n"
				+ "            return ts == null ? null : ts.toInstant();\n";
		}
		if (type == PrimitiveType.LOCAL_DATE) {
			return ""
				+ "            java.util.Date date = ancestor." + getterName + "();\n"
				+ "            return date == null ? null : ((java.sql.Date) date).toLocalDate();\n";
		}
		throw new IllegalStateException("Unexpected type needing conversion: " + type);
	}

	private String getInheritedConversionSetterBody(@Nonnull PrimitiveProperty property, String setterName) {
		PrimitiveType type = property.getType();
		if (type == PrimitiveType.INSTANT || type == PrimitiveType.TEMPORAL_INSTANT) {
			return "            ancestor." + setterName + "(value == null ? null : Timestamp.from(value));\n";
		}
		if (type == PrimitiveType.LOCAL_DATE) {
			return "            ancestor." + setterName + "(value == null ? null : Date.valueOf(value));\n";
		}
		throw new IllegalStateException("Unexpected type needing conversion: " + type);
	}
	// @formatter:on

	private String getInheritedUnboxedMethods(
		@Nonnull Klass klass,
		@Nonnull PrimitiveProperty property,
		@Nonnull Klass ancestor,
		@Nonnull String navigation
	) {
		String className = klass.getName();
		String lowerCamelName = getLowerCamelIdentifier(className);
		String ancestorName = ancestor.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, property.getName());
		boolean isBoolean = property.getType() == PrimitiveType.BOOLEAN;
		String getterName = (isBoolean ? "is" : "get") + propNameUpper;
		String setterName = "set" + propNameUpper;
		String primitiveType = this.getPrimitiveTypeName(property.getType());
		String methodSuffix = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, primitiveType);

		// @formatter:off
		// language=JAVA
		return ""
				+ "\n"
				+ "        @Override\n"
				+ "        public " + primitiveType + " get" + methodSuffix + "(@Nonnull " + className + " " + lowerCamelName + ")\n"
				+ "        {\n"
				+ "            " + ancestorName + " ancestor = " + navigation + ";\n"
				+ "            return ancestor." + getterName + "();\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void set" + methodSuffix + "(@Nonnull " + className + " " + lowerCamelName + ", " + primitiveType + " value)\n"
				+ "        {\n"
				+ "            " + ancestorName + " ancestor = " + navigation + ";\n"
				+ "            ancestor." + setterName + "(value);\n"
				+ "        }\n";
		// @formatter:on
	}

	private String getInheritedEnumerationLensClass(
		@Nonnull Klass klass,
		@Nonnull EnumerationProperty property,
		@Nonnull Klass ancestor,
		@Nonnull String navigation
	) {
		String className = klass.getName();
		String lowerCamelName = getLowerCamelIdentifier(className);
		String ancestorName = ancestor.getName();
		String propName = property.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName);
		String lensClass = "Reladomo_" + className + "_" + propName + "_EnumerationPropertyLens";

		// @formatter:off
		// language=JAVA
		return ""
				+ "    // Inherited from " + ancestorName + "\n"
				+ "    private static class " + lensClass + "\n"
				+ "            implements EnumerationLens<" + className + ">\n"
				+ "    {\n"
				+ "        private final cool.klass.model.meta.domain.api.property.EnumerationProperty property;\n"
				+ "\n"
				+ "        " + lensClass + "(cool.klass.model.meta.domain.api.property.EnumerationProperty property)\n"
				+ "        {\n"
				+ "            this.property = Objects.requireNonNull(property);\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        @Nullable\n"
				+ "        public EnumerationLiteral get(@Nonnull " + className + " " + lowerCamelName + ")\n"
				+ "        {\n"
				+ "            " + ancestorName + " ancestor = " + navigation + ";\n"
				+ "            if (ancestor == null)\n"
				+ "            {\n"
				+ "                return null;\n"
				+ "            }\n"
				+ "            String prettyName = ancestor.get" + propNameUpper + "();\n"
				+ "            if (prettyName == null)\n"
				+ "            {\n"
				+ "                return null;\n"
				+ "            }\n"
				+ "            return this.property.getType().getEnumerationLiterals()\n"
				+ "                    .detect(el -> el.getPrettyName().equals(prettyName));\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void set(@Nonnull " + className + " " + lowerCamelName + ", @Nullable EnumerationLiteral value)\n"
				+ "        {\n"
				+ "            " + ancestorName + " ancestor = " + navigation + ";\n"
				+ "            if (ancestor == null)\n"
				+ "            {\n"
				+ "                return;\n"
				+ "            }\n"
				+ "            ancestor.set" + propNameUpper + "(value == null ? null : value.getPrettyName());\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public boolean isNull(@Nonnull " + className + " " + lowerCamelName + ")\n"
				+ "        {\n"
				+ "            return this.get(" + lowerCamelName + ") == null;\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void setNull(@Nonnull " + className + " " + lowerCamelName + ")\n"
				+ "        {\n"
				+ "            this.set(" + lowerCamelName + ", null);\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        @Nonnull\n"
				+ "        public cool.klass.model.meta.domain.api.property.EnumerationProperty getEnumerationProperty()\n"
				+ "        {\n"
				+ "            return this.property;\n"
				+ "        }\n"
				+ "\n"
				+ "        @Nonnull\n"
				+ "        public Attribute getAttribute()\n"
				+ "        {\n"
				+ "            return " + ancestorName + "Finder." + propName + "();\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public String toString()\n"
				+ "        {\n"
				+ "            return \"" + lensClass + "\";\n"
				+ "        }\n"
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	private String getInheritedAssociationLensClass(
		@Nonnull Klass klass,
		@Nonnull AssociationEnd associationEnd,
		@Nonnull Klass ancestor,
		@Nonnull String navigation
	) {
		String className = klass.getName();
		String lowerCamelName = getLowerCamelIdentifier(className);
		String ancestorName = ancestor.getName();
		String propName = associationEnd.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName);
		String lensClass = "Reladomo_" + className + "_" + propName + "_AssociationEndLens";
		String targetType = associationEnd.getType().getName();
		boolean isToMany = associationEnd.getMultiplicity().isToMany();

		String reladomoAssociationLensType = isToMany
			? "ReladomoAssociationLens<" + className + ", ImmutableList<" + targetType + ">>"
			: "ReladomoAssociationLens<" + className + ", " + targetType + ">";
		String interfaceType = isToMany
			? "ToManyLens<" + className + ", " + targetType + ">, " + reladomoAssociationLensType
			: "ToOneLens<" + className + ", " + targetType + ">, " + reladomoAssociationLensType;

		String returnType = isToMany ? "ImmutableList<" + targetType + ">" : targetType;

		String getBody;
		if (isToMany) {
			getBody = "            return Lists.immutable.withAll(ancestor.get" + propNameUpper + "());\n";
		} else {
			getBody = "            return ancestor.get" + propNameUpper + "();\n";
		}

		String setBody;
		if (isToMany) {
			// @formatter:off
			setBody = ""
					+ "            if (value == null)\n"
					+ "            {\n"
					+ "                ancestor.get" + propNameUpper + "().clear();\n"
					+ "            }\n"
					+ "            else\n"
					+ "            {\n"
					+ "                " + targetType + "List incoming = new " + targetType + "List(value.castToList());\n"
					+ "                ancestor.get" + propNameUpper + "().merge(incoming, new TopLevelMergeOptions<>(" + targetType + "Finder.getFinderInstance()));\n"
					+ "            }\n";
			// @formatter:on
		} else {
			setBody = "            ancestor.set" + propNameUpper + "(value);\n";
		}

		// @formatter:off
		// language=JAVA
		return ""
				+ "    // Inherited from " + ancestorName + "\n"
				+ "    private static class " + lensClass + "\n"
				+ "            implements " + interfaceType + "\n"
				+ "    {\n"
				+ "        private final cool.klass.model.meta.domain.api.property.AssociationEnd property;\n"
				+ "\n"
				+ "        " + lensClass + "(cool.klass.model.meta.domain.api.property.AssociationEnd property)\n"
				+ "        {\n"
				+ "            this.property = Objects.requireNonNull(property);\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        @Nullable\n"
				+ "        public " + returnType + " get(@Nonnull " + className + " " + lowerCamelName + ")\n"
				+ "        {\n"
				+ "            " + ancestorName + " ancestor = " + navigation + ";\n"
				+ "            if (ancestor == null)\n"
				+ "            {\n"
				+ "                return null;\n"
				+ "            }\n"
				+ getBody
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void set(@Nonnull " + className + " " + lowerCamelName + ", @Nullable " + returnType + " value)\n"
				+ "        {\n"
				+ "            " + ancestorName + " ancestor = " + navigation + ";\n"
				+ "            if (ancestor == null)\n"
				+ "            {\n"
				+ "                return;\n"
				+ "            }\n"
				+ setBody
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        @Nonnull\n"
				+ "        public cool.klass.model.meta.domain.api.property.AssociationEnd getAssociationEnd()\n"
				+ "        {\n"
				+ "            return this.property;\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        @Nonnull\n"
				+ "        public AbstractRelatedFinder getRelationshipFinder()\n"
				+ "        {\n"
				+ "            return " + ancestorName + "Finder." + propName + "();\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public String toString()\n"
				+ "        {\n"
				+ "            return \"" + lensClass + "\";\n"
				+ "        }\n"
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	private String getFactorySourceCode(
		@Nonnull ImmutableList<Klass> allClasses,
		String packageName,
		String factoryClassName
	) {
		// getAllLenses() body - inline constructor calls
		String allLensesEntries = allClasses
			.collect((klass) ->
				MessageFormat.format(
					"                new Reladomo_{0}_ClassLens(domainModel.getClassByName(\"{0}\"))",
					klass.getName()
				)
			)
			.makeString(",\n");

		// @formatter:off
		// language=JAVA
		return ""
				+ "package " + packageName + ";\n"
				+ "\n"
				+ "import javax.annotation.Nonnull;\n"
				+ "import javax.annotation.processing.Generated;\n"
				+ "\n"
				+ "import cool.klass.model.lens.reladomo.AbstractReladomoLensRegistry;\n"
				+ "import cool.klass.model.lens.reladomo.ReladomoClassLens;\n"
				+ "import cool.klass.model.meta.domain.api.DomainModel;\n"
				+ "\n"
				+ "import org.eclipse.collections.api.list.ImmutableList;\n"
				+ "import org.eclipse.collections.impl.factory.Lists;\n"
				+ "\n"
				+ "/**\n"
				+ " * Auto-generated factory that creates and wires together all Reladomo ClassLenses.\n"
				+ " */\n"
				+ "@Generated(\"" + this.getClass().getCanonicalName() + "\")\n"
				+ "public class " + factoryClassName + "\n"
				+ "        extends AbstractReladomoLensRegistry\n"
				+ "{\n"
				+ "    public " + factoryClassName + "(@Nonnull DomainModel domainModel)\n"
				+ "    {\n"
				+ "        super(domainModel);\n"
				+ "    }\n"
				+ "\n"
				+ "    @Override\n"
				+ "    @Nonnull\n"
				+ "    protected ImmutableList<ReladomoClassLens<?>> getAllLenses(@Nonnull DomainModel domainModel)\n"
				+ "    {\n"
				+ "        return Lists.immutable.with(\n"
				+ allLensesEntries + ");\n"
				+ "    }\n"
				+ "}\n";
		// @formatter:on
	}

	private void printStringToFile(@Nonnull Path path, String contents) throws IOException {
		try (
			PrintStream printStream = new PrintStream(new FileOutputStream(path.toFile()), true, StandardCharsets.UTF_8)
		) {
			printStream.print(contents);
		}
	}

	private record InheritedProperty<T>(T property, Klass ancestor, String navigation) {}
}
