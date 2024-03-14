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

package cool.klass.generator.dto;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.google.common.base.CaseFormat;
import cool.klass.model.meta.domain.api.DataType;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Enumeration;
import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.Interface;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.PackageableElement;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.visitor.PrimitiveToJavaTypeVisitor;
import org.eclipse.collections.api.list.ImmutableList;

public class DataTransferObjectsGenerator
{
    @Nonnull
    private final DomainModel domainModel;

    public DataTransferObjectsGenerator(@Nonnull DomainModel domainModel)
    {
        this.domainModel = Objects.requireNonNull(domainModel);
    }

    public void writeDataTransferObjectFiles(@Nonnull Path outputPath) throws IOException
    {
        for (Enumeration enumeration : this.domainModel.getEnumerations())
        {
            Path dtoOutputPath = this.getDtoOutputPath(outputPath, enumeration);
            this.printStringToFile(dtoOutputPath, this.getEnumerationSourceCode(enumeration));
        }

        for (Klass klass : this.domainModel.getClasses())
        {
            Path dtoOutputPath = this.getDtoOutputPath(outputPath, klass);
            this.printStringToFile(dtoOutputPath, this.getClassSourceCode(klass));
        }
    }

    @Nonnull
    public Path getDtoOutputPath(
            @Nonnull Path outputPath,
            @Nonnull PackageableElement packageableElement)
    {
        String packageRelativePath = packageableElement.getPackageName()
                .replaceAll("\\.", "/");
        Path dtoDirectory = outputPath
                .resolve(packageRelativePath)
                .resolve("dto");
        dtoDirectory.toFile().mkdirs();
        String fileName = packageableElement.getName() + "DTO.java";
        return dtoDirectory.resolve(fileName);
    }

    private void printStringToFile(@Nonnull Path path, String contents) throws FileNotFoundException
    {
        try (PrintStream printStream = new PrintStream(new FileOutputStream(path.toFile())))
        {
            printStream.print(contents);
        }
    }

    @Nonnull
    private String getEnumerationSourceCode(@Nonnull Enumeration enumeration)
    {
        String packageName        = enumeration.getPackageName() + ".dto";
        String literalsSourceCode = enumeration.getEnumerationLiterals().collect(this::getLiteral).makeString("");

        boolean hasPrettyName = enumeration
                .getEnumerationLiterals()
                .anySatisfy(each -> each.getDeclaredPrettyName().isPresent());
        String prettyNameImport = hasPrettyName
                ? "import com.fasterxml.jackson.annotation.JsonProperty;\n"
                : "";

        // @formatter:off
        //language=JAVA
        return ""
                + "package " + packageName + ";\n"
                + "\n"
                + prettyNameImport
                + "/**\n"
                + " * Auto-generated by {@link " + this.getClass().getCanonicalName() + "}\n"
                + " */\n"
                + "public enum " + enumeration.getName() + "DTO\n"
                + "{\n"
                + literalsSourceCode
                + "}\n";
        // @formatter:on
    }

    @Nonnull
    public String getClassSourceCode(@Nonnull Klass klass)
    {
        String packageName = klass.getPackageName() + ".dto";

        ImmutableList<DataTypeProperty> dataTypeProperties = klass.getDataTypeProperties()
                .select(each -> each.getOwningClassifier() == klass || each.getOwningClassifier() instanceof Interface)
                .reject(DataTypeProperty::isPrivate);
        String dataFieldsSourceCode = dataTypeProperties.collect(this::getDataField).makeString("")
                + (dataTypeProperties.isEmpty() ? "" : "\n");

        String dataGettersSettersSourceCode = dataTypeProperties.collect(this::getDataGetterSetter).makeString("");

        ImmutableList<AssociationEnd> associationEnds = klass.getDeclaredAssociationEnds();

        String referenceFieldsSourceCode =
                associationEnds.collect(this::getReferenceField).makeString("");

        String referenceGettersSettersSourceCode =
                associationEnds.collect(this::getReferenceGetterSetter).makeString("");

        boolean hasConstraints = dataTypeProperties
                .asLazy()
                .reject(DataTypeProperty::isKey)
                .reject(DataTypeProperty::isTemporal)
                .select(DataTypeProperty::isRequired)
                .notEmpty();
        String constraintImports = hasConstraints
                ? "import javax.validation.constraints.*;\n"
                : "";

        String superClassDeclaration = klass.getSuperClass().isPresent()
                ? "        extends " + klass.getSuperClass().get().getName() + "DTO\n"
                : "";

        String abstractKeyword = klass.isAbstract() ? "abstract " : "";

        // @formatter:off
        //language=JAVA
        String sourceCode = ""
                + "package " + packageName + ";\n"
                + "\n"
                + "import java.time.*;\n"
                + "import java.util.*;\n"
                + "\n"
                + constraintImports
                + "\n"
                + "/**\n"
                + " * Auto-generated by {@link " + this.getClass().getCanonicalName() + "}\n"
                + " */\n"
                + "public " + abstractKeyword + "class " + klass.getName() + "DTO\n"
                + superClassDeclaration
                + "{\n"
                + dataFieldsSourceCode
                + referenceFieldsSourceCode
                + dataGettersSettersSourceCode
                + referenceGettersSettersSourceCode
                + "}\n";
        // @formatter:on

        return sourceCode;
    }

    @Nonnull
    private String getLiteral(@Nonnull EnumerationLiteral enumerationLiteral)
    {
        String line1 = enumerationLiteral.getDeclaredPrettyName()
                .map(prettyName -> "    @JsonProperty(\"" + prettyName + "\")\n")
                .orElse("");

        String line2 = "    " + enumerationLiteral.getName() + ",\n";

        return line1 + line2;
    }

    @Nonnull
    private String getDataGetterSetter(@Nonnull DataTypeProperty dataTypeProperty)
    {
        String type          = this.getType(dataTypeProperty.getType());
        String name          = dataTypeProperty.getName();
        String uppercaseName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name);
        return this.getGetterSetter(type, name, uppercaseName);
    }

    @Nonnull
    private String getReferenceGetterSetter(@Nonnull AssociationEnd associationEnd)
    {
        String type          = this.getType(associationEnd.getType(), associationEnd.getMultiplicity());
        String name          = associationEnd.getName();
        String uppercaseName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name);
        return this.getGetterSetter(type, name, uppercaseName);
    }

    @Nonnull
    private String getGetterSetter(String type, String name, String uppercaseName)
    {
        //language=JAVA
        return ""
                + "\n"
                + "    public " + type + " get" + uppercaseName + "()\n"
                + "    {\n"
                + "        return this." + name + ";\n"
                + "    }\n"
                + "\n"
                + "    public void set" + uppercaseName + "(" + type + " " + name + ")\n"
                + "    {\n"
                + "        this." + name + " = " + name + ";\n"
                + "    }\n";
    }

    @Nonnull
    private String getType(DataType dataType)
    {
        if (dataType instanceof Enumeration)
        {
            return ((Enumeration) dataType).getName() + "DTO";
        }
        if (dataType instanceof PrimitiveType)
        {
            return PrimitiveToJavaTypeVisitor.getJavaType((PrimitiveType) dataType);
        }
        throw new AssertionError();
    }

    @Nonnull
    private String getType(@Nonnull Klass klass, @Nonnull Multiplicity multiplicity)
    {
        String toOneType = klass.getName() + "DTO";
        if (multiplicity.isToOne())
        {
            return toOneType;
        }

        return "List<" + toOneType + ">";
    }

    private String getDataField(@Nonnull DataTypeProperty dataTypeProperty)
    {
        String annotation = this.getAnnotation(dataTypeProperty);
        String type       = this.getType(dataTypeProperty.getType());
        return String.format("%s    private %s %s;\n", annotation, type, dataTypeProperty.getName());
    }

    @Nonnull
    private String getAnnotation(@Nonnull DataTypeProperty dataTypeProperty)
    {
        return this.isNullable(dataTypeProperty) ? "" : "    @NotNull\n";
    }

    private boolean isNullable(@Nonnull DataTypeProperty dataTypeProperty)
    {
        return dataTypeProperty.isTemporal() || dataTypeProperty.isOptional() || dataTypeProperty.isKey();
    }

    private String getReferenceField(@Nonnull AssociationEnd associationEnd)
    {
        Multiplicity multiplicity = associationEnd.getMultiplicity();

        // TODO: NotNull shouldn't apply if the ONE_TO_ONE is a version association end.
        String annotation = "";
        // String annotation = multiplicity.isToMany() || multiplicity == Multiplicity.ONE_TO_ONE ? "    @NotNull\n" : "";
        String type = this.getType(associationEnd.getType(), multiplicity);
        return String.format("%s    private %s %s;\n", annotation, type, associationEnd.getName());
    }
}
