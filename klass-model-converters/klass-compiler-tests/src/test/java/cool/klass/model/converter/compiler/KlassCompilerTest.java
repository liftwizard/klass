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

package cool.klass.model.converter.compiler;

import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.annotation.CompilerAnnotation;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationRenderer;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(LogMarkerTestExtension.class)
class KlassCompilerTest
{
	@Test
	void stackOverflow()
	{
		this.assertNoCompilerErrors("/com/stackoverflow/stackoverflow.klass");
	}

	@Test
	void factorioPrints()
	{
		this.assertNoCompilerErrors("factorio-prints.klass");
	}

	@Test
	void emoji()
	{
		this.assertNoCompilerErrors("emoji.klass");
	}

	@Test
	void projectionOnInterface()
	{
		this.assertNoCompilerErrors("projectionOnInterface.klass");
	}

	@Test
	void coverageExample()
	{
		this.assertNoCompilerErrors("/cool/klass/xample/coverage/coverage-example.klass");
	}

	@Test
	void objectStorage()
	{
		DomainModelWithSourceCode domainModel = this.assertNoCompilerErrors("objectStorage.klass");

		Klass blueprintString = domainModel.getClassByName("BlueprintString");
		DataTypeProperty content = blueprintString.getDataTypePropertyByName("blueprintString");
		assertThat(content.isObjectStorage()).isTrue();
		assertThat(content.getObjectStorageDigestProperty()).hasValue(
			blueprintString.getDataTypePropertyByName("digest")
		);
		assertThat(content.getObjectStorageAlgorithm()).hasValue("sha256");

		Klass screenshot = domainModel.getClassByName("Screenshot");
		DataTypeProperty image = screenshot.getDataTypePropertyByName("image");
		assertThat(image.getObjectStorageDigestProperty()).hasValue(screenshot.getDataTypePropertyByName("digest"));
		assertThat(image.getObjectStorageAlgorithm()).hasValue("blake3");

		DataTypeProperty attachment = domainModel.getClassByName("Attachment").getDataTypePropertyByName("content");
		assertThat(attachment.isObjectStorage()).isTrue();
		assertThat(attachment.getObjectStorageDigestProperty()).isEmpty();
		assertThat(attachment.getObjectStorageAlgorithm()).isEmpty();
	}

	private DomainModelWithSourceCode assertNoCompilerErrors(@Nonnull String sourceCodeName)
	{
		String sourceCodeText = FileSlurper.slurp(sourceCodeName, this.getClass());
		CompilationUnit compilationUnit = CompilationUnit.createFromText(
			0,
			Optional.empty(),
			sourceCodeName,
			sourceCodeText
		);
		AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("dark");
		var compiler = new KlassCompiler(compilationUnit);
		CompilationResult compilationResult = compiler.compile();
		ImmutableList<CompilerAnnotation> compilerAnnotations = compilationResult
			.compilerAnnotations()
			.select(CompilerAnnotation::isError);

		var compilerAnnotationRenderer = new CompilerAnnotationRenderer(
			colorScheme,
			compilationResult.tokenCategories(),
			false
		);
		String message = compilerAnnotations.collect(compilerAnnotationRenderer::render).makeString("\n");
		assertThat(compilerAnnotations).as(message).isEqualTo(Lists.immutable.empty());

		assertThat(compilationResult.domainModelWithSourceCode()).isPresent();
		return compilationResult.domainModelWithSourceCode().get();
	}
}
