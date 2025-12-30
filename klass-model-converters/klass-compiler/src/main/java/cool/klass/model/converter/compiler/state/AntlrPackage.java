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

package cool.klass.model.converter.compiler.state;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.meta.grammar.KlassParser.PackageDeclarationContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.tuple.Pair;

public class AntlrPackage extends AntlrNamedElement {

	private final AntlrCompilationUnit compilationUnitState;

	public AntlrPackage(
		@Nonnull PackageDeclarationContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		int ordinal,
		@Nonnull ParserRuleContext nameContext,
		@Nonnull AntlrCompilationUnit compilationUnitState
	) {
		super(elementContext, compilationUnit, ordinal, nameContext);
		this.compilationUnitState = Objects.requireNonNull(compilationUnitState);
	}

	@Nonnull
	@Override
	public PackageDeclarationContext getElementContext() {
		return (PackageDeclarationContext) super.getElementContext();
	}

	@Override
	protected Pattern getNamePattern() {
		return PACKAGE_NAME_PATTERN;
	}

	@Nonnull
	@Override
	public Optional<IAntlrElement> getSurroundingElement() {
		return Optional.of(this.compilationUnitState);
	}

	@Override
	public Pair<Token, Token> getContextBefore() {
		return this.getEntireContext();
	}
}
