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

package cool.klass.model.converter.compiler.phase;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.meta.grammar.KlassParser.TopLevelDeclarationContext;

public class TopLevelElementsPhase extends AbstractCompilerPhase {

	public TopLevelElementsPhase(@Nonnull CompilerState compilerState) {
		super(compilerState);
	}

	@Override
	public void enterTopLevelDeclaration(@Nonnull TopLevelDeclarationContext ctx) {
		super.enterTopLevelDeclaration(ctx);
		this.compilerState.getDomainModel().enterTopLevelDeclaration(ctx);
	}
}
