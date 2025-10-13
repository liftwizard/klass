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
import javax.annotation.OverridingMethodsMustInvokeSuper;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.order.AntlrOrderByMemberReferencePath;
import cool.klass.model.meta.grammar.KlassParser;
import cool.klass.model.meta.grammar.KlassParser.OrderByMemberReferencePathContext;
import org.antlr.v4.runtime.tree.ParseTreeListener;

public class OrderByDirectionInferencePhase extends AbstractCompilerPhase {

    public OrderByDirectionInferencePhase(@Nonnull CompilerState compilerState) {
        super(compilerState);
    }

    @Nonnull
    @Override
    public String getName() {
        return "OrderBy Direction";
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void exitOrderByMemberReferencePath(OrderByMemberReferencePathContext inPlaceContext) {
        this.runCompilerMacro(inPlaceContext);
        super.exitOrderByMemberReferencePath(inPlaceContext);
    }

    private void runCompilerMacro(OrderByMemberReferencePathContext inPlaceContext) {
        AntlrOrderByMemberReferencePath orderByMemberReferencePath =
            this.compilerState.getCompilerWalk().getOrderByMemberReferencePath();

        if (orderByMemberReferencePath.getOrderByDirection() != null) {
            return;
        }

        String sourceCodeText = "ascending";
        ParseTreeListener compilerPhase = new OrderByDirectionPhase(this.compilerState);

        this.compilerState.runInPlaceCompilerMacro(
            orderByMemberReferencePath,
            this,
            sourceCodeText,
            KlassParser::orderByDirection,
            inPlaceContext,
            compilerPhase
        );
    }
}
