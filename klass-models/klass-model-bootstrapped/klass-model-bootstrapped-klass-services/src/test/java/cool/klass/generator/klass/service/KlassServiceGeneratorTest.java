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

import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import cool.klass.model.meta.domain.api.PackageableElement;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import cool.klass.model.meta.loader.compiler.DomainModelCompilerLoader;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(LogMarkerTestExtension.class)
public class KlassServiceGeneratorTest {

    public static final String FULLY_QUALIFIED_PACKAGE = "klass.model.meta.domain";

    @RegisterExtension
    final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

    @Test
    void smokeTest() {
        ImmutableList<String> klassSourcePackages = Lists.immutable.with(FULLY_QUALIFIED_PACKAGE);

        var domainModelCompilerLoader = new DomainModelCompilerLoader(
            klassSourcePackages,
            Thread.currentThread().getContextClassLoader(),
            DomainModelCompilerLoader::logCompilerError,
            ColorSchemeProvider.getByName("craig-light")
        );

        DomainModelWithSourceCode domainModel = domainModelCompilerLoader.load();
        ImmutableList<String> packageNames = domainModel
            .getClassifiers()
            .asLazy()
            .collect(PackageableElement::getPackageName)
            .distinct()
            .toImmutableList();
        for (String packageName : packageNames) {
            String sourceCode = KlassServiceSourceCodeGenerator.getPackageSourceCode(domainModel, packageName);
            String resourceClassPathLocation = packageName + ".klass";

            this.fileMatchExtension.assertFileContents(resourceClassPathLocation, sourceCode);
        }
    }
}
