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

package cool.klass.generator.perpackage;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.PackageableElement;
import org.eclipse.collections.api.list.ImmutableList;

public abstract class AbstractPerPackageGenerator {

    @Nonnull
    protected final DomainModel domainModel;

    protected AbstractPerPackageGenerator(@Nonnull DomainModel domainModel) {
        this.domainModel = Objects.requireNonNull(domainModel);
    }

    public void writeFiles(@Nonnull Path outputPath) {
        ImmutableList<String> packageNames = this.getPackageNames();
        for (String packageName : packageNames) {
            this.writeFile(packageName, outputPath);
        }
    }

    protected ImmutableList<String> getPackageNames() {
        return this.domainModel.getClasses()
            .asLazy()
            .collect(PackageableElement::getPackageName)
            .distinct()
            .toImmutableList();
    }

    protected void writeFile(String fullyQualifiedPackage, Path outputPath) {
        Path klassOutputPath = this.getOutputPath(outputPath, fullyQualifiedPackage);
        String sourceCode = this.getPackageSourceCode(fullyQualifiedPackage);

        this.printStringToFile(klassOutputPath, sourceCode);
    }

    @Nonnull
    private Path getOutputPath(@Nonnull Path outputPath, @Nonnull String fullyQualifiedPackage) {
        String packageRelativePathString = fullyQualifiedPackage.replaceAll("\\.", "/");
        Path packageRelativePath = this.getPluginRelativePath(outputPath.resolve(packageRelativePathString));
        packageRelativePath.toFile().mkdirs();
        return packageRelativePath.resolve(this.getFileName());
    }

    protected void printStringToFile(@Nonnull Path path, String contents) {
        try (PrintStream printStream = new PrintStream(new FileOutputStream(path.toFile()))) {
            printStream.print(contents);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    protected abstract Path getPluginRelativePath(Path path);

    @Nonnull
    protected abstract String getFileName();

    @Nonnull
    protected abstract String getPackageSourceCode(@Nonnull String fullyQualifiedPackage);
}
