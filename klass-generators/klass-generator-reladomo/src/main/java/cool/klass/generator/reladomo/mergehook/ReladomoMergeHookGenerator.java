package cool.klass.generator.reladomo.mergehook;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.PackageableElement;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;

// TODO: Consider moving this into its own module.
public class ReladomoMergeHookGenerator
{
    private static final Converter<String, String> LOWER_CAMEL_TO_UPPER_CAMEL =
            CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL);

    @Nonnull
    private final DomainModel domainModel;

    public ReladomoMergeHookGenerator(@Nonnull DomainModel domainModel)
    {
        this.domainModel = Objects.requireNonNull(domainModel);
    }

    public void writeMergeHookFiles(@Nonnull Path outputPath)
    {
        this.domainModel
                .getClasses()
                .reject(Klass::isAbstract)
                .select(Klass::isVersioned)
                .forEachWith(this::writeMergeHookFile, outputPath);
    }

    private void writeMergeHookFile(@Nonnull Klass klass, @Nonnull Path outputPath)
    {
        Path   mergeHookOutputPath = this.getMergeHookOutputPath(outputPath, klass);
        String classSourceCode     = this.getMergeHookSourceCode(klass);
        this.printStringToFile(mergeHookOutputPath, classSourceCode);
    }

    @Nonnull
    private Path getMergeHookOutputPath(
            @Nonnull Path outputPath,
            @Nonnull PackageableElement packageableElement)
    {
        String packageRelativePath = packageableElement.getPackageName()
                .replaceAll("\\.", "/");
        Path mergeHookDirectory = outputPath
                .resolve(packageRelativePath)
                .resolve("reladomo")
                .resolve("merge")
                .resolve("hook");
        mergeHookDirectory.toFile().mkdirs();
        String fileName = packageableElement.getName() + "MergeHook.java";
        return mergeHookDirectory.resolve(fileName);
    }

    private void printStringToFile(@Nonnull Path path, String contents)
    {
        try (PrintStream printStream = new PrintStream(new FileOutputStream(path.toFile())))
        {
            printStream.print(contents);
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    private String getMergeHookSourceCode(@Nonnull Klass klass)
    {
        Klass versionKlass = klass.getVersionProperty().get().getType();

        String setKeyPropertiesSourceCode = klass
                .getKeyProperties()
                .collect(this::getKeyPropertySourceCode)
                .makeString("");

        String setAuditPropertiesSourceCode = klass.isAudited()
                ? ""
                + "        version.setCreatedOn(newObject.getCreatedOn());\n"
                + "        version.setCreatedById(newObject.getCreatedById());\n"
                + "        version.setLastUpdatedById(newObject.getLastUpdatedById());\n"
                : "";

        // @formatter:off
        //language=JAVA
        String sourceCode = ""
                + "package " + klass.getPackageName() + ".reladomo.merge.hook;\n"
                + "\n"
                + "import javax.annotation.*;\n"
                + "\n"
                + "import " + klass.getFullyQualifiedName() + ";\n"
                + "import " + versionKlass.getFullyQualifiedName() + ";\n"
                + "import com.gs.fw.common.mithra.list.merge.MergeBuffer;\n"
                + "import com.gs.fw.common.mithra.list.merge.MergeHook;\n"
                + "\n"
                + "/**\n"
                + " * Auto-generated by {@link cool.klass.generator.reladomo.mergehook.ReladomoMergeHookGenerator}\n"
                + " */\n"
                + "public class " + klass.getName() + "MergeHook extends MergeHook<" + klass.getName() + ">\n"
                + "{\n"
                + "    @Override\n"
                + "    public InsertInstruction beforeInsertOfNew(@Nonnull " + klass.getName() + " newObject)\n"
                + "    {\n"
                + "        " + versionKlass.getName() + " version = new " + versionKlass.getName() + "();\n"
                + "        version.setNumber(1);\n"
                + setKeyPropertiesSourceCode
                + setAuditPropertiesSourceCode
                + "        version.insert();\n"
                + "        return super.beforeInsertOfNew(newObject);\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public UpdateInstruction matchedWithDifferenceBeforeAttributeCopy(\n"
                + "            @Nonnull " + klass.getName() + " existing,\n"
                + "            " + klass.getName() + " incoming)\n"
                + "    {\n"
                + "        " + versionKlass.getName() + " existingVersion = existing.getVersion();\n"
                + "        existingVersion.setNumber(existingVersion.getNumber() + 1);\n"
                + "        return super.matchedWithDifferenceBeforeAttributeCopy(existing, incoming);\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public DeleteOrTerminateInstruction beforeDeleteOrTerminate(\n"
                + "            @Nonnull " + klass.getName() + " existing,\n"
                + "            MergeBuffer<" + klass.getName() + "> mergeBuffer)\n"
                + "    {\n"
                + "        existing.getVersion().terminate();\n"
                + "        return super.beforeDeleteOrTerminate(existing, mergeBuffer);\n"
                + "    }\n"
                + "}\n"
                + "\n";
        // @formatter:on

        return sourceCode;
    }

    private String getKeyPropertySourceCode(DataTypeProperty keyProperty)
    {
        String name = LOWER_CAMEL_TO_UPPER_CAMEL.convert(keyProperty.getName());
        return "        version.set" + name + "(newObject.get" + name + "());\n";
    }
}
