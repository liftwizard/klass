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

package cool.klass.model.converter.compiler.state.service.url;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrOrdinalElement;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.meta.domain.service.url.UrlConstantImpl.UrlConstantBuilder;
import cool.klass.model.meta.grammar.KlassParser.UrlConstantContext;

public class AntlrUrlConstant extends AntlrOrdinalElement {

	private UrlConstantBuilder elementBuilder;

	public AntlrUrlConstant(
		@Nonnull UrlConstantContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		int ordinal
	) {
		super(elementContext, compilationUnit, ordinal);
	}

	@Nonnull
	@Override
	public Optional<IAntlrElement> getSurroundingElement() {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".getSurroundingContext() not implemented yet"
		);
	}

	@Nonnull
	public UrlConstantBuilder build() {
		if (this.elementBuilder != null) {
			throw new IllegalStateException();
		}

		this.elementBuilder = new UrlConstantBuilder(
			(UrlConstantContext) this.elementContext,
			this.getMacroElementBuilder(),
			this.getSourceCodeBuilder(),
			this.ordinal,
			this.getName()
		);
		return this.elementBuilder;
	}

	@Override
	@Nonnull
	public UrlConstantBuilder getElementBuilder() {
		return Objects.requireNonNull(this.elementBuilder);
	}

	public String getName() {
		return this.elementContext.getText();
	}

	public void reportNameErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		// URLs can contain almost anything. The parser is already more strict than any error checking that needs to happen here.
		// https://stackoverflow.com/questions/7109143/what-characters-are-valid-in-a-url
	}
}
