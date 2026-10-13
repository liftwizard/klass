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

package cool.klass.model.converter.compiler.benchmarks;

import java.util.Optional;

import cool.klass.model.converter.compiler.CompilationResult;
import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.KlassCompiler;
import cool.klass.model.converter.compiler.annotation.AbstractCompilerAnnotation;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * Compiles every model in {@link KlassSources#ALL}, each as its own domain model.
 */
final class KlassCorpusCompiler {

	private final ImmutableList<String> sourceNames;
	private final ImmutableList<String> sourceCodeTexts;
	private final AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("dark");

	KlassCorpusCompiler() {
		this.sourceNames = KlassSources.ALL;
		this.sourceCodeTexts = this.sourceNames.collect(KlassSources::read);
	}

	int lineCount() {
		return (int) this.sourceCodeTexts.sumOfInt(text -> text.split("\n", -1).length);
	}

	ImmutableList<CompilationResult> compileAll() {
		return this.sourceNames.collectWithIndex((sourceName, index) -> this.compile(
			sourceName,
			this.sourceCodeTexts.get(index)
		));
	}

	private CompilationResult compile(String sourceName, String sourceCodeText) {
		CompilationUnit compilationUnit = CompilationUnit.createFromText(0, Optional.empty(), sourceName, sourceCodeText);
		CompilationResult result = new KlassCompiler(compilationUnit, this.colorScheme).compile();
		if (result.compilerAnnotations().anySatisfy(AbstractCompilerAnnotation::isError)) {
			throw new IllegalStateException("Expected " + sourceName + " to compile without errors");
		}
		return result;
	}
}
