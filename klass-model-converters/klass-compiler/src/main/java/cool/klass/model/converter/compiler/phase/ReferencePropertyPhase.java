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

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrClassReference;
import cool.klass.model.converter.compiler.state.AntlrClassReferenceOwner;
import cool.klass.model.converter.compiler.state.AntlrDomainModel;
import cool.klass.model.converter.compiler.state.AntlrMultiplicity;
import cool.klass.model.converter.compiler.state.AntlrMultiplicityOwner;
import cool.klass.model.meta.grammar.KlassParser.ClassReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.MultiplicityContext;

public class ReferencePropertyPhase extends AbstractCompilerPhase {

    @Nullable
    protected AntlrClassReferenceOwner classReferenceOwner;

    @Nullable
    protected AntlrMultiplicityOwner multiplicityOwner;

    public ReferencePropertyPhase(CompilerState compilerState) {
        super(compilerState);
    }

    public void handleClassReference(@Nonnull ClassReferenceContext ctx) {
        if (this.classReferenceOwner == null) {
            return;
        }

        String className = ctx.identifier().getText();
        AntlrDomainModel domainModel = this.compilerState.getDomainModel();
        AntlrClass klass = domainModel.getClassByName(className);

        AntlrClassReference classReference = new AntlrClassReference(
            ctx,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            this.classReferenceOwner,
            klass
        );

        this.classReferenceOwner.enterClassReference(classReference);
    }

    public void handleMultiplicity(@Nonnull MultiplicityContext ctx) {
        if (this.multiplicityOwner == null) {
            return;
        }

        AntlrMultiplicity multiplicity = new AntlrMultiplicity(
            ctx,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            this.multiplicityOwner
        );

        this.multiplicityOwner.enterMultiplicity(multiplicity);
    }
}
