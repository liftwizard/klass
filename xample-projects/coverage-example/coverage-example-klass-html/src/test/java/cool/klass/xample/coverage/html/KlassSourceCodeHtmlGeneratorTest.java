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

package cool.klass.xample.coverage.html;

import cool.klass.generator.klass.html.KlassSourceCodeHtmlGenerator;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.loader.compiler.DomainModelCompilerLoader;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(LogMarkerTestExtension.class)
public class KlassSourceCodeHtmlGeneratorTest {

    public static final String FULLY_QUALIFIED_PACKAGE = "cool.klass.xample.coverage";

    @RegisterExtension
    final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

    @Test
    void smokeTest() {
        ImmutableList<String> klassSourcePackages = Lists.immutable.with(FULLY_QUALIFIED_PACKAGE);

        var domainModelCompilerLoader = new DomainModelCompilerLoader(
            klassSourcePackages,
            Thread.currentThread().getContextClassLoader(),
            DomainModelCompilerLoader::logCompilerError,
            ColorSchemeProvider.getByName("dark")
        );

        DomainModelWithSourceCode domainModel = domainModelCompilerLoader.load();
        ImmutableList<SourceCode> sourceCodesFromMacros = domainModel
            .getSourceCodes()
            .select(each -> each.getMacroSourceCode().isPresent());
        ImmutableListMultimap<String, SourceCode> sourceCodesByFullPath = sourceCodesFromMacros.groupBy(
            SourceCode::getFullPathSourceName
        );
        sourceCodesByFullPath.forEachKeyMultiValues((fullPath, sourceCodes) -> {
            if (sourceCodes.size() > 1) {
                fail("Multiple source codes for " + fullPath);
            }
        });

        for (SourceCode sourceCode : domainModel.getSourceCodes()) {
            String fullPathSourceName = sourceCode.getFullPathSourceName();

            String html = KlassSourceCodeHtmlGenerator.getSourceCode(domainModel, sourceCode);

            String resourceClassPathLocation = fullPathSourceName + ".html";
            this.fileMatchExtension.assertFileContents(resourceClassPathLocation, html);
        }
    }
}
