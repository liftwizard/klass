/*
 * Copyright 2024 Craig Motlin
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

package cool.klass.generator.meta.constants;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import cool.klass.model.meta.domain.api.Association;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.Enumeration;
import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.Interface;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.NamedElement;
import cool.klass.model.meta.domain.api.PackageableElement;
import cool.klass.model.meta.domain.api.TopLevelElement;
import cool.klass.model.meta.domain.api.modifier.Modifier;
import cool.klass.model.meta.domain.api.modifier.ModifierOwner;
import cool.klass.model.meta.domain.api.projection.Projection;
import cool.klass.model.meta.domain.api.projection.ProjectionDataTypeProperty;
import cool.klass.model.meta.domain.api.projection.ProjectionElement;
import cool.klass.model.meta.domain.api.projection.ProjectionParent;
import cool.klass.model.meta.domain.api.projection.ProjectionProjectionReference;
import cool.klass.model.meta.domain.api.projection.ProjectionReferenceProperty;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import cool.klass.model.meta.domain.api.service.ServiceGroup;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.Pair;

public class JavaConstantsMetaModelGenerator {

    private static final Converter<String, String> TO_CONSTANT_CASE = CaseFormat.LOWER_CAMEL.converterTo(
        CaseFormat.UPPER_UNDERSCORE
    );

    @Nonnull
    private final DomainModel domainModel;

    @Nonnull
    private final String applicationName;

    @Nonnull
    private final String rootPackageName;

    public JavaConstantsMetaModelGenerator(
        @Nonnull DomainModel domainModel,
        @Nonnull String applicationName,
        @Nonnull String rootPackageName
    ) {
        this.domainModel = Objects.requireNonNull(domainModel);
        this.applicationName = Objects.requireNonNull(applicationName);
        this.rootPackageName = Objects.requireNonNull(rootPackageName);
    }

    public void writeJavaConstantsMetaModelFiles(@Nonnull Path outputPath) throws IOException {
        Path domainModelOutputPath = this.getOutputPath(outputPath);
        this.printStringToFile(domainModelOutputPath, this.getDomainModelSourceCode());

        for (Enumeration enumeration : this.domainModel.getEnumerations()) {
            Path path = this.getOutputPath(outputPath, enumeration);
            String enumerationSourceCode = this.getEnumerationSourceCode(enumeration);
            this.printStringToFile(path, enumerationSourceCode);
        }

        for (Interface eachInterface : this.domainModel.getInterfaces()) {
            Path path = this.getOutputPath(outputPath, eachInterface);
            String classSourceCode = this.getInterfaceSourceCode(eachInterface);
            this.printStringToFile(path, classSourceCode);
        }

        for (Klass klass : this.domainModel.getClasses()) {
            Path path = this.getOutputPath(outputPath, klass);
            String classSourceCode = this.getClassSourceCode(klass);
            this.printStringToFile(path, classSourceCode);
        }

        for (Association association : this.domainModel.getAssociations()) {
            Path path = this.getOutputPath(outputPath, association);
            String associationSourceCode = this.getAssociationSourceCode(association);
            this.printStringToFile(path, associationSourceCode);
        }

        for (Projection projection : this.domainModel.getProjections()) {
            Path path = this.getOutputPath(outputPath, projection);
            String projectionSourceCode = this.getProjectionSourceCode(projection);
            this.printStringToFile(path, projectionSourceCode);
        }
    }

    @Nonnull
    private String getDomainModelSourceCode() {
        String imports =
            this.domainModel.getTopLevelElements()
                .collect(TopLevelElement::getPackageName)
                .distinct()
                .toSortedList()
                .collect(packageName -> "import " + packageName + ".meta.constants.*;\n")
                .makeString("");

        // @formatter:off
        // language=JAVA
        return ""
                + "package " + this.rootPackageName + ".meta.constants;\n"
                + "\n"
                + "import javax.annotation.*;\n"
                + "\n"
                + "import cool.klass.model.meta.domain.api.*;\n"
                + "import cool.klass.model.meta.domain.api.order.*;\n"
                + "import cool.klass.model.meta.domain.api.modifier.*;\n"
                + "import cool.klass.model.meta.domain.api.projection.*;\n"
                + "import cool.klass.model.meta.domain.api.service.*;\n"
                + "import org.eclipse.collections.api.list.*;\n"
                + "import org.eclipse.collections.api.multimap.list.*;\n"
                + "\n"
                + imports
                + "/**\n"
                + " * Auto-generated by {@link " + this.getClass().getCanonicalName() + "}\n"
                + " */\n"
                + "public enum " + this.applicationName + "DomainModel implements DomainModel\n"
                + "{\n"
                + "    INSTANCE;\n"
                + "\n"
                + this.getTopLevelElementsSourceCode()
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public ImmutableList<TopLevelElement> getTopLevelElements()\n"
                + "    {\n"
                + "        throw new UnsupportedOperationException(this.getClass().getSimpleName() + \".getTopLevelElements() not implemented yet\");\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public ImmutableList<Enumeration> getEnumerations()\n"
                + "    {\n"
                + "        throw new UnsupportedOperationException(this.getClass().getSimpleName() + \".getEnumerations() not implemented yet\");\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public ImmutableList<Classifier> getClassifiers()\n"
                + "    {\n"
                + "        throw new UnsupportedOperationException(this.getClass().getSimpleName()\n"
                + "                + \".getClassifiers() not implemented yet\");\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public ImmutableList<Interface> getInterfaces()\n"
                + "    {\n"
                + "        throw new UnsupportedOperationException(this.getClass().getSimpleName() + \".getInterfaces() not implemented yet\");\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public ImmutableList<Klass> getClasses()\n"
                + "    {\n"
                + "        throw new UnsupportedOperationException(this.getClass().getSimpleName() + \".getClasses() not implemented yet\");\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public ImmutableList<Association> getAssociations()\n"
                + "    {\n"
                + "        throw new UnsupportedOperationException(this.getClass().getSimpleName() + \".getAssociations() not implemented yet\");\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public ImmutableList<Projection> getProjections()\n"
                + "    {\n"
                + "        throw new UnsupportedOperationException(this.getClass().getSimpleName() + \".getProjections() not implemented yet\");\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public ImmutableList<ServiceGroup> getServiceGroups()\n"
                + "    {\n"
                + "        throw new UnsupportedOperationException(this.getClass().getSimpleName() + \".getServiceGroups() not implemented yet\");\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Enumeration getEnumerationByName(String name)\n"
                + "    {\n"
                + "        throw new UnsupportedOperationException(this.getClass().getSimpleName() + \".getEnumerationByName() not implemented yet\");\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Classifier getClassifierByName(String name)\n"
                + "    {\n"
                + "        throw new UnsupportedOperationException(this.getClass().getSimpleName()\n"
                + "                + \".getClassifierByName() not implemented yet\");\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Interface getInterfaceByName(String name)\n"
                + "    {\n"
                + "        throw new UnsupportedOperationException(this.getClass().getSimpleName() + \".getInterfaceByName() not implemented yet\");\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Klass getClassByName(String name)\n"
                + "    {\n"
                + "        throw new UnsupportedOperationException(this.getClass().getSimpleName() + \".getClassByName() not implemented yet\");\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Association getAssociationByName(String name)\n"
                + "    {\n"
                + "        throw new UnsupportedOperationException(this.getClass().getSimpleName() + \".getAssociationByName() not implemented yet\");\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Projection getProjectionByName(String name)\n"
                + "    {\n"
                + "        throw new UnsupportedOperationException(this.getClass().getSimpleName() + \".getProjectionByName() not implemented yet\");\n"
                + "    }\n"
                + "}\n";
        // @formatter:on
    }

    @Nonnull
    private String getTopLevelElementsSourceCode() {
        return this.domainModel.getTopLevelElements().collect(this::getTopLevelElementSourceCode).makeString("");
    }

    @Nonnull
    private String getTopLevelElementSourceCode(TopLevelElement topLevelElement) {
        if (topLevelElement instanceof ServiceGroup) {
            // TODO: ServiceGroup code generation
            return "";
        }
        return MessageFormat.format(
            "    public static final {1}_{0} {1} = {1}_{0}.INSTANCE;\n",
            this.getTypeName(topLevelElement),
            topLevelElement.getName()
        );
    }

    @Nonnull
    private String getTypeName(Element element) {
        if (element instanceof Enumeration) {
            return Enumeration.class.getSimpleName();
        }

        if (element instanceof Interface) {
            return Interface.class.getSimpleName();
        }

        if (element instanceof Klass) {
            return Klass.class.getSimpleName();
        }

        if (element instanceof Association) {
            return Association.class.getSimpleName();
        }

        if (element instanceof Projection) {
            return Projection.class.getSimpleName();
        }

        if (element instanceof ServiceGroup) {
            return ServiceGroup.class.getSimpleName();
        }

        if (element instanceof PrimitiveProperty) {
            return PrimitiveProperty.class.getSimpleName();
        }

        if (element instanceof EnumerationProperty) {
            return EnumerationProperty.class.getSimpleName();
        }

        if (element instanceof AssociationEnd) {
            return AssociationEnd.class.getSimpleName();
        }

        if (element instanceof ProjectionDataTypeProperty) {
            return ProjectionDataTypeProperty.class.getSimpleName();
        }

        if (element instanceof ProjectionReferenceProperty) {
            return ProjectionReferenceProperty.class.getSimpleName();
        }

        if (element instanceof ProjectionProjectionReference) {
            return ProjectionProjectionReference.class.getSimpleName();
        }

        if (element instanceof EnumerationLiteral) {
            return EnumerationLiteral.class.getSimpleName();
        }

        throw new AssertionError(element.getClass().getSimpleName());
    }

    @Nonnull
    public Path getOutputPath(@Nonnull Path outputPath) {
        String rootPackageRelativePath = this.rootPackageName.replaceAll("\\.", "/");

        Path directory = outputPath.resolve(rootPackageRelativePath).resolve("meta").resolve("constants");
        directory.toFile().mkdirs();
        return directory.resolve(this.applicationName + "DomainModel.java");
    }

    @Nonnull
    public Path getOutputPath(@Nonnull Path outputPath, @Nonnull PackageableElement packageableElement) {
        String packageRelativePath = packageableElement.getPackageName().replaceAll("\\.", "/");
        Path directory = outputPath.resolve(packageRelativePath).resolve("meta").resolve("constants");
        directory.toFile().mkdirs();
        String fileName = packageableElement.getName() + "_" + this.getTypeName(packageableElement) + ".java";
        return directory.resolve(fileName);
    }

    @Nonnull
    public Path getOutputPath(@Nonnull Path outputPath, @Nonnull Projection projection) {
        String packageRelativePath = projection.getPackageName().replaceAll("\\.", "/");
        Path directory = outputPath.resolve(packageRelativePath).resolve("meta").resolve("constants");
        directory.toFile().mkdirs();
        String fileName = projection.getName() + "_" + this.getTypeName(projection) + ".java";
        return directory.resolve(fileName);
    }

    private void printStringToFile(@Nonnull Path path, String contents) throws FileNotFoundException {
        try (
            PrintStream printStream = new PrintStream(new FileOutputStream(path.toFile()), true, StandardCharsets.UTF_8)
        ) {
            printStream.print(contents);
        }
    }

    @Nonnull
    private String getEnumerationSourceCode(@Nonnull Enumeration enumeration) {
        String packageName = enumeration.getPackageName() + ".meta.constants";

        // @formatter:off
        // language=JAVA
        return ""
                + "package " + packageName + ";\n"
                + "\n"
                + "import java.util.Optional;\n"
                + "import java.util.LinkedHashMap;\n"
                + "\n"
                + "import javax.annotation.*;\n"
                + "\n"
                + "import cool.klass.model.meta.domain.api.*;\n"
                + "import cool.klass.model.meta.domain.api.order.*;\n"
                + "import cool.klass.model.meta.domain.api.modifier.*;\n"
                + "import cool.klass.model.meta.domain.api.property.*;\n"
                + "import cool.klass.model.meta.domain.api.property.validation.*;\n"
                + "import cool.klass.model.meta.domain.api.projection.*;\n"
                + "import cool.klass.model.meta.domain.api.source.*;\n"
                + "\n"
                + "import " + this.rootPackageName + ".meta.constants." + this.applicationName + "DomainModel;\n"
                + "\n"
                + "import org.eclipse.collections.api.list.*;\n"
                + "import org.eclipse.collections.api.multimap.list.*;\n"
                + "import org.eclipse.collections.impl.factory.*;\n"
                + "\n"
                + "/**\n"
                + " * Auto-generated by {@link " + this.getClass().getCanonicalName() + "}\n"
                + " */\n"
                + "public enum " + enumeration.getName() + "_" + "Enumeration implements Enumeration\n"
                + "{\n"
                + "    INSTANCE;\n"
                + "\n"
                + this.getEnumerationLiteralConstantsSourceCode(enumeration)
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public String getPackageName()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(enumeration.getPackageName()) + "\";\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public String getName()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(enumeration.getName()) + "\";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public int getOrdinal()\n"
                + "    {\n"
                + "        return " + enumeration.getOrdinal() + ";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Optional<Element> getMacroElement()\n"
                + "    {\n"
                + "        return Optional.empty();\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public ImmutableList<EnumerationLiteral> getEnumerationLiterals()\n"
                + "    {\n"
                + "        return Lists.immutable.with(" + enumeration.getEnumerationLiterals().collect(NamedElement::getName).makeString() + ");\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public String toString()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(enumeration.toString()) + "\";\n"
                + "    }\n"
                + "\n"
                + this.getEnumerationLiteralsSourceCode(enumeration)
                + "}\n";
        // @formatter:on
    }

    @Nonnull
    private String getEnumerationLiteralsSourceCode(@Nonnull Enumeration enumeration) {
        return enumeration
            .getEnumerationLiterals()
            .collect(this::getEnumerationLiteralSourceCode)
            .makeString("\n")
            // https://stackoverflow.com/questions/15888934/how-to-indent-a-multi-line-paragraph-being-written-to-the-console-in-java
            // https://stackoverflow.com/a/15889069
            // https://stackoverflow.com/questions/11125459/java-regex-negative-lookahead
            .replaceAll("(?m)^(?!$)", "    ");
    }

    @Nonnull
    private String getEnumerationLiteralSourceCode(@Nonnull EnumerationLiteral enumerationLiteral) {
        String uppercaseName = this.getUppercaseName(enumerationLiteral);

        String declaredPrettyName = enumerationLiteral
            .getDeclaredPrettyName()
            .map(StringEscapeUtils::escapeJava)
            .map(string -> String.format("of(\"%s\")", string))
            .orElse("empty()");

        // @formatter:off
        // language=JAVA
        return ""
                + "public static enum " + uppercaseName + "_EnumerationLiteral implements EnumerationLiteral\n"
                + "{\n"
                + "    INSTANCE;\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public String getName()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(enumerationLiteral.getName()) + "\";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public int getOrdinal()\n"
                + "    {\n"
                + "        return " + enumerationLiteral.getOrdinal() + ";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Optional<Element> getMacroElement()\n"
                + "    {\n"
                + "        return Optional.empty();\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public Optional<String> getDeclaredPrettyName()\n"
                + "    {\n"
                + "        return Optional." + declaredPrettyName + ";\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public Enumeration getType()\n"
                + "    {\n"
                + "        return " + this.applicationName + "DomainModel." + enumerationLiteral.getType().getName() + ";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public String toString()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(enumerationLiteral.toString()) + "\";\n"
                + "    }\n"
                + "}\n";
        // @formatter:on
    }

    private String getEnumerationLiteralConstantsSourceCode(@Nonnull Enumeration enumeration) {
        return enumeration
            .getEnumerationLiterals()
            .collect(this::getEnumerationLiteralConstantSourceCode)
            .makeString("");
    }

    @Nonnull
    private String getEnumerationLiteralConstantSourceCode(@Nonnull EnumerationLiteral enumerationLiteral) {
        String name = enumerationLiteral.getName();
        String uppercaseName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name);
        String type = this.getTypeName(enumerationLiteral);

        return MessageFormat.format(
            "    public static final {0}_{1} {2} = {0}_{1}.INSTANCE;\n",
            uppercaseName,
            type,
            name
        );
    }

    @Nonnull
    private String getInterfaceSourceCode(@Nonnull Interface eachInterface) {
        // @formatter:off
        // language=JAVA
        return ""
                + "package " + eachInterface.getPackageName() + ".meta.constants;\n"
                + "\n"
                + "import java.util.Optional;\n"
                + "import java.util.LinkedHashMap;\n"
                + "\n"
                + "import javax.annotation.*;\n"
                + "\n"
                + "import cool.klass.model.meta.domain.api.*;\n"
                + "import cool.klass.model.meta.domain.api.order.*;\n"
                + "import cool.klass.model.meta.domain.api.modifier.*;\n"
                + "import cool.klass.model.meta.domain.api.property.*;\n"
                + "import cool.klass.model.meta.domain.api.property.validation.*;\n"
                + "import cool.klass.model.meta.domain.api.source.*;\n"
                + "\n"
                + "import " + this.rootPackageName + ".meta.constants." + this.applicationName + "DomainModel;\n"
                + "\n"
                + "import org.eclipse.collections.api.list.*;\n"
                + "import org.eclipse.collections.api.map.*;\n"
                + "import org.eclipse.collections.api.multimap.list.*;\n"
                + "import org.eclipse.collections.impl.factory.*;\n"
                + "import org.eclipse.collections.impl.map.ordered.mutable.*;\n"
                + "\n"
                + "/**\n"
                + " * Auto-generated by {@link " + this.getClass().getCanonicalName() + "}\n"
                + " */\n"
                + "public enum " + eachInterface.getName() + "_Interface implements Interface\n"
                + "{\n"
                + "    INSTANCE;\n"
                + "\n"
                + this.getMemberConstantsSourceCode(eachInterface)
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public String getPackageName()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(eachInterface.getPackageName()) + "\";\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public String getName()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(eachInterface.getName()) + "\";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public int getOrdinal()\n"
                + "    {\n"
                + "        return " + eachInterface.getOrdinal() + ";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Optional<Element> getMacroElement()\n"
                + "    {\n"
                + "        return Optional.empty();\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public ImmutableList<Interface> getInterfaces()\n"
                + "    {\n"
                + "        return Lists.immutable.with(" + eachInterface.getInterfaces().collect(this::getDomainModelConstant).makeString() + ");\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public ImmutableList<DataTypeProperty> getDeclaredDataTypeProperties()\n"
                + "    {\n"
                + "        return Lists.immutable.with(" + eachInterface.getDeclaredDataTypeProperties().collect(NamedElement::getName).makeString() + ");\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public ImmutableList<Modifier> getDeclaredModifiers()\n"
                + "    {\n"
                + "        throw new UnsupportedOperationException(this.getClass().getSimpleName()\n"
                + "                + \".getDeclaredModifiers() not implemented yet\");\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public String toString()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(eachInterface.toString()) + "\";\n"
                + "    }\n"
                + "\n"
                + this.getDataTypePropertiesSourceCode(eachInterface)
                + "}\n";
        // @formatter:on
    }

    @Nonnull
    private String getClassSourceCode(@Nonnull Klass klass) {
        // @formatter:off
        // language=JAVA
        return ""
                + "package " + klass.getPackageName() + ".meta.constants;\n"
                + "\n"
                + "import java.util.Optional;\n"
                + "import java.util.LinkedHashMap;\n"
                + "\n"
                + "import javax.annotation.*;\n"
                + "\n"
                + "import cool.klass.model.meta.domain.api.*;\n"
                + "import cool.klass.model.meta.domain.api.order.*;\n"
                + "import cool.klass.model.meta.domain.api.modifier.*;\n"
                + "import cool.klass.model.meta.domain.api.property.*;\n"
                + "import cool.klass.model.meta.domain.api.property.validation.*;\n"
                + "import cool.klass.model.meta.domain.api.source.*;\n"
                + "\n"
                + "import " + this.rootPackageName + ".meta.constants." + this.applicationName + "DomainModel;\n"
                + "\n"
                + "import org.eclipse.collections.api.list.*;\n"
                + "import org.eclipse.collections.api.map.*;\n"
                + "import org.eclipse.collections.api.multimap.list.*;\n"
                + "import org.eclipse.collections.impl.factory.*;\n"
                + "import org.eclipse.collections.impl.map.ordered.mutable.*;\n"
                + "\n"
                + "/**\n"
                + " * Auto-generated by {@link " + this.getClass().getCanonicalName() + "}\n"
                + " */\n"
                + "public enum " + klass.getName() + "_Klass implements Klass\n"
                + "{\n"
                + "    INSTANCE;\n"
                + "\n"
                + this.getMemberConstantsSourceCode(klass)
                + this.getAssociationEndConstantsSourceCode(klass)
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public String getPackageName()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(klass.getPackageName()) + "\";\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public String getName()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(klass.getName()) + "\";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public int getOrdinal()\n"
                + "    {\n"
                + "        return " + klass.getOrdinal() + ";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Optional<Element> getMacroElement()\n"
                + "    {\n"
                + "        return Optional.empty();\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public ImmutableList<Interface> getInterfaces()\n"
                + "    {\n"
                + "        return Lists.immutable.with(" + klass.getInterfaces().collect(this::getDomainModelConstant).makeString() + ");\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public ImmutableList<DataTypeProperty> getDeclaredDataTypeProperties()\n"
                + "    {\n"
                + "        return Lists.immutable.with(" + klass.getDeclaredDataTypeProperties().collect(NamedElement::getName).makeString() + ");\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public Optional<Klass> getSuperClass()\n"
                + "    {\n"
                + "        return " + this.getSuperClassSourceCode(klass) + ";\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public Optional<AssociationEnd> getVersionProperty()\n"
                + "    {\n"
                + "        return " + this.getOptionalAssociationEndSourceCode(klass.getVersionProperty()) + ";\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public Optional<AssociationEnd> getVersionedProperty()\n"
                + "    {\n"
                + "        return " + this.getOptionalAssociationEndSourceCode(klass.getVersionedProperty()) + ";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public ImmutableList<AssociationEnd> getDeclaredAssociationEnds()\n"
                + "    {\n"
                + "        return Lists.immutable.with(" + klass.getDeclaredAssociationEnds().collect(NamedElement::getName).makeString() + ");\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public ImmutableList<Modifier> getDeclaredModifiers()\n"
                + "    {\n"
                + "        throw new UnsupportedOperationException(this.getClass().getSimpleName()\n"
                + "                + \".getDeclaredModifiers() not implemented yet\");\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public boolean isUser()\n"
                + "    {\n"
                + "        return " + klass.isUser() + ";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public boolean isTransient()\n"
                + "    {\n"
                + "        return " + klass.isTransient() + ";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public String toString()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(klass.toString()) + "\";\n"
                + "    }\n"
                + "\n"
                + this.getDataTypePropertiesSourceCode(klass)
                + "\n"
                + this.getAssociationEndsSourceCode(klass)
                + "}\n";
        // @formatter:on
    }

    @Nonnull
    private String getDomainModelConstant(@Nonnull NamedElement namedElement) {
        return String.format("%sDomainModel.%s", this.applicationName, namedElement.getName());
    }

    private String getSuperClassSourceCode(@Nonnull Klass klass) {
        if (klass.getSuperClass().isEmpty()) {
            return "Optional.empty()";
        }

        return String.format(
            "Optional.of(%sDomainModel.%s)",
            this.applicationName,
            klass.getSuperClass().get().getName()
        );
    }

    private String getAssociationEndsSourceCode(@Nonnull Klass klass) {
        return klass.getDeclaredAssociationEnds().collect(this::getAssociationEndSourceCode).makeString("\n");
    }

    @Nonnull
    private String getOptionalAssociationEndSourceCode(@Nonnull Optional<AssociationEnd> optionalAssociationEnd) {
        return optionalAssociationEnd
            .map(associationEnd -> String.format("Optional.of(%s)", associationEnd.getName()))
            .orElse("Optional.empty()");
    }

    private String getDataTypePropertiesSourceCode(@Nonnull Classifier classifier) {
        return classifier.getDeclaredDataTypeProperties().collect(this::getDataTypePropertySourceCode).makeString("\n");
    }

    @Nonnull
    private String getDataTypePropertySourceCode(DataTypeProperty dataTypeProperty) {
        if (dataTypeProperty instanceof PrimitiveProperty primitiveProperty) {
            return this.getPrimitivePropertySourceCode(primitiveProperty);
        }
        if (dataTypeProperty instanceof EnumerationProperty enumerationProperty) {
            return this.getEnumerationPropertySourceCode(enumerationProperty);
        }
        throw new AssertionError();
    }

    @Nonnull
    private String getPrimitivePropertySourceCode(@Nonnull PrimitiveProperty primitiveProperty) {
        String uppercaseName = this.getUppercaseName(primitiveProperty);

        // language=JAVA
        return (
            "" +
            "    public static enum " +
            uppercaseName +
            "_PrimitiveProperty implements PrimitiveProperty\n" +
            "    {\n" +
            "        INSTANCE;\n" +
            "\n" +
            "        @Nonnull\n" +
            "        @Override\n" +
            "        public String getName()\n" +
            "        {\n" +
            "            return \"" +
            StringEscapeUtils.escapeJava(primitiveProperty.getName()) +
            "\";\n" +
            "        }\n" +
            "\n" +
            "        @Override\n" +
            "        public int getOrdinal()\n" +
            "        {\n" +
            "            return " +
            primitiveProperty.getOrdinal() +
            ";\n" +
            "        }\n" +
            "\n" +
            "        @Override\n" +
            "        public Optional<Element> getMacroElement()\n" +
            "        {\n" +
            "            return Optional.empty();\n" +
            "        }\n" +
            "\n" +
            "        @Override\n" +
            "        public boolean isOptional()\n" +
            "        {\n" +
            "            return " +
            primitiveProperty.isOptional() +
            ";\n" +
            "        }\n" +
            "\n" +
            "        @Override\n" +
            "        public boolean isForeignKey()\n" +
            "        {\n" +
            "            return " +
            primitiveProperty.isForeignKey() +
            ";\n" +
            "        }\n" +
            "\n" +
            "        @Nonnull\n" +
            "        @Override\n" +
            "        public ImmutableList<Modifier> getModifiers()\n" +
            "        {\n" +
            this.getPropertyModifiersSourceCode(primitiveProperty.getModifiers()) +
            "        }\n" +
            "\n" +
            "        @Override\n" +
            "        public Optional<MinLengthPropertyValidation> getMinLengthPropertyValidation()\n" +
            "        {\n" +
            "            return Optional.empty();\n" +
            "        }\n" +
            "\n" +
            "        @Override\n" +
            "        public Optional<MaxLengthPropertyValidation> getMaxLengthPropertyValidation()\n" +
            "        {\n" +
            "            return Optional.empty();\n" +
            "        }\n" +
            "\n" +
            "        @Override\n" +
            "        public Optional<MinPropertyValidation> getMinPropertyValidation()\n" +
            "        {\n" +
            "            return Optional.empty();\n" +
            "        }\n" +
            "\n" +
            "        @Override\n" +
            "        public Optional<MaxPropertyValidation> getMaxPropertyValidation()\n" +
            "        {\n" +
            "            return Optional.empty();\n" +
            "        }\n" +
            "\n" +
            "        @Nonnull\n" +
            "        @Override\n" +
            "        public Classifier getOwningClassifier()\n" +
            "        {\n" +
            "            return " +
            this.applicationName +
            "DomainModel." +
            primitiveProperty.getOwningClassifier().getName() +
            ";\n" +
            "        }\n" +
            "\n" +
            "        @Nonnull\n" +
            "        @Override\n" +
            "        public PrimitiveType getType()\n" +
            "        {\n" +
            "            return PrimitiveType." +
            primitiveProperty.getType().name() +
            ";\n" +
            "        }\n" +
            "\n" +
            "        @Override\n" +
            "        public OrderedMap<AssociationEnd, ImmutableList<DataTypeProperty>> getKeysMatchingThisForeignKey()\n" +
            "        {\n" +
            "            MutableOrderedMap<AssociationEnd, ImmutableList<DataTypeProperty>> result = OrderedMapAdapter.adapt(new LinkedHashMap<>());\n" +
            this.getKeysMatchingThisForeignKey(primitiveProperty) +
            "            return result.asUnmodifiable();\n" +
            "        }\n" +
            "\n" +
            "        @Override\n" +
            "        public OrderedMap<AssociationEnd, ImmutableList<DataTypeProperty>> getForeignKeysMatchingThisKey()\n" +
            "        {\n" +
            "            MutableOrderedMap<AssociationEnd, ImmutableList<DataTypeProperty>> result = OrderedMapAdapter.adapt(new LinkedHashMap<>());\n" +
            this.getForeignKeysMatchingThisKey(primitiveProperty) +
            "            return result.asUnmodifiable();\n" +
            "        }\n" +
            "\n" +
            "        @Override\n" +
            "        public String toString()\n" +
            "        {\n" +
            "            return \"" +
            StringEscapeUtils.escapeJava(primitiveProperty.toString()) +
            "\";\n" +
            "        }\n" +
            "    }\n"
        );
    }

    private String getKeysMatchingThisForeignKey(@Nonnull DataTypeProperty dataTypeProperty) {
        return dataTypeProperty
            .getKeysMatchingThisForeignKey()
            .keyValuesView()
            .collect(this::getForeignKeySourceCode)
            .makeString("");
    }

    private String getForeignKeySourceCode(@Nonnull Pair<AssociationEnd, DataTypeProperty> each) {
        return String.format(
            "            result.put(%s, Lists.immutable.with(%s));\n",
            this.getForeignKeySourceCode(each.getOne()),
            this.getForeignKeySourceCode(each.getTwo())
        );
    }

    private String getForeignKeySourceCode(@Nonnull Property property) {
        return String.format(
            "%sDomainModel.%s.%s",
            this.applicationName,
            property.getOwningClassifier().getName(),
            property.getName()
        );
    }

    private String getForeignKeysMatchingThisKey(DataTypeProperty dataTypeProperty) {
        return dataTypeProperty
            .getForeignKeysMatchingThisKey()
            .keyValuesView()
            .collect(this::getForeignKeySourceCode)
            .makeString("");
    }

    @Nonnull
    private String getPropertyModifiersSourceCode(@Nonnull ImmutableList<Modifier> modifiers) {
        if (modifiers.isEmpty()) {
            return "            return Lists.immutable.empty();\n";
        }
        String variablesSourceCode = modifiers.collect(this::getDataTypePropertyModifierSourceCode).makeString("\n");

        ImmutableList<String> variableNames = modifiers
            .collect(Modifier::getKeyword)
            .collect(each -> each + "_" + Modifier.class.getSimpleName());

        return (
            variablesSourceCode +
            "\n" +
            "            return Lists.immutable.with(" +
            variableNames.makeString() +
            ");\n"
        );
    }

    @Nonnull
    private String getDataTypePropertyModifierSourceCode(@Nonnull Modifier modifier) {
        String className = Modifier.class.getSimpleName();
        ModifierOwner modifierOwner = modifier.getModifierOwner();

        // @formatter:off
        // language=JAVA
        return ""
                + "            " + className + " " + modifier.getKeyword() + "_" + className + " = new " + className + "()\n"
                + "            {\n"
                + "                @Override\n"
                + "                public DataTypeProperty getModifierOwner()\n"
                + "                {\n"
                + "                    return " + this.getUppercaseName(modifierOwner) + "_" + this.getTypeName(modifierOwner) + ".INSTANCE;\n"
                + "                }\n"
                + "\n"
                + "                @Nonnull\n"
                + "                @Override\n"
                + "                public String getName()\n"
                + "                {\n"
                + "                    return \"" + StringEscapeUtils.escapeJava(modifier.getKeyword()) + "\";\n"
                + "                }\n"
                + "\n"
                + "                @Override\n"
                + "                public int getOrdinal()\n"
                + "                {\n"
                + "                    return " + modifier.getOrdinal() + ";\n"
                + "                }\n"
                + "\n"
                + "                @Override\n"
                + "                public Optional<Element> getMacroElement()\n"
                + "                {\n"
                + "                    return Optional.empty();\n"
                + "                }\n"
                + "\n"
                + "                @Override\n"
                + "                public String toString()\n"
                + "                {\n"
                + "                    return \"" + StringEscapeUtils.escapeJava(modifier.toString()) + "\";\n"
                + "                }\n"
                + "            };\n";
        // @formatter:on
    }

    @Nonnull
    private String getEnumerationPropertySourceCode(@Nonnull EnumerationProperty enumerationProperty) {
        String uppercaseName = this.getUppercaseName(enumerationProperty);

        // @formatter:off
        // language=JAVA
        return ""
                + "    public static enum " + uppercaseName + "_EnumerationProperty implements EnumerationProperty\n"
                + "    {\n"
                + "        INSTANCE;\n"
                + "\n"
                + "        @Nonnull\n"
                + "        @Override\n"
                + "        public String getName()\n"
                + "        {\n"
                + "            return \""
                + StringEscapeUtils.escapeJava(enumerationProperty.getName()) + "\";\n"
                + "        }\n"
                + "\n"
                + "        @Override\n"
                + "        public int getOrdinal()\n"
                + "        {\n"
                + "            return " + enumerationProperty.getOrdinal() + ";\n"
                + "        }\n"
                + "\n"
                + "        @Override\n"
                + "        public Optional<Element> getMacroElement()\n"
                + "        {\n"
                + "            return Optional.empty();\n"
                + "        }\n"
                + "\n"
                + "        @Override\n"
                + "        public boolean isOptional()\n"
                + "        {\n"
                + "            return " + enumerationProperty.isOptional() + ";\n"
                + "        }\n"
                + "\n"
                + "        @Override\n"
                + "        public boolean isForeignKey()\n"
                + "        {\n"
                + "            return " + enumerationProperty.isForeignKey() + ";\n"
                + "        }\n"
                + "\n"
                + "        @Nonnull\n"
                + "        @Override\n"
                + "        public ImmutableList<Modifier> getModifiers()\n"
                + "        {\n"
                + this.getPropertyModifiersSourceCode(enumerationProperty.getModifiers())
                + "        }\n"
                + "\n"
                + "        @Override\n"
                + "        public Optional<MinLengthPropertyValidation> getMinLengthPropertyValidation()\n"
                + "        {\n"
                + "            return Optional.empty();\n"
                + "        }\n"
                + "\n"
                + "        @Override\n"
                + "        public Optional<MaxLengthPropertyValidation> getMaxLengthPropertyValidation()\n"
                + "        {\n"
                + "            return Optional.empty();\n"
                + "        }\n"
                + "\n"
                + "        @Override\n"
                + "        public Optional<MinPropertyValidation> getMinPropertyValidation()\n"
                + "        {\n"
                + "            return Optional.empty();\n"
                + "        }\n"
                + "\n"
                + "        @Override\n"
                + "        public Optional<MaxPropertyValidation> getMaxPropertyValidation()\n"
                + "        {\n"
                + "            return Optional.empty();\n"
                + "        }\n"
                + "\n"
                + "        @Nonnull\n"
                + "        @Override\n"
                + "        public Classifier getOwningClassifier()\n"
                + "        {\n"
                + "            return " + this.applicationName + "DomainModel." + enumerationProperty.getOwningClassifier().getName() + ";\n"
                + "        }\n"
                + "\n"
                + "        @Nonnull\n"
                + "        @Override\n"
                + "        public Enumeration getType()\n"
                + "        {\n"
                + "            return " + this.applicationName + "DomainModel." + enumerationProperty.getType().getName() + ";\n"
                + "        }\n"
                + "\n"
                + "        @Override\n"
                + "        public OrderedMap<AssociationEnd, ImmutableList<DataTypeProperty>> getKeysMatchingThisForeignKey()\n"
                + "        {\n"
                + "            MutableOrderedMap<AssociationEnd, ImmutableList<DataTypeProperty>> result = OrderedMapAdapter.adapt(new LinkedHashMap<>());\n"
                + this.getKeysMatchingThisForeignKey(enumerationProperty)
                + "            return result.asUnmodifiable();\n"
                + "        }\n"
                + "\n"
                + "        @Override\n"
                + "        public OrderedMap<AssociationEnd, ImmutableList<DataTypeProperty>> getForeignKeysMatchingThisKey()\n"
                + "        {\n"
                + "            throw new UnsupportedOperationException(this.getClass().getSimpleName()\n"
                + "                    + \".getForeignKeysMatchingThisKey() not implemented yet\");\n"
                + "        }\n"
                + "\n"
                + "        @Override\n"
                + "        public String toString()\n"
                + "        {\n"
                + "            return \"" + StringEscapeUtils.escapeJava(enumerationProperty.toString()) + "\";\n"
                + "        }\n"
                + "    }\n";
        // @formatter:on
    }

    private String getMemberConstantsSourceCode(@Nonnull Classifier classifier) {
        // TODO: Change from properties to members
        return classifier
            .getDeclaredDataTypeProperties()
            .collect(this::getDataTypePropertyConstantSourceCode)
            .makeString("");
    }

    @Nonnull
    private String getDataTypePropertyConstantSourceCode(@Nonnull DataTypeProperty dataTypeProperty) {
        String name = dataTypeProperty.getName();
        String uppercaseName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name);
        String type = this.getTypeName(dataTypeProperty);

        return MessageFormat.format(
            "    public static final {0}_{1} {2} = {0}_{1}.INSTANCE;\n",
            uppercaseName,
            type,
            name
        );
    }

    @Nonnull
    private String getAssociationSourceCode(@Nonnull Association association) {
        // @formatter:off
        // language=JAVA
        return ""
                + "package " + association.getPackageName() + ".meta.constants;\n"
                + "\n"
                + "import java.util.Optional;\n"
                + "import java.util.LinkedHashMap;\n"
                + "\n"
                + "import javax.annotation.*;\n"
                + "\n"
                + "import cool.klass.model.meta.domain.api.*;\n"
                + "import cool.klass.model.meta.domain.api.criteria.*;\n"
                + "import cool.klass.model.meta.domain.api.modifier.*;\n"
                + "import cool.klass.model.meta.domain.api.order.*;\n"
                + "import cool.klass.model.meta.domain.api.property.*;\n"
                + "import cool.klass.model.meta.domain.api.projection.*;\n"
                + "import cool.klass.model.meta.domain.api.source.*;\n"
                + "\n"
                + "import org.eclipse.collections.api.list.*;\n"
                + "import org.eclipse.collections.api.multimap.list.*;\n"
                + "import org.eclipse.collections.impl.factory.*;\n"
                + "\n"
                + "/**\n"
                + " * Auto-generated by {@link " + this.getClass().getCanonicalName() + "}\n"
                + " */\n"
                + "public enum " + association.getName() + "_Association implements Association\n"
                + "{\n"
                + "    INSTANCE;\n"
                + "\n"
                + this.getAssociationEndConstantsSourceCode(association)
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public String getPackageName()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(association.getPackageName()) + "\";\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public String getName()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(association.getName()) + "\";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public int getOrdinal()\n"
                + "    {\n"
                + "        return " + association.getOrdinal() + ";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Optional<Element> getMacroElement()\n"
                + "    {\n"
                + "        return Optional.empty();\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Criteria getCriteria()\n"
                + "    {\n"
                + "        throw new UnsupportedOperationException(this.getClass().getSimpleName() + \".getCriteria() not implemented yet\");\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public ImmutableList<AssociationEnd> getAssociationEnds()\n"
                + "    {\n"
                + "        return Lists.immutable.with(source, target);\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public AssociationEnd getSourceAssociationEnd()\n"
                + "    {\n"
                + "        return source;\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public AssociationEnd getTargetAssociationEnd()\n"
                + "    {\n"
                + "        return target;\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public String toString()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(association.toString()) + "\";\n"
                + "    }\n"
                + "}\n";
        // @formatter:on
    }

    @Nonnull
    private String getAssociationEndSourceCode(@Nonnull AssociationEnd associationEnd) {
        String uppercaseName = this.getUppercaseName(associationEnd);

        // @formatter:off
        // language=JAVA
        return ""
                + "    public static enum " + uppercaseName + "_AssociationEnd implements AssociationEnd\n"
                + "    {\n"
                + "        INSTANCE;\n"
                + "\n"
                + this.getAssociationEndModifierConstantsSourceCode(associationEnd)
                + "        @Nonnull\n"
                + "        @Override\n"
                + "        public String getName()\n"
                + "        {\n"
                + "            return \""
                + StringEscapeUtils.escapeJava(associationEnd.getName()) + "\";\n"
                + "        }\n"
                + "\n"
                + "        @Override\n"
                + "        public int getOrdinal()\n"
                + "        {\n"
                + "            return " + associationEnd.getOrdinal() + ";\n"
                + "        }\n"
                + "\n"
                + "        @Override\n"
                + "        public Optional<Element> getMacroElement()\n"
                + "        {\n"
                + "            return Optional.empty();\n"
                + "        }\n"
                + "\n"
                + "        @Nonnull\n"
                + "        @Override\n"
                + "        public Klass getType()\n"
                + "        {\n"
                + "            return " + this.applicationName + "DomainModel." + associationEnd.getType().getName() + ";\n"
                + "        }\n"
                + "\n"
                + "        @Nonnull\n"
                + "        @Override\n"
                + "        public Multiplicity getMultiplicity()\n"
                + "        {\n"
                + "            return Multiplicity." + associationEnd.getMultiplicity().name() + ";\n"
                + "        }\n"
                + "\n"
                + "        @Nonnull\n"
                + "        @Override\n"
                + "        public ImmutableList<Modifier> getModifiers()\n"
                + "        {\n"
                + "            return Lists.immutable.with(" + associationEnd.getModifiers().collect(Modifier::getKeyword).collect(TO_CONSTANT_CASE::convert).collect(each -> each + "_MODIFIER").makeString() + ");\n"
                + "        }\n"
                + "\n"
                + "        @Nonnull\n"
                + "        @Override\n"
                + "        public Association getOwningAssociation()\n"
                + "        {\n"
                + "            return " + this.applicationName + "DomainModel." + associationEnd.getOwningAssociation().getName() + ";\n"
                + "        }\n"
                + "\n"
                + "        @Nonnull\n"
                + "        @Override\n"
                + "        public Optional<OrderBy> getOrderBy()\n"
                + "        {\n"
                + "            throw new UnsupportedOperationException(this.getClass().getSimpleName()\n"
                + "                    + \".getOrderBy() not implemented yet\");\n"
                + "        }\n"
                + "\n"
                + "        @Nonnull\n"
                + "        @Override\n"
                + "        public Klass getOwningClassifier()\n"
                + "        {\n"
                + "            return " + this.applicationName + "DomainModel." + associationEnd.getOwningClassifier().getName() + ";\n"
                + "        }\n"
                + "\n"
                + "        @Override\n"
                + "        public String toString()\n"
                + "        {\n"
                + "            return \"" + StringEscapeUtils.escapeJava(associationEnd.toString()) + "\";\n"
                + "        }\n"
                + "    }\n";
        // @formatter:on
    }

    private String getAssociationEndModifierConstantsSourceCode(@Nonnull AssociationEnd associationEnd) {
        return associationEnd.getModifiers().collect(this::getModifierConstantSourceCode).makeString("");
    }

    @Nonnull
    private String getModifierConstantSourceCode(@Nonnull Modifier modifier) {
        String className = Modifier.class.getSimpleName();

        // @formatter:off
        // language=JAVA
        return ""
                + "        public static final " + className + " " + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, modifier.getKeyword()) + "_MODIFIER = new " + className + "()\n"
                + "        {\n"
                + "            @Override\n"
                + "            public AssociationEnd getModifierOwner()\n"
                + "            {\n"
                + "                return INSTANCE;\n"
                + "            }\n"
                + "\n"
                + "            @Nonnull\n"
                + "            @Override\n"
                + "            public String getName()\n"
                + "            {\n"
                + "                return \"" + modifier.getKeyword() + "\";\n"
                + "            }\n"
                + "\n"
                + "            @Override\n"
                + "            public int getOrdinal()\n"
                + "            {\n"
                + "                return " + modifier.getOrdinal() + ";\n"
                + "            }\n"
                + "\n"
                + "            @Override\n"
                + "            public Optional<Element> getMacroElement()\n"
                + "            {\n"
                + "                return Optional.empty();\n"
                + "            }\n"
                + "        };\n\n";
        // @formatter:on
    }

    private String getAssociationEndConstantsSourceCode(@Nonnull Klass klass) {
        return klass.getDeclaredAssociationEnds().collect(this::getAssociationEndConstantSourceCode).makeString("");
    }

    @Nonnull
    private String getAssociationEndConstantsSourceCode(@Nonnull Association association) {
        return (
            this.getAssociationEndConstantSourceCode(association.getSourceAssociationEnd(), "source") +
            this.getAssociationEndConstantSourceCode(association.getTargetAssociationEnd(), "target")
        );
    }

    @Nonnull
    private String getAssociationEndConstantSourceCode(@Nonnull AssociationEnd associationEnd) {
        String name = associationEnd.getName();
        String uppercaseName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name);
        String type = this.getTypeName(associationEnd);

        return MessageFormat.format(
            "    public static final {0}_{1} {2} = {0}_{1}.INSTANCE;\n",
            uppercaseName,
            type,
            name
        );
    }

    @Nonnull
    private String getAssociationEndConstantSourceCode(@Nonnull AssociationEnd associationEnd, String sideName) {
        String name = associationEnd.getName();
        String uppercaseName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name);

        return String.format(
            "    public static final %s_Klass.%s_AssociationEnd %s = %sDomainModel.%s.%s;\n",
            associationEnd.getOwningClassifier().getName(),
            uppercaseName,
            sideName,
            this.applicationName,
            associationEnd.getOwningClassifier().getName(),
            name
        );
    }

    @Nonnull
    private String getProjectionSourceCode(@Nonnull Projection projection) {
        // @formatter:off
        // language=JAVA
        return ""
                + "package " + projection.getPackageName() + ".meta.constants;\n"
                + "\n"
                + "import java.util.Optional;\n"
                + "import java.util.LinkedHashMap;\n"
                + "\n"
                + "import javax.annotation.*;\n"
                + "\n"
                + "import cool.klass.model.meta.domain.api.*;\n"
                + "import cool.klass.model.meta.domain.api.order.*;\n"
                + "import cool.klass.model.meta.domain.api.modifier.*;\n"
                + "import cool.klass.model.meta.domain.api.property.*;\n"
                + "import cool.klass.model.meta.domain.api.projection.*;\n"
                + "import cool.klass.model.meta.domain.api.source.*;\n"
                + "\n"
                + "import org.eclipse.collections.api.list.*;\n"
                + "import org.eclipse.collections.api.multimap.list.*;\n"
                + "import org.eclipse.collections.impl.factory.*;\n"
                + "\n"
                + "/**\n"
                + " * Auto-generated by {@link " + this.getClass().getCanonicalName() + "}\n"
                + " */\n"
                + "public enum " + projection.getName() + "_Projection implements Projection\n"
                + "{\n"
                + "    INSTANCE;\n"
                + "\n"
                + this.getProjectionChildrenConstantsSourceCode(projection)
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public String getPackageName()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(projection.getPackageName()) + "\";\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public String getName()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(projection.getName()) + "\";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public int getOrdinal()\n"
                + "    {\n"
                + "        return " + projection.getOrdinal() + ";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Optional<Element> getMacroElement()\n"
                + "    {\n"
                + "        return Optional.empty();\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public Classifier getClassifier()\n"
                + "    {\n"
                + "        return " + this.applicationName + "DomainModel." + projection.getClassifier().getName() + ";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Optional<ProjectionParent> getParent()\n"
                + "    {\n"
                + "        return Optional.empty();\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public ImmutableList<? extends ProjectionChild> getChildren()\n"
                + "    {\n"
                + "        return Lists.immutable.with(" + projection.getChildren().collect(ProjectionElement::getName).makeString() + ");\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public String toString()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(projection.toString()) + "\";\n"
                + "    }\n"
                + "\n"
                + this.getProjectionChildrenSourceCode(projection, projection.getName() + "_Projection")
                + "}\n";
        // @formatter:on
    }

    private String getProjectionChildrenConstantsSourceCode(@Nonnull ProjectionParent projectionParent) {
        return projectionParent.getChildren().collect(this::getProjectionChildConstantSourceCode).makeString("");
    }

    @Nonnull
    private String getProjectionChildConstantSourceCode(@Nonnull ProjectionElement projectionElement) {
        String name = projectionElement.getName();
        String uppercaseName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name) + projectionElement.getDepth();
        String type = this.getTypeName(projectionElement);

        return MessageFormat.format(
            "    public static final {0}_{1} {2} = {0}_{1}.INSTANCE;\n",
            uppercaseName,
            type,
            name
        );
    }

    @Nonnull
    private String getProjectionChildrenSourceCode(
        @Nonnull ProjectionParent projectionParent,
        String projectionParentName
    ) {
        return projectionParent
            .getChildren()
            .collectWith(this::getProjectionChildSourceCode, projectionParentName)
            .makeString("\n")
            // https://stackoverflow.com/questions/15888934/how-to-indent-a-multi-line-paragraph-being-written-to-the-console-in-java
            .replaceAll("(?m)^", "    ");
    }

    @Nonnull
    private String getProjectionChildSourceCode(ProjectionElement projectionElement, String projectionParentName) {
        if (projectionElement instanceof ProjectionDataTypeProperty projectionDataTypeProperty) {
            return this.getProjectionDataTypePropertySourceCode(projectionDataTypeProperty, projectionParentName);
        }
        if (projectionElement instanceof ProjectionReferenceProperty projectionReferenceProperty) {
            return this.getProjectionReferencePropertySourceCode(projectionReferenceProperty, projectionParentName);
        }
        if (projectionElement instanceof ProjectionProjectionReference projectionProjectionReference) {
            return this.getProjectionProjectionReferenceSourceCode(projectionProjectionReference, projectionParentName);
        }
        throw new AssertionError(projectionElement.getClass().getSimpleName());
    }

    @Nonnull
    private String getProjectionDataTypePropertySourceCode(
        @Nonnull ProjectionDataTypeProperty projectionDataTypeProperty,
        String projectionParentName
    ) {
        String uppercaseName =
            this.getUppercaseName(projectionDataTypeProperty) + projectionDataTypeProperty.getDepth();

        DataTypeProperty dataTypeProperty = projectionDataTypeProperty.getProperty();

        // @formatter:off
        // language=JAVA
        return ""
                + "public static enum " + uppercaseName + "_ProjectionDataTypeProperty implements ProjectionDataTypeProperty\n"
                + "{\n"
                + "    INSTANCE;\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public String getName()\n"
                + "    {\n"
                + "        return \""
                + StringEscapeUtils.escapeJava(projectionDataTypeProperty.getName()) + "\";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public int getOrdinal()\n"
                + "    {\n"
                + "        return " + projectionDataTypeProperty.getOrdinal() + ";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Optional<Element> getMacroElement()\n"
                + "    {\n"
                + "        return Optional.empty();\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public String getHeaderText()\n"
                + "    {\n"
                + "        return \""
                + StringEscapeUtils.escapeJava(projectionDataTypeProperty.getHeaderText()) + "\";\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public DataTypeProperty getProperty()\n"
                + "    {\n"
                + "        return " + this.applicationName + "DomainModel." + dataTypeProperty.getOwningClassifier().getName() + "." + dataTypeProperty.getName() + ";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Optional<ProjectionParent> getParent()\n"
                + "    {\n"
                + "        return Optional.of(" + projectionParentName + ".INSTANCE);\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public String toString()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(projectionDataTypeProperty.toString()) + "\";\n"
                + "    }\n"
                + "}\n";
        // @formatter:on
    }

    @Nonnull
    private String getProjectionReferencePropertySourceCode(
        @Nonnull ProjectionReferenceProperty projectionReferenceProperty,
        String projectionParentName
    ) {
        String uppercaseName =
            this.getUppercaseName(projectionReferenceProperty) + projectionReferenceProperty.getDepth();

        ReferenceProperty referenceProperty = projectionReferenceProperty.getProperty();

        // @formatter:off
        // language=JAVA
        return ""
                + "public static enum " + uppercaseName + "_ProjectionReferenceProperty implements ProjectionReferenceProperty\n"
                + "{\n"
                + "    INSTANCE;\n"
                + "\n"
                + this.getProjectionChildrenConstantsSourceCode(projectionReferenceProperty)
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public String getName()\n"
                + "    {\n"
                + "        return \""
                + StringEscapeUtils.escapeJava(projectionReferenceProperty.getName()) + "\";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public int getOrdinal()\n"
                + "    {\n"
                + "        return " + projectionReferenceProperty.getOrdinal() + ";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Optional<Element> getMacroElement()\n"
                + "    {\n"
                + "        return Optional.empty();\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Optional<ProjectionParent> getParent()\n"
                + "    {\n"
                + "        return Optional.of(" + projectionParentName + ".INSTANCE);\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public ImmutableList<? extends ProjectionChild> getChildren()\n"
                + "    {\n"
                + "        return Lists.immutable.with(" + projectionReferenceProperty.getChildren().collect(ProjectionElement::getName).makeString() + ");\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public AssociationEnd getProperty()\n"
                + "    {\n"
                + "        return " + this.applicationName + "DomainModel." + referenceProperty.getOwningClassifier().getName() + "." + referenceProperty.getName() + ";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public String toString()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(projectionReferenceProperty.toString()) + "\";\n"
                + "    }\n"
                + "\n"
                + this.getProjectionChildrenSourceCode(projectionReferenceProperty, uppercaseName + "_ProjectionReferenceProperty")
                + "}\n";
        // @formatter:on
    }

    @Nonnull
    private String getProjectionProjectionReferenceSourceCode(
        @Nonnull ProjectionProjectionReference projectionProjectionReference,
        String projectionParentName
    ) {
        String uppercaseName =
            this.getUppercaseName(projectionProjectionReference) + projectionProjectionReference.getDepth();

        ReferenceProperty referenceProperty = projectionProjectionReference.getProperty();

        // @formatter:off
        // language=JAVA
        return ""
                + "public static enum " + uppercaseName + "_ProjectionProjectionReference implements ProjectionProjectionReference\n"
                + "{\n"
                + "    INSTANCE;\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public String getName()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(projectionProjectionReference.getName()) + "\";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public int getOrdinal()\n"
                + "    {\n"
                + "        return " + projectionProjectionReference.getOrdinal() + ";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Optional<Element> getMacroElement()\n"
                + "    {\n"
                + "        return Optional.empty();\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Projection getProjection()\n"
                + "    {\n"
                + "        return " + this.applicationName + "DomainModel." + projectionProjectionReference.getProjection().getName() + ";\n"
                + "    }\n"
                + "\n"
                + "    @Nonnull\n"
                + "    @Override\n"
                + "    public AssociationEnd getProperty()\n"
                + "    {\n"
                + "        return " + this.applicationName + "DomainModel." + referenceProperty.getOwningClassifier().getName() + "." + referenceProperty.getName() + ";\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public Optional<ProjectionParent> getParent()\n"
                + "    {\n"
                + "        return Optional.of(" + projectionParentName + ".INSTANCE);\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public String toString()\n"
                + "    {\n"
                + "        return \"" + StringEscapeUtils.escapeJava(projectionProjectionReference.toString()) + "\";\n"
                + "    }\n"
                + "}\n";
        // @formatter:on
    }

    private String getUppercaseName(NamedElement namedElement) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, namedElement.getName());
    }
}
