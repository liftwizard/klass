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

import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.service.AntlrService;
import cool.klass.model.converter.compiler.state.service.AntlrServiceMultiplicity;
import cool.klass.model.meta.domain.api.service.ServiceMultiplicity;
import cool.klass.model.meta.grammar.KlassParser.ServiceMultiplicityContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceMultiplicityDeclarationContext;

public class ServiceMultiplicityPhase extends AbstractCompilerPhase {

	public ServiceMultiplicityPhase(@Nonnull CompilerState compilerState) {
		super(compilerState);
	}

	@Override
	public void enterServiceMultiplicityDeclaration(ServiceMultiplicityDeclarationContext ctx) {
		super.enterServiceMultiplicityDeclaration(ctx);
		ServiceMultiplicityContext multiplicityContext = ctx.serviceMultiplicity();

		AntlrServiceMultiplicity serviceMultiplicity = new AntlrServiceMultiplicity(
			multiplicityContext,
			Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
			this.getServiceMultiplicity(multiplicityContext)
		);

		AntlrService service = this.compilerState.getCompilerWalk().getService();
		service.enterServiceMultiplicityDeclaration(serviceMultiplicity);
	}

	@Nonnull
	private ServiceMultiplicity getServiceMultiplicity(@Nonnull ServiceMultiplicityContext serviceMultiplicityContext) {
		if (serviceMultiplicityContext.one != null) {
			return ServiceMultiplicity.ONE;
		}
		if (serviceMultiplicityContext.many != null) {
			return ServiceMultiplicity.MANY;
		}
		throw new AssertionError();
	}
}
