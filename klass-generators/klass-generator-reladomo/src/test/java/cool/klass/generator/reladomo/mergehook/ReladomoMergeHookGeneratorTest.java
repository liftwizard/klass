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

package cool.klass.generator.reladomo.mergehook;

import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import cool.klass.model.meta.loader.compiler.DomainModelCompilerLoader;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(LogMarkerTestExtension.class)
class ReladomoMergeHookGeneratorTest
{
	@RegisterExtension
	final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

	@Test
	void versionedClassWithOwnedChildren()
	{
		var domainModelCompilerLoader = new DomainModelCompilerLoader(
			Lists.immutable.with("cool.klass.generator.reladomo.mergehook"),
			Thread.currentThread().getContextClassLoader(),
			DomainModelCompilerLoader::logCompilerError,
			ColorSchemeProvider.getByName("dark"),
			false
		);
		DomainModelWithSourceCode domainModel = domainModelCompilerLoader.load();

		Klass klass = domainModel.getClassByName("Owner");
		String sourceCode = new ReladomoMergeHookGenerator(domainModel).getSourceCode(klass);

		this.fileMatchExtension.assertFileContents(
			this.getClass().getSimpleName() + ".versionedClassWithOwnedChildren.java",
			sourceCode
		);
	}
}
