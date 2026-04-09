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

		Set<String> alreadyCollected = new LinkedHashSet<>();
		for (DataTypeProperty p : originalKlass.getDeclaredDataTypeProperties()) {
			alreadyCollected.add(p.getName());
		}
		for (InheritedProperty<DataTypeProperty> inherited : result) {
			alreadyCollected.add(inherited.property().getName());
		}

		// Compute properties introduced at this ancestor level: properties in this class's
		// getDataTypeProperties() that are NOT in its superclass's getDataTypeProperties().
		// This correctly includes interface-inherited properties (e.g. 'name' and 'ordinal'
		// from NamedElement) at the class that first introduces them (PackageableElement),
		// while avoiding attributing them to intermediate classes (Classifier) that inherit
		// them from their superclass chain.
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

	/**
	 * Collects inherited association ends from the superclass chain, paired with their
	 * navigation expressions.
	 */
	private ImmutableList<InheritedProperty<AssociationEnd>> getInheritedAssociationEnds(@Nonnull Klass klass) {
		MutableList<InheritedProperty<AssociationEnd>> result = Lists.mutable.empty();
		this.collectInheritedAssociationEnds(klass, "domainObject", result);
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

			String fileName = "Reladomo" + klass.getName() + "ClassLens.java";
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

		String fileName = "ReladomoLensFactory.java";
		Path outputPath = parentPath.resolve(fileName);

		String sourceCode = this.getFactorySourceCode(concreteClasses, allClasses, packageName);
		this.printStringToFile(outputPath, sourceCode);
	}

	private void writeRegistryFactory(@Nonnull Path path) throws IOException {
		ImmutableList<Klass> concreteClasses = this.domainModel.getClasses().reject(Klass::isAbstract);
		if (concreteClasses.isEmpty()) {
			return;
		}

		String rootPackageName = concreteClasses.getFirst().getPackageName();
		String packageName = rootPackageName + ".lens.reladomo";
		String relativePath = packageName.replaceAll("\\.", "/");
		Path parentPath = path.resolve(relativePath);
		Files.createDirectories(parentPath);

		String fileName = "ReladomoLensRegistryFactory.java";
		Path outputPath = parentPath.resolve(fileName);

		String sourceCode = this.getRegistryFactorySourceCode(packageName, rootPackageName);
		this.printStringToFile(outputPath, sourceCode);
	}

	private String getRegistryFactorySourceCode(String packageName, String rootPackageName) {
		// @formatter:off
		// language=JAVA
		return ""
				+ "package " + packageName + ";\n"
				+ "\n"
				+ "import com.fasterxml.jackson.annotation.JsonTypeName;\n"
				+ "import com.google.auto.service.AutoService;\n"
				+ "import cool.klass.model.lens.LensRegistry;\n"
				+ "import cool.klass.model.lens.LensRegistryFactory;\n"
				+ "import cool.klass.model.meta.domain.api.DomainModel;\n"
				+ "\n"
				+ "/**\n"
				+ " * Auto-generated factory for creating ReladomoLensFactory from config.\n"
				+ " *\n"
				+ " * <p>Generated by {@link " + this.getClass().getCanonicalName() + "}\n"
				+ " */\n"
				+ "@JsonTypeName(\"" + rootPackageName + "\")\n"
				+ "@AutoService(LensRegistryFactory.class)\n"
				+ "public class ReladomoLensRegistryFactory\n"
				+ "        implements LensRegistryFactory\n"
				+ "{\n"
				+ "    @Override\n"
				+ "    public LensRegistry createLensRegistry(@javax.annotation.Nonnull DomainModel domainModel)\n"
				+ "    {\n"
				+ "        return new ReladomoLensFactory(domainModel);\n"
				+ "    }\n"
				+ "}\n";
		// @formatter:on
	}

	public String getClassLensSourceCode(@Nonnull Klass klass) {
		Objects.requireNonNull(klass);

		String packageName = klass.getPackageName() + ".lens.reladomo";
		String className = klass.getName();
		String lensClassName = "Reladomo" + className + "ClassLens";

		// @formatter:off
		// language=JAVA
		String sourceCode = ""
				+ "package " + packageName + ";\n"
				+ "\n"
				+ this.getImports(klass)
				+ "/**\n"
				+ " * Auto-generated Reladomo ClassLens for {@link " + className + "}.\n"
				+ " *\n"
				+ " * <p>Generated by {@link " + this.getClass().getCanonicalName() + "}\n"
				+ " */\n"
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

	private ImmutableList<DataTypeProperty> getInterfaceOnlyProperties(@Nonnull Klass klass) {
		Set<String> declaredNames = new LinkedHashSet<>();
		for (DataTypeProperty p : klass.getDeclaredDataTypeProperties()) {
			declaredNames.add(p.getName());
		}
		for (InheritedProperty<DataTypeProperty> inherited : this.getInheritedDataTypeProperties(klass)) {
			declaredNames.add(inherited.property().getName());
		}

		return klass
			.getDataTypeProperties()
			.reject((p) -> declaredNames.contains(p.getName()))
			.reject(Property::isDerived)
			.reject(this::isTemporalRange);
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
		boolean hasTimestamp = hasInstant; // Reladomo uses Timestamp for Instant/TemporalInstant
		boolean hasDate = hasLocalDate; // Reladomo uses Date for LocalDate
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
		javaxImports.add("javax.annotation.Nonnull");
		javaxImports.add("javax.annotation.Nullable");

		// Lens imports
		lensImports.add("cool.klass.model.lens.AssociationLens");
		lensImports.add("cool.klass.model.lens.ClassLens");
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
			lensImports.add("cool.klass.model.lens.ToOneLens");
			lensImports.add("cool.klass.model.lens.reladomo.ReladomoAssociationLens");
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
		eclipseCollectionsImports.add("org.eclipse.collections.impl.factory.Maps");

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

		if (hasAnyAssociationEnd) {
			reladomoTypeImports.add("com.gs.fw.common.mithra.finder.AbstractRelatedFinder");
		}

		// Association end target type imports - declared
		for (AssociationEnd associationEnd : declaredAssociationEnds) {
			Klass targetType = associationEnd.getType();
			associationTypeImports.add(targetType.getPackageName() + "." + targetType.getName());
			associationTypeImports.add(targetType.getPackageName() + "." + targetType.getName() + "Finder");
			if (associationEnd.getMultiplicity().isToMany()) {
				associationTypeImports.add(targetType.getPackageName() + "." + targetType.getName() + "List");
			}
		}

		// Association end target type imports - inherited
		for (InheritedProperty<AssociationEnd> inherited : inheritedAssociationEnds) {
			Klass targetType = inherited.property().getType();
			associationTypeImports.add(targetType.getPackageName() + "." + targetType.getName());
			associationTypeImports.add(targetType.getPackageName() + "." + targetType.getName() + "Finder");
			if (inherited.property().getMultiplicity().isToMany()) {
				associationTypeImports.add(targetType.getPackageName() + "." + targetType.getName() + "List");
			}
		}

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
		ImmutableList<InheritedProperty<DataTypeProperty>> inheritedDataTypeProperties =
			this.getInheritedDataTypeProperties(klass);
		ImmutableList<InheritedProperty<AssociationEnd>> inheritedAssociationEnds = this.getInheritedAssociationEnds(
			klass
		);
		ImmutableList<DataTypeProperty> interfaceOnlyProperties = this.getInterfaceOnlyProperties(klass);

		// Public typed lens fields - declared data type properties
		String declaredDataTypeFields = klass
			.getDeclaredDataTypeProperties()
			.reject((p) -> !this.shouldIncludeDerived(klass) && p.isDerived())
			.reject(this::isTemporalRange)
			.collect(
				(property) ->
					"    public final " + this.getLensFieldType(klass, property) + " " + property.getName() + ";\n"
			)
			.makeString("");

		// Public typed lens fields - declared association ends
		String declaredAssociationFields = klass
			.getDeclaredAssociationEnds()
			.collect(
				(associationEnd) ->
					"    public final "
					+ this.getAssociationLensFieldType(associationEnd, klass)
					+ " "
					+ associationEnd.getName()
					+ ";\n"
			)
			.makeString("");

		// Public typed lens fields - inherited data type properties
		String inheritedDataTypeFields = inheritedDataTypeProperties
			.collect(
				(inherited) ->
					"    public final "
					+ this.getLensFieldType(klass, inherited.property())
					+ " "
					+ inherited.property().getName()
					+ ";\n"
			)
			.makeString("");

		// Public typed lens fields - inherited association ends
		String inheritedAssociationFields = inheritedAssociationEnds
			.collect(
				(inherited) ->
					"    public final "
					+ this.getAssociationLensFieldType(inherited.property(), klass)
					+ " "
					+ inherited.property().getName()
					+ ";\n"
			)
			.makeString("");

		// Public typed lens fields - interface-only data type properties (direct access)
		String interfaceOnlyFields = interfaceOnlyProperties
			.collect(
				(property) ->
					"    public final " + this.getLensFieldType(klass, property) + " " + property.getName() + ";\n"
			)
			.makeString("");

		String className = klass.getName();

		// @formatter:off
		return ""
				+ "    private final cool.klass.model.meta.domain.api.Klass metaKlass;\n"
				+ "\n"
				+ declaredDataTypeFields
				+ declaredAssociationFields
				+ inheritedDataTypeFields
				+ inheritedAssociationFields
				+ interfaceOnlyFields
				+ "\n"
				+ "    private final ImmutableList<PropertyLens<" + className + ", ?>> allLenses;\n"
				+ "    private final ImmutableMap<cool.klass.model.meta.domain.api.property.PrimitiveProperty, PrimitiveLens<" + className + ", ?>> primitiveLenses;\n"
				+ "    private final ImmutableMap<cool.klass.model.meta.domain.api.property.EnumerationProperty, EnumerationLens<" + className + ">> enumerationLenses;\n"
				+ "    private final ImmutableMap<cool.klass.model.meta.domain.api.property.AssociationEnd, AssociationLens<" + className + ", ?>> associationLenses;\n"
				+ "    private final ImmutableMap<cool.klass.model.meta.domain.api.property.Property, PropertyLens<" + className + ", ?>> allLensesByProperty;\n"
				+ "\n";
		// @formatter:on
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
		ImmutableList<InheritedProperty<DataTypeProperty>> inheritedDataTypeProperties =
			this.getInheritedDataTypeProperties(klass);
		ImmutableList<InheritedProperty<AssociationEnd>> inheritedAssociationEnds = this.getInheritedAssociationEnds(
			klass
		);
		ImmutableList<DataTypeProperty> interfaceOnlyProperties = this.getInterfaceOnlyProperties(klass);

		ImmutableList<DataTypeProperty> declaredDataTypeProperties = klass
			.getDeclaredDataTypeProperties()
			.reject((p) -> !this.shouldIncludeDerived(klass) && p.isDerived())
			.reject(this::isTemporalRange);
		ImmutableList<AssociationEnd> declaredAssociationEnds = klass.getDeclaredAssociationEnds();

		// Initialize lens fields - declared data type properties
		String initDeclaredDataType = declaredDataTypeProperties
			.collect(
				(property) ->
					"        this."
					+ property.getName()
					+ " = new "
					+ this.getLensClassName(klass, property)
					+ "("
					+ this.getPropertyLookup(property)
					+ ");\n"
			)
			.makeString("");

		// Initialize lens fields - declared association ends
		String initDeclaredAssociation = declaredAssociationEnds
			.collect(
				(associationEnd) ->
					"        this."
					+ associationEnd.getName()
					+ " = new "
					+ this.getAssociationLensClassName(klass, associationEnd)
					+ "(klass.getAssociationEndByName(\""
					+ associationEnd.getName()
					+ "\"));\n"
			)
			.makeString("");

		// Initialize lens fields - inherited data type properties
		String initInheritedDataType = inheritedDataTypeProperties
			.collect(
				(inherited) ->
					"        this."
					+ inherited.property().getName()
					+ " = new "
					+ this.getLensClassName(klass, inherited.property())
					+ "("
					+ this.getPropertyLookup(inherited.property())
					+ ");\n"
			)
			.makeString("");

		// Initialize lens fields - inherited association ends
		String initInheritedAssociation = inheritedAssociationEnds
			.collect(
				(inherited) ->
					"        this."
					+ inherited.property().getName()
					+ " = new "
					+ this.getAssociationLensClassName(klass, inherited.property())
					+ "(klass.getAssociationEndByName(\""
					+ inherited.property().getName()
					+ "\"));\n"
			)
			.makeString("");

		// Initialize lens fields - interface-only data type properties (direct access, same as declared)
		String initInterfaceOnly = interfaceOnlyProperties
			.collect(
				(property) ->
					"        this."
					+ property.getName()
					+ " = new "
					+ this.getLensClassName(klass, property)
					+ "("
					+ this.getPropertyLookup(property)
					+ ");\n"
			)
			.makeString("");

		// Initialize allLenses list
		MutableList<String> lensNames = Lists.mutable.empty();
		lensNames.addAllIterable(declaredDataTypeProperties.collect((property) -> "this." + property.getName()));
		lensNames.addAllIterable(
			declaredAssociationEnds.collect((associationEnd) -> "this." + associationEnd.getName())
		);
		lensNames.addAllIterable(
			inheritedDataTypeProperties.collect((inherited) -> "this." + inherited.property().getName())
		);
		lensNames.addAllIterable(interfaceOnlyProperties.collect((property) -> "this." + property.getName()));
		lensNames.addAllIterable(
			inheritedAssociationEnds.collect((inherited) -> "this." + inherited.property().getName())
		);

		String className = klass.getName();

		// Initialize primitiveLenses map
		ImmutableList<PrimitiveProperty> declaredPrimitives = klass
			.getDeclaredDataTypeProperties()
			.selectInstancesOf(PrimitiveProperty.class)
			.reject((p) -> !this.shouldIncludeDerived(klass) && p.isDerived())
			.reject((p) -> p.getType().isTemporalRange());
		MutableList<String> primitiveMapEntries = Lists.mutable.empty();
		primitiveMapEntries.addAllIterable(declaredPrimitives.collect((property) -> getMapEntry(property.getName())));
		primitiveMapEntries.addAllIterable(
			inheritedDataTypeProperties
				.select((inherited) -> inherited.property() instanceof PrimitiveProperty)
				.collect((inherited) -> getMapEntry(inherited.property().getName()))
		);
		primitiveMapEntries.addAllIterable(
			interfaceOnlyProperties
				.selectInstancesOf(PrimitiveProperty.class)
				.collect((property) -> getMapEntry(property.getName()))
		);
		String primitiveLensMap = getMapInit(
			"primitiveLenses",
			"cool.klass.model.meta.domain.api.property.PrimitiveProperty",
			"PrimitiveLens<" + className + ", ?>",
			primitiveMapEntries
		);

		// Initialize enumerationLenses map
		ImmutableList<EnumerationProperty> declaredEnumerations = klass
			.getDeclaredDataTypeProperties()
			.selectInstancesOf(EnumerationProperty.class)
			.reject((p) -> !this.shouldIncludeDerived(klass) && p.isDerived());
		MutableList<String> enumerationMapEntries = Lists.mutable.empty();
		enumerationMapEntries.addAllIterable(
			declaredEnumerations.collect((property) -> getMapEntry(property.getName()))
		);
		enumerationMapEntries.addAllIterable(
			inheritedDataTypeProperties
				.select((inherited) -> inherited.property() instanceof EnumerationProperty)
				.collect((inherited) -> getMapEntry(inherited.property().getName()))
		);
		enumerationMapEntries.addAllIterable(
			interfaceOnlyProperties
				.selectInstancesOf(EnumerationProperty.class)
				.collect((property) -> getMapEntry(property.getName()))
		);
		String enumerationLensMap = getMapInit(
			"enumerationLenses",
			"cool.klass.model.meta.domain.api.property.EnumerationProperty",
			"EnumerationLens<" + className + ">",
			enumerationMapEntries
		);

		// Initialize associationLenses map
		MutableList<String> associationMapEntries = Lists.mutable.empty();
		associationMapEntries.addAllIterable(
			declaredAssociationEnds.collect((associationEnd) -> getMapEntry(associationEnd.getName()))
		);
		associationMapEntries.addAllIterable(
			inheritedAssociationEnds.collect((inherited) -> getMapEntry(inherited.property().getName()))
		);
		String associationLensMap = getMapInit(
			"associationLenses",
			"cool.klass.model.meta.domain.api.property.AssociationEnd",
			"AssociationLens<" + className + ", ?>",
			associationMapEntries
		);

		// Initialize allLensesByProperty map (combined)
		MutableList<String> allMapEntries = Lists.mutable.empty();
		allMapEntries.addAllIterable(declaredDataTypeProperties.collect((property) -> getMapEntry(property.getName())));
		allMapEntries.addAllIterable(
			declaredAssociationEnds.collect((associationEnd) -> getMapEntry(associationEnd.getName()))
		);
		allMapEntries.addAllIterable(
			inheritedDataTypeProperties.collect((inherited) -> getMapEntry(inherited.property().getName()))
		);
		allMapEntries.addAllIterable(
			inheritedAssociationEnds.collect((inherited) -> getMapEntry(inherited.property().getName()))
		);
		allMapEntries.addAllIterable(interfaceOnlyProperties.collect((property) -> getMapEntry(property.getName())));
		String allLensesByPropertyMap = getMapInit(
			"allLensesByProperty",
			"cool.klass.model.meta.domain.api.property.Property",
			"PropertyLens<" + className + ", ?>",
			allMapEntries
		);

		// @formatter:off
		return ""
				+ "    public " + lensClassName + "(@Nonnull cool.klass.model.meta.domain.api.Klass klass)\n"
				+ "    {\n"
				+ "        this.metaKlass = Objects.requireNonNull(klass);\n"
				+ "\n"
				+ initDeclaredDataType
				+ initDeclaredAssociation
				+ initInheritedDataType
				+ initInheritedAssociation
				+ initInterfaceOnly
				+ "\n"
				+ "        this.allLenses = Lists.immutable.with(\n"
				+ "                " + lensNames.makeString(", ") + ");\n"
				+ "\n"
				+ primitiveLensMap
				+ "\n"
				+ enumerationLensMap
				+ "\n"
				+ associationLensMap
				+ "\n"
				+ allLensesByPropertyMap
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	private static String getMapInit(String fieldName, String keyType, String valueType, MutableList<String> entries) {
		if (entries.isEmpty()) {
			return "        this." + fieldName + " = Maps.immutable.<" + keyType + ", " + valueType + ">empty();\n";
		}
		String header = "        this." + fieldName + " = Maps.immutable.<" + keyType + ", " + valueType + ">empty()\n";
		String body = entries.makeString("");
		// Remove the trailing \n from the last entry and add semicolon
		return header + body.substring(0, body.length() - 1) + ";\n";
	}

	private static String getMapEntry(String propertyName) {
		return "                .newWithKeyValue(this." + propertyName + ".getProperty(), this." + propertyName + ")\n";
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
				+ "    public ImmutableList<PropertyLens<" + className + ", ?>> getLenses()\n"
				+ "    {\n"
				+ "        return this.allLenses;\n"
				+ "    }\n"
				+ "\n"
				+ "    @Override\n"
				+ "    @Nonnull\n"
				+ "    public PropertyLens<" + className + ", ?> getLensByProperty(@Nonnull cool.klass.model.meta.domain.api.property.Property property)\n"
				+ "    {\n"
				+ "        PropertyLens<" + className + ", ?> lens = this.allLensesByProperty.get(property);\n"
				+ "        if (lens == null)\n"
				+ "        {\n"
				+ "            throw new IllegalStateException(\"No lens found for property: \" + property.getName());\n"
				+ "        }\n"
				+ "        return lens;\n"
				+ "    }\n"
				+ "\n"
				+ this.getCastOverload(className, "DataTypeLens", "cool.klass.model.meta.domain.api.property.DataTypeProperty", "allLensesByProperty")
				+ this.getDirectOverload("PrimitiveLens<" + className + ", ?>", "cool.klass.model.meta.domain.api.property.PrimitiveProperty", "primitiveLenses")
				+ this.getDirectOverload("EnumerationLens<" + className + ">", "cool.klass.model.meta.domain.api.property.EnumerationProperty", "enumerationLenses")
				+ this.getCastOverload(className, "ReferenceLens", "cool.klass.model.meta.domain.api.property.ReferenceProperty", "allLensesByProperty")
				+ this.getDirectOverload("AssociationLens<" + className + ", ?>", "cool.klass.model.meta.domain.api.property.AssociationEnd", "associationLenses")
				+ "    @Override\n"
				+ "    @Nonnull\n"
				+ "    public ImmutableMap<cool.klass.model.meta.domain.api.property.Property, PropertyLens<" + className + ", ?>> getLensesByProperty()\n"
				+ "    {\n"
				+ "        return this.allLensesByProperty;\n"
				+ "    }\n"
				+ "\n"
				+ "    @Override\n"
				+ "    @Nonnull\n"
				+ "    public com.gs.fw.common.mithra.finder.RelatedFinder<" + className + "> getRelatedFinder()\n"
				+ "    {\n"
				+ "        return " + className + "Finder.getFinderInstance();\n"
				+ "    }\n"
				+ "\n"
				+ this.getInstantiateMethod(klass)
			+ this.getGetJavaClassMethod(klass)
			+ this.getGenerateAndSetIdMethod(klass);
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
					+ "        return new " + className + "(cool.klass.reladomo.utc.infinity.timestamp.UtcInfinityTimestamp.getDefaultInfinity());\n"
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
			// @formatter:off
			return ""
					+ "    @Override\n"
					+ "    public void generateAndSetId(@Nonnull " + className + " instance)\n"
					+ "    {\n"
					+ "    }\n"
					+ "\n";
			// @formatter:on
		}

		PrimitiveProperty idProperty = (PrimitiveProperty) idProperties.getOnly();

		// Non-numeric IDs (e.g., String) are handled by the data store via UUID supplier
		if (!idProperty.getType().isNumeric()) {
			// @formatter:off
			return ""
					+ "    @Override\n"
					+ "    public void generateAndSetId(@Nonnull " + className + " instance)\n"
					+ "    {\n"
					+ "    }\n"
					+ "\n";
			// @formatter:on
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

	private String getCastOverload(String className, String returnLensType, String paramPropertyType, String mapName) {
		// @formatter:off
		return ""
				+ "    @Override\n"
				+ "    @Nonnull\n"
				+ "    public " + returnLensType + "<" + className + ", ?> getLensByProperty(@Nonnull " + paramPropertyType + " property)\n"
				+ "    {\n"
				+ "        PropertyLens<" + className + ", ?> lens = this." + mapName + ".get(property);\n"
				+ "        if (lens == null)\n"
				+ "        {\n"
				+ "            throw new IllegalStateException(\"No lens found for property: \" + property.getName());\n"
				+ "        }\n"
				+ "        return (" + returnLensType + "<" + className + ", ?>) lens;\n"
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
				+ "        " + returnType + " lens = this." + mapName + ".get(property);\n"
				+ "        if (lens == null)\n"
				+ "        {\n"
				+ "            throw new IllegalStateException(\"No lens found for property: \" + property.getName());\n"
				+ "        }\n"
				+ "        return lens;\n"
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	private String getInnerLensClasses(@Nonnull Klass klass) {
		// Inner classes for declared data type properties
		String declaredDataTypeLensClasses = klass
			.getDeclaredDataTypeProperties()
			.reject((p) -> !this.shouldIncludeDerived(klass) && p.isDerived())
			.reject(this::isTemporalRange)
			.collect((property) -> this.getDataTypeLensClass(klass, property))
			.makeString("");

		// Inner classes for declared association ends
		String declaredAssociationLensClasses = klass
			.getDeclaredAssociationEnds()
			.collect((associationEnd) -> this.getAssociationLensClass(klass, associationEnd))
			.makeString("");

		// Inner classes for inherited data type properties (with navigation)
		String inheritedDataTypeLensClasses = this.getInheritedDataTypeProperties(klass)
			.collect((inherited) -> this.getInheritedDataTypeLensClass(klass, inherited))
			.makeString("");

		// Inner classes for inherited association ends (with navigation)
		String inheritedAssociationLensClasses = this.getInheritedAssociationEnds(klass)
			.collect((inherited) ->
				this.getInheritedAssociationLensClass(
					klass,
					inherited.property(),
					inherited.ancestor(),
					inherited.navigation()
				)
			)
			.makeString("");

		// Inner classes for interface-only data type properties (direct access, same as declared)
		String interfaceOnlyLensClasses = this.getInterfaceOnlyProperties(klass)
			.collect((property) -> this.getDataTypeLensClass(klass, property))
			.makeString("");

		return (
			declaredDataTypeLensClasses
			+ declaredAssociationLensClasses
			+ interfaceOnlyLensClasses
			+ inheritedDataTypeLensClasses
			+ inheritedAssociationLensClasses
		);
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
		String propName = property.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName);
		String lensClass = className + propNameUpper + "Lens";
		String interfaceType = this.getPrimitiveLensType(property.getType()) + "<" + className + ">";
		String javaType = this.getJavaType(property.getType());
		boolean isBoolean = property.getType() == PrimitiveType.BOOLEAN;
		String getterName = (isBoolean ? "is" : "get") + propNameUpper;
		String setterName = "set" + propNameUpper;
		boolean needsConversion = this.needsTypeConversion(property.getType());
		// Derived properties don't have Reladomo-generated isXxxNull() methods
		boolean needsNullCheck =
			!property.isDerived() && property.isOptional() && this.hasUnboxedMethods(property.getType());

		String getBody;
		if (property.isDerived() && needsConversion) {
			// Derived properties with type conversion: getter returns the native type directly
			getBody = "            return domainObject." + getterName + "();\n";
		} else if (needsConversion) {
			getBody = this.getConversionGetterBody(property, getterName);
		} else if (needsNullCheck) {
			getBody =
				""
				+ "            if (domainObject.is"
				+ propNameUpper
				+ "Null())\n"
				+ "            {\n"
				+ "                return null;\n"
				+ "            }\n"
				+ "            return domainObject."
				+ getterName
				+ "();\n";
		} else {
			getBody = "            return domainObject." + getterName + "();\n";
		}

		String setBody;
		if (property.isDerived()) {
			setBody =
				"            throw new UnsupportedOperationException(\"Cannot set derived property: "
				+ propName
				+ "\");\n";
		} else if (needsConversion) {
			setBody = this.getConversionSetterBody(property, setterName);
		} else {
			setBody = "            domainObject." + setterName + "(value);\n";
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
				+ "        public " + javaType + " get(@Nonnull " + className + " domainObject)\n"
				+ "        {\n"
				+ getBody
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void set(@Nonnull " + className + " domainObject, @Nullable " + javaType + " value)\n"
				+ "        {\n"
				+ setBody
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public boolean isNull(@Nonnull " + className + " domainObject)\n"
				+ "        {\n"
				+ "            return this.get(domainObject) == null;\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void setNull(@Nonnull " + className + " domainObject)\n"
				+ "        {\n"
				+ "            this.set(domainObject, null);\n"
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

	private String getConversionGetterBody(@Nonnull PrimitiveProperty property, String getterName) {
		PrimitiveType type = property.getType();
		if (type == PrimitiveType.INSTANT || type == PrimitiveType.TEMPORAL_INSTANT) {
			return (
				"            Timestamp ts = domainObject."
				+ getterName
				+ "();\n"
				+ "            return ts == null ? null : ts.toInstant();\n"
			);
		}
		if (type == PrimitiveType.LOCAL_DATE) {
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
		}
		if (type == PrimitiveType.LOCAL_DATE) {
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
		String className = klass.getName();
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
				+ "        public " + primitiveType + " get" + methodSuffix + "(@Nonnull " + className + " domainObject)\n"
				+ "        {\n"
				+ "            return domainObject." + getterName + "();\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void set" + methodSuffix + "(@Nonnull " + className + " domainObject, " + primitiveType + " value)\n"
				+ "        {\n"
				+ "            domainObject." + setterName + "(value);\n"
				+ "        }\n";
		// @formatter:on
	}

	private String getDerivedUnboxedMethods(@Nonnull Klass klass, @Nonnull PrimitiveProperty property) {
		String className = klass.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, property.getName());
		boolean isBoolean = property.getType() == PrimitiveType.BOOLEAN;
		String getterName = (isBoolean ? "is" : "get") + propNameUpper;
		String primitiveType = this.getPrimitiveTypeName(property.getType());
		String methodSuffix = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, primitiveType);

		// @formatter:off
		return ""
				+ "\n"
				+ "        @Override\n"
				+ "        public " + primitiveType + " get" + methodSuffix + "(@Nonnull " + className + " domainObject)\n"
				+ "        {\n"
				+ "            return domainObject." + getterName + "();\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void set" + methodSuffix + "(@Nonnull " + className + " domainObject, " + primitiveType + " value)\n"
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
		String propName = property.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName);
		String lensClass = className + propNameUpper + "Lens";

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
				+ "        public EnumerationLiteral get(@Nonnull " + className + " domainObject)\n"
				+ "        {\n"
				+ "            String prettyName = domainObject.get" + propNameUpper + "();\n"
				+ "            if (prettyName == null)\n"
				+ "            {\n"
				+ "                return null;\n"
				+ "            }\n"
				+ "            return this.property.getType().getEnumerationLiterals()\n"
				+ "                    .detect(el -> el.getPrettyName().equals(prettyName));\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void set(@Nonnull " + className + " domainObject, @Nullable EnumerationLiteral value)\n"
				+ "        {\n"
				+ "            domainObject.set" + propNameUpper + "(value == null ? null : value.getPrettyName());\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public boolean isNull(@Nonnull " + className + " domainObject)\n"
				+ "        {\n"
				+ "            return this.get(domainObject) == null;\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void setNull(@Nonnull " + className + " domainObject)\n"
				+ "        {\n"
				+ "            this.set(domainObject, null);\n"
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
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	private String getAssociationLensClass(@Nonnull Klass klass, @Nonnull AssociationEnd associationEnd) {
		String className = klass.getName();
		String propName = associationEnd.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName);
		String lensClass = className + propNameUpper + "Lens";
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
			? "            return Lists.immutable.withAll(domainObject.get" + propNameUpper + "());\n"
			: "            return domainObject.get" + propNameUpper + "();\n";

		String setterBody;
		if (isToMany) {
			// @formatter:off
			// language=JAVA
			setterBody = ""
					+ "            if (value == null)\n"
					+ "            {\n"
					+ "                domainObject.get" + propNameUpper + "().clear();\n"
					+ "            }\n"
					+ "            else\n"
					+ "            {\n"
					+ "                " + targetType + "List incoming = new " + targetType + "List(value.castToList());\n"
					+ "                domainObject.get" + propNameUpper + "().merge(incoming, new TopLevelMergeOptions<>(" + targetType + "Finder.getFinderInstance()));\n"
					+ "            }\n";
			// @formatter:on
		} else {
			setterBody = "            domainObject.set" + propNameUpper + "(value);\n";
		}

		return (
			// language=JAVA
			""
			+ "    private static class "
			+ lensClass
			+ "\n"
			+ "            implements "
			+ interfaceType
			+ "\n"
			+ "    {\n"
			+ "        private final cool.klass.model.meta.domain.api.property.AssociationEnd property;\n"
			+ "\n"
			+ "        "
			+ lensClass
			+ "(cool.klass.model.meta.domain.api.property.AssociationEnd property)\n"
			+ "        {\n"
			+ "            this.property = Objects.requireNonNull(property);\n"
			+ "        }\n"
			+ "\n"
			+ "        @Override\n"
			+ "        @Nullable\n"
			+ "        public "
			+ returnType
			+ " get(@Nonnull "
			+ className
			+ " domainObject)\n"
			+ "        {\n"
			+ getterBody
			+ "        }\n"
			+ "\n"
			+ "        @Override\n"
			+ "        public void set(@Nonnull "
			+ className
			+ " domainObject, @Nullable "
			+ returnType
			+ " value)\n"
			+ "        {\n"
			+ setterBody
			+ "        }\n"
			+ "\n"
			+ "        @Override\n"
			+ "        public boolean isNull(@Nonnull "
			+ className
			+ " domainObject)\n"
			+ "        {\n"
			+ "            return this.get(domainObject) == null;\n"
			+ "        }\n"
			+ "\n"
			+ "        @Override\n"
			+ "        public void setNull(@Nonnull "
			+ className
			+ " domainObject)\n"
			+ "        {\n"
			+ "            this.set(domainObject, null);\n"
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
			+ "            return "
			+ className
			+ "Finder."
			+ propName
			+ "();\n"
			+ "        }\n"
			+ "    }\n"
			+ "\n"
		);
	}

	private String getInheritedPrimitiveLensClass(
		@Nonnull Klass klass,
		@Nonnull PrimitiveProperty property,
		@Nonnull Klass ancestor,
		@Nonnull String navigation
	) {
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

		boolean needsNullCheck = property.isOptional() && this.hasUnboxedMethods(property.getType());

		String getBody;
		if (needsConversion) {
			getBody = this.getInheritedConversionGetterBody(property, getterName);
		} else if (needsNullCheck) {
			getBody =
				""
				+ "            if (ancestor.is"
				+ propNameUpper
				+ "Null())\n"
				+ "            {\n"
				+ "                return null;\n"
				+ "            }\n"
				+ "            return ancestor."
				+ getterName
				+ "();\n";
		} else {
			getBody = "            return ancestor." + getterName + "();\n";
		}

		String setBody = needsConversion
			? this.getInheritedConversionSetterBody(property, setterName)
			: "            ancestor." + setterName + "(value);\n";

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
				+ "        public " + javaType + " get(@Nonnull " + className + " domainObject)\n"
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
				+ "        public void set(@Nonnull " + className + " domainObject, @Nullable " + javaType + " value)\n"
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
				+ "        public boolean isNull(@Nonnull " + className + " domainObject)\n"
				+ "        {\n"
				+ "            return this.get(domainObject) == null;\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void setNull(@Nonnull " + className + " domainObject)\n"
				+ "        {\n"
				+ "            this.set(domainObject, null);\n"
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
				+ "    }\n"
				+ "\n";
		// @formatter:on
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
		}
		if (type == PrimitiveType.LOCAL_DATE) {
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
		}
		if (type == PrimitiveType.LOCAL_DATE) {
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
		String className = klass.getName();
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
				+ "        public " + primitiveType + " get" + methodSuffix + "(@Nonnull " + className + " domainObject)\n"
				+ "        {\n"
				+ "            " + ancestorName + " ancestor = " + navigation + ";\n"
				+ "            return ancestor." + getterName + "();\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void set" + methodSuffix + "(@Nonnull " + className + " domainObject, " + primitiveType + " value)\n"
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
		String ancestorName = ancestor.getName();
		String propName = property.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName);
		String lensClass = className + propNameUpper + "Lens";

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
				+ "        public EnumerationLiteral get(@Nonnull " + className + " domainObject)\n"
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
				+ "        public void set(@Nonnull " + className + " domainObject, @Nullable EnumerationLiteral value)\n"
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
				+ "        public boolean isNull(@Nonnull " + className + " domainObject)\n"
				+ "        {\n"
				+ "            return this.get(domainObject) == null;\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void setNull(@Nonnull " + className + " domainObject)\n"
				+ "        {\n"
				+ "            this.set(domainObject, null);\n"
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
		String ancestorName = ancestor.getName();
		String propName = associationEnd.getName();
		String propNameUpper = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName);
		String lensClass = className + propNameUpper + "Lens";
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
				+ "        public " + returnType + " get(@Nonnull " + className + " domainObject)\n"
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
				+ "        public void set(@Nonnull " + className + " domainObject, @Nullable " + returnType + " value)\n"
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
				+ "        public boolean isNull(@Nonnull " + className + " domainObject)\n"
				+ "        {\n"
				+ "            return this.get(domainObject) == null;\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public void setNull(@Nonnull " + className + " domainObject)\n"
				+ "        {\n"
				+ "            this.set(domainObject, null);\n"
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
				+ "    }\n"
				+ "\n";
		// @formatter:on
	}

	private String getFactorySourceCode(
		@Nonnull ImmutableList<Klass> concreteClasses,
		@Nonnull ImmutableList<Klass> allClasses,
		String packageName
	) {
		// Fields for each lens (all classes including abstract)
		String lensFields = allClasses
			.collect(
				(klass) ->
					"    private final Reladomo"
					+ klass.getName()
					+ "ClassLens "
					+ CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, klass.getName())
					+ "Lens;\n"
			)
			.makeString("");

		// Constructor body - initialize lens fields
		String lensFieldInits = allClasses
			.collect((klass) -> {
				String fieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, klass.getName()) + "Lens";
				return (
					"        this."
					+ fieldName
					+ " = new Reladomo"
					+ klass.getName()
					+ "ClassLens(domainModel.getClassByName(\""
					+ klass.getName()
					+ "\"));\n"
				);
			})
			.makeString("");

		// Map entries for lensesByKlass (all classes including abstract)
		MutableList<String> mapEntries = allClasses
			.collect((klass) -> {
				String fieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, klass.getName()) + "Lens";
				return "                .newWithKeyValue(this." + fieldName + ".getKlass(), this." + fieldName + ")\n";
			})
			.toList();
		String lensesByKlassMap =
			"        this.lensesByKlass = Maps.immutable.<Klass, ClassLens<?>>empty()\n" + mapEntries.makeString("");
		lensesByKlassMap = lensesByKlassMap.substring(0, lensesByKlassMap.length() - 1) + ";\n";

		// Map entries for klassByJavaClass (reverse lookup, all classes) — use FQN to avoid name conflicts
		MutableList<String> klassByJavaClassEntries = allClasses
			.collect(
				(klass) ->
					"                .newWithKeyValue("
					+ klass.getFullyQualifiedName()
					+ ".class, domainModel.getClassByName(\""
					+ klass.getName()
					+ "\"))\n"
			)
			.toList();
		String klassByJavaClassMap =
			"        this.klassByJavaClass = Maps.immutable.<Class<?>, Klass>empty()\n"
			+ klassByJavaClassEntries.makeString("");
		klassByJavaClassMap = klassByJavaClassMap.substring(0, klassByJavaClassMap.length() - 1) + ";\n";

		// Map entries for relatedFindersByKlass (ALL classes including abstract) — use FQN to avoid name conflicts
		MutableList<String> finderEntries = allClasses
			.collect(
				(klass) ->
					"                .newWithKeyValue(domainModel.getClassByName(\""
					+ klass.getName()
					+ "\"), "
					+ klass.getFullyQualifiedName()
					+ "Finder.getFinderInstance())\n"
			)
			.toList();
		String relatedFindersByKlassMap =
			"        this.relatedFindersByKlass = Maps.immutable.<Klass, com.gs.fw.common.mithra.finder.RelatedFinder<?>>empty()\n"
			+ finderEntries.makeString("");
		relatedFindersByKlassMap = relatedFindersByKlassMap.substring(0, relatedFindersByKlassMap.length() - 1) + ";\n";

		// Getter methods for each lens
		String getterMethods = concreteClasses
			.collect((klass) -> {
				String fieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, klass.getName()) + "Lens";
				return (
					""
					+ "    @Nonnull\n"
					+ "    public Reladomo"
					+ klass.getName()
					+ "ClassLens get"
					+ klass.getName()
					+ "Lens()\n"
					+ "    {\n"
					+ "        return this."
					+ fieldName
					+ ";\n"
					+ "    }\n"
					+ "\n"
				);
			})
			.makeString("");

		// @formatter:off
		// language=JAVA
		return ""
				+ "package " + packageName + ";\n"
				+ "\n"
				+ "import java.util.Objects;\n"
				+ "\n"
				+ "import javax.annotation.Nonnull;\n"
				+ "\n"
				+ "import cool.klass.model.lens.ClassLens;\n"
				+ "import cool.klass.model.lens.reladomo.ReladomoLensRegistry;\n"
				+ "import cool.klass.model.meta.domain.api.DomainModel;\n"
				+ "import cool.klass.model.meta.domain.api.Klass;\n"
				+ "\n"
				+ "import org.eclipse.collections.api.map.ImmutableMap;\n"
				+ "import org.eclipse.collections.impl.factory.Maps;\n"
				+ "\n"
				+ "/**\n"
				+ " * Auto-generated factory that creates and wires together all Reladomo ClassLenses.\n"
				+ " *\n"
				+ " * <p>Generated by {@link " + this.getClass().getCanonicalName() + "}\n"
				+ " */\n"
				+ "public class ReladomoLensFactory\n"
				+ "        implements ReladomoLensRegistry\n"
				+ "{\n"
				+ lensFields
				+ "\n"
				+ "    private final ImmutableMap<Klass, ClassLens<?>> lensesByKlass;\n"
				+ "    private final ImmutableMap<Class<?>, Klass> klassByJavaClass;\n"
				+ "    private final ImmutableMap<Klass, com.gs.fw.common.mithra.finder.RelatedFinder<?>> relatedFindersByKlass;\n"
				+ "\n"
				+ "    public ReladomoLensFactory(@Nonnull DomainModel domainModel)\n"
				+ "    {\n"
				+ lensFieldInits
				+ "\n"
				+ lensesByKlassMap
				+ "\n"
				+ klassByJavaClassMap
				+ "\n"
				+ relatedFindersByKlassMap
				+ "    }\n"
				+ "\n"
				+ getterMethods
				+ "    @Override\n"
				+ "    public boolean hasClassLens(@Nonnull Klass klass)\n"
				+ "    {\n"
				+ "        return this.lensesByKlass.containsKey(klass);\n"
				+ "    }\n"
				+ "\n"
				+ "    @Override\n"
				+ "    @Nonnull\n"
				+ "    public ClassLens<?> getClassLens(@Nonnull Klass klass)\n"
				+ "    {\n"
				+ "        Objects.requireNonNull(klass);\n"
				+ "        ClassLens<?> lens = this.lensesByKlass.get(klass);\n"
				+ "        if (lens == null)\n"
				+ "        {\n"
				+ "            throw new IllegalStateException(\"No ClassLens registered for Klass: \" + klass.getName());\n"
				+ "        }\n"
				+ "        return lens;\n"
				+ "    }\n"
				+ "\n"
				+ "    @Override\n"
				+ "    public boolean hasKlassForJavaClass(@Nonnull Class<?> javaClass)\n"
				+ "    {\n"
				+ "        return this.klassByJavaClass.containsKey(javaClass);\n"
				+ "    }\n"
				+ "\n"
				+ "    @Override\n"
				+ "    @Nonnull\n"
				+ "    public Klass getKlassForJavaClass(@Nonnull Class<?> javaClass)\n"
				+ "    {\n"
				+ "        Objects.requireNonNull(javaClass);\n"
				+ "        Klass klass = this.klassByJavaClass.get(javaClass);\n"
				+ "        if (klass == null)\n"
				+ "        {\n"
				+ "            throw new IllegalStateException(\"No Klass registered for Java class: \" + javaClass.getName());\n"
				+ "        }\n"
				+ "        return klass;\n"
				+ "    }\n"
				+ "\n"
				+ "    @Override\n"
				+ "    public boolean hasRelatedFinderForKlass(@Nonnull Klass klass)\n"
				+ "    {\n"
				+ "        return this.relatedFindersByKlass.containsKey(klass);\n"
				+ "    }\n"
				+ "\n"
				+ "    @Override\n"
				+ "    @Nonnull\n"
				+ "    public com.gs.fw.common.mithra.finder.RelatedFinder<?> getRelatedFinderForKlass(@Nonnull Klass klass)\n"
				+ "    {\n"
				+ "        Objects.requireNonNull(klass);\n"
				+ "        com.gs.fw.common.mithra.finder.RelatedFinder<?> finder = this.relatedFindersByKlass.get(klass);\n"
				+ "        if (finder == null)\n"
				+ "        {\n"
				+ "            throw new IllegalStateException(\"No RelatedFinder registered for Klass: \" + klass.getName());\n"
				+ "        }\n"
				+ "        return finder;\n"
				+ "    }\n"
				+ "\n"
				+ "    @Nonnull\n"
				+ "    public ImmutableMap<Klass, ClassLens<?>> getLensesByKlass()\n"
				+ "    {\n"
				+ "        return this.lensesByKlass;\n"
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
}
