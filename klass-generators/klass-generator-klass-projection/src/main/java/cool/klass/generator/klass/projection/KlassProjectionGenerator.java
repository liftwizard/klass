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

package cool.klass.generator.klass.projection;

import java.nio.file.Path;

import javax.annotation.Nonnull;

import cool.klass.generator.perpackage.AbstractPerPackageGenerator;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.PackageableElement;
import org.eclipse.collections.api.list.ImmutableList;

public class KlassProjectionGenerator extends AbstractPerPackageGenerator {

    public KlassProjectionGenerator(@Nonnull DomainModel domainModel) {
        super(domainModel);
    }

    // Overridden to process all Classifiers instead of Classes
    @Override
    protected ImmutableList<String> getPackageNames() {
        return this.domainModel.getClassifiers()
            .asLazy()
            .collect(PackageableElement::getPackageName)
            .distinct()
            .toImmutableList();
    }

    @Nonnull
    @Override
    protected Path getPluginRelativePath(Path path) {
        return path.resolve("klass").resolve("projection");
    }

    @Override
    @Nonnull
    protected String getFileName() {
        return "generated-projections.klass";
    }

    @Override
    @Nonnull
    protected String getPackageSourceCode(@Nonnull String fullyQualifiedPackage) {
        return KlassProjectionSourceCodeGenerator.getPackageSourceCode(this.domainModel, fullyQualifiedPackage);
    }
}
