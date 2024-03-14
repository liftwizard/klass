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

package cool.klass.model.converter.compiler.phase;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.service.AntlrService;
import cool.klass.model.meta.domain.api.service.Verb;
import cool.klass.model.meta.grammar.KlassParser;
import cool.klass.model.meta.grammar.KlassParser.ServiceBodyContext;
import org.antlr.v4.runtime.tree.ParseTreeListener;

public class ServiceCriteriaInferencePhase
        extends AbstractCompilerPhase
{
    public ServiceCriteriaInferencePhase(@Nonnull CompilerState compilerState)
    {
        super(compilerState);
    }

    @Nonnull
    @Override
    public String getName()
    {
        return "Service criteria";
    }

    @Override
    public void exitServiceBody(ServiceBodyContext ctx)
    {
        this.runCompilerMacro(ctx);
        super.exitServiceBody(ctx);
    }

    private void runCompilerMacro(ServiceBodyContext inPlaceContext)
    {
        AntlrService service = this.compilerState.getCompilerWalk().getService();
        if (service.getServiceCriterias().notEmpty() || service.getVerb().getVerb() != Verb.GET)
        {
            return;
        }
        String sourceCodeText = "            criteria    : all;\n";
        this.runCompilerMacro(inPlaceContext, sourceCodeText);
    }

    private void runCompilerMacro(ServiceBodyContext inPlaceContext, @Nonnull String sourceCodeText)
    {
        AntlrService      service       = this.compilerState.getCompilerWalk().getService();
        ParseTreeListener compilerPhase = new ServiceCriteriaPhase(this.compilerState);

        this.compilerState.runInPlaceCompilerMacro(
                service,
                this,
                sourceCodeText,
                KlassParser::serviceCriteriaDeclaration,
                inPlaceContext,
                compilerPhase);
    }
}
