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

package cool.klass.generator.klass.service;

import java.nio.file.Path;

import javax.annotation.Nonnull;

import cool.klass.generator.perpackage.AbstractPerPackageGenerator;
import cool.klass.model.meta.domain.api.DomainModel;

public class KlassServiceGenerator extends AbstractPerPackageGenerator {

    public KlassServiceGenerator(@Nonnull DomainModel domainModel) {
        super(domainModel);
    }

    @Nonnull
    @Override
    protected Path getPluginRelativePath(Path path) {
        return path.resolve("klass").resolve("service");
    }

    @Override
    @Nonnull
    protected String getFileName() {
        return "generated-services.klass";
    }

    @Override
    @Nonnull
    protected String getPackageSourceCode(@Nonnull String fullyQualifiedPackage) {
        return KlassServiceSourceCodeGenerator.getPackageSourceCode(this.domainModel, fullyQualifiedPackage);
    }
}
