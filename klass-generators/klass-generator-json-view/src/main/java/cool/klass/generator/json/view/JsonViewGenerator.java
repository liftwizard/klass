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

package cool.klass.generator.json.view;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.projection.Projection;

// TODO: Refactor out the commonality between the several Generators
public class JsonViewGenerator
{
    @Nonnull
    private final DomainModel domainModel;
    @Nonnull
    private final String      rootPackageName;
    @Nonnull
    private final String      applicationName;

    public JsonViewGenerator(
            @Nonnull DomainModel domainModel,
            @Nonnull String rootPackageName,
            @Nonnull String applicationName)
    {
        this.domainModel     = Objects.requireNonNull(domainModel);
        this.rootPackageName = Objects.requireNonNull(rootPackageName);
        this.applicationName = Objects.requireNonNull(applicationName);
    }

    public void writeJsonViews(@Nonnull Path outputPath) throws IOException
    {
        for (Projection projection : this.domainModel.getProjections())
        {
            Path jsonViewOutputPath = this.getJsonViewOutputPath(outputPath, projection);
            this.printStringToFile(jsonViewOutputPath, this.getJsonViewSourceCode(projection));
        }
    }

    @Nonnull
    private Path getJsonViewOutputPath(
            @Nonnull Path outputPath,
            @Nonnull Projection packageableElement)
    {
        String packageRelativePath = packageableElement.getPackageName()
                .replaceAll("\\.", "/");
        Path outputDirectory = outputPath
                .resolve(packageRelativePath)
                .resolve("json")
                .resolve("view");
        outputDirectory.toFile().mkdirs();
        String fileName = packageableElement.getName() + "_JsonView.java";
        return outputDirectory.resolve(fileName);
    }

    private void printStringToFile(@Nonnull Path path, String contents) throws FileNotFoundException
    {
        try (PrintStream printStream = new PrintStream(new FileOutputStream(path.toFile())))
        {
            printStream.print(contents);
        }
    }

    @Nonnull
    private String getJsonViewSourceCode(@Nonnull Projection projection)
    {
        // @formatter:off
        //language=JAVA
        return ""
                + "package " + projection.getPackageName() + ".json.view;\n"
                + "\n"
                + "import cool.klass.serialization.jackson.jsonview.KlassJsonView;\n"
                + "\n"
                + "/**\n"
                + " * Auto-generated by {@link " + this.getClass().getCanonicalName() + "}\n"
                + " */\n"
                + "public class " + projection.getName() + "_JsonView implements KlassJsonView\n"
                + "{\n"
                + "    @Override\n"
                + "    public String getProjectionName()\n"
                + "    {\n"
                + "        return \"" + projection.getName() + "\";\n"
                + "    }\n"
                + "}\n";
        // @formatter:on
    }
}
