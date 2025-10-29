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
import cool.klass.model.converter.compiler.state.criteria.AntlrCriteria;
import cool.klass.model.converter.compiler.state.parameter.AntlrParameter;
import cool.klass.model.converter.compiler.state.service.AntlrService;
import cool.klass.model.converter.compiler.state.service.AntlrServiceCriteria;
import cool.klass.model.converter.compiler.state.service.url.AntlrUrl;
import cool.klass.model.meta.grammar.KlassParser.UrlDeclarationContext;
import org.eclipse.collections.api.map.OrderedMap;

// Phase must run after inferring additional parameters like version, in ServicePhase
public class VariableResolutionPhase extends AbstractCompilerPhase {

    public VariableResolutionPhase(@Nonnull CompilerState compilerState) {
        super(compilerState);
    }

    @Override
    public void enterUrlDeclaration(@Nonnull UrlDeclarationContext ctx) {
        super.enterUrlDeclaration(ctx);

        AntlrUrl url = this.compilerState.getCompilerWalk().getUrl();

        OrderedMap<String, AntlrParameter> formalParametersByName = url.getFormalParametersByName();
        for (AntlrService service : url.getServices()) {
            for (AntlrServiceCriteria serviceCriteria : service.getServiceCriterias()) {
                AntlrCriteria criteria = serviceCriteria.getCriteria();
                criteria.resolveServiceVariables(formalParametersByName);
                // TODO: ❓ Type inference here?
                criteria.resolveTypes();
            }
        }
    }
}
