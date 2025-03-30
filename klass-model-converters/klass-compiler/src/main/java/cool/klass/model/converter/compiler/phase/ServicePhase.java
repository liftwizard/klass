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
import cool.klass.model.converter.compiler.state.projection.AntlrProjection;
import cool.klass.model.converter.compiler.state.service.AntlrService;
import cool.klass.model.converter.compiler.state.service.AntlrServiceGroup;
import cool.klass.model.converter.compiler.state.service.AntlrServiceProjectionDispatch;
import cool.klass.model.converter.compiler.state.service.AntlrVerb;
import cool.klass.model.converter.compiler.state.service.url.AntlrUrl;
import cool.klass.model.meta.domain.api.service.Verb;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceGroupDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceProjectionDispatchContext;
import cool.klass.model.meta.grammar.KlassParser.UrlDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.VerbContext;

public class ServicePhase extends AbstractCompilerPhase {

    @Nullable
    private AntlrServiceGroup serviceGroup;

    @Nullable
    private AntlrUrl url;

    @Nullable
    private AntlrService service;

    public ServicePhase(@Nonnull CompilerState compilerState) {
        super(compilerState);
    }

    @Override
    public void enterServiceGroupDeclaration(@Nonnull ServiceGroupDeclarationContext ctx) {
        super.enterServiceGroupDeclaration(ctx);

        IdentifierContext nameContext = ctx.identifier();
        String serviceClassName = ctx.classReference().getText();
        AntlrClass klass = this.compilerState.getDomainModel().getClassByName(serviceClassName);

        this.serviceGroup = new AntlrServiceGroup(
            ctx,
            this.compilerState.getCompilerWalk().getCompilationUnit(),
            this.compilerState.getOrdinal(ctx),
            nameContext,
            klass
        );
    }

    @Override
    public void exitServiceGroupDeclaration(@Nonnull ServiceGroupDeclarationContext ctx) {
        this.compilerState.getDomainModel().exitServiceGroupDeclaration(this.serviceGroup);
        this.serviceGroup = null;
        super.exitServiceGroupDeclaration(ctx);
    }

    @Override
    public void enterUrlDeclaration(@Nonnull UrlDeclarationContext ctx) {
        super.enterUrlDeclaration(ctx);
        this.url = new AntlrUrl(
            ctx,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            this.serviceGroup
        );
        this.serviceGroup.enterUrlDeclaration(this.url);
    }

    @Override
    public void exitUrlDeclaration(@Nonnull UrlDeclarationContext ctx) {
        this.url = null;
        super.exitUrlDeclaration(ctx);
    }

    @Override
    public void enterServiceDeclaration(@Nonnull ServiceDeclarationContext ctx) {
        super.enterServiceDeclaration(ctx);
        VerbContext verb = ctx.verb();
        AntlrVerb antlrVerb = new AntlrVerb(
            verb,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            Verb.valueOf(verb.getText())
        );

        this.service = new AntlrService(
            ctx,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            this.url,
            antlrVerb
        );
    }

    @Override
    public void exitServiceDeclaration(@Nonnull ServiceDeclarationContext ctx) {
        this.url.exitServiceDeclaration(this.service);
        this.service = null;
        super.exitServiceDeclaration(ctx);
    }

    @Override
    public void enterServiceProjectionDispatch(@Nonnull ServiceProjectionDispatchContext ctx) {
        super.enterServiceProjectionDispatch(ctx);
        ProjectionReferenceContext projectionReferenceContext = ctx.projectionReference();

        String projectionName = projectionReferenceContext.identifier().getText();
        AntlrProjection projection = this.compilerState.getDomainModel().getProjectionByName(projectionName);

        if (ctx.argumentList() != null && !ctx.argumentList().argument().isEmpty()) {
            throw new AssertionError();
        }

        AntlrServiceProjectionDispatch projectionDispatch = new AntlrServiceProjectionDispatch(
            ctx,
            Optional.of(this.compilerState.getCompilerWalk().getCurrentCompilationUnit()),
            this.service,
            projection
        );

        this.service.enterServiceProjectionDispatch(projectionDispatch);
    }
}
