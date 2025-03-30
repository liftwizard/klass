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

package cool.klass.model.converter.compiler.state.service;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrElement;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.criteria.AntlrCriteria;
import cool.klass.model.meta.grammar.KlassParser.ServiceCriteriaDeclarationContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.Pair;

public class AntlrServiceCriteria extends AntlrElement {

    @Nonnull
    private final String serviceCriteriaKeyword;

    @Nonnull
    private final AntlrService service;

    private AntlrCriteria antlrCriteria;

    public AntlrServiceCriteria(
        @Nonnull ServiceCriteriaDeclarationContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        @Nonnull String serviceCriteriaKeyword,
        @Nonnull AntlrService service
    ) {
        super(elementContext, compilationUnit);
        this.serviceCriteriaKeyword = Objects.requireNonNull(serviceCriteriaKeyword);
        this.service = Objects.requireNonNull(service);
    }

    @Nonnull
    @Override
    public Optional<IAntlrElement> getSurroundingElement() {
        return Optional.of(this.service);
    }

    @Override
    public boolean isContext() {
        return true;
    }

    @Override
    public Pair<Token, Token> getContextBefore() {
        return this.getEntireContext();
    }

    @Nonnull
    public AntlrCriteria getCriteria() {
        return Objects.requireNonNull(this.antlrCriteria);
    }

    public void setCriteria(@Nonnull AntlrCriteria antlrCriteria) {
        this.antlrCriteria = Objects.requireNonNull(antlrCriteria);
    }

    public void reportDuplicateKeyword(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        // TODO: Test coverage of duplicate service criteria
        String message = String.format("Duplicate service criteria: '%s'.", this.serviceCriteriaKeyword);
        compilerAnnotationHolder.add("ERR_DUP_CRI", message, this.antlrCriteria, this.getElementContext());
    }

    @Nonnull
    @Override
    public ServiceCriteriaDeclarationContext getElementContext() {
        return (ServiceCriteriaDeclarationContext) super.getElementContext();
    }

    @Nonnull
    public String getServiceCriteriaKeyword() {
        return this.serviceCriteriaKeyword;
    }

    public void reportAllowedCriteriaTypes(
        @Nonnull CompilerAnnotationHolder compilerAnnotationHolder,
        @Nonnull ImmutableList<String> allowedCriteriaTypes
    ) {
        if (!allowedCriteriaTypes.contains(this.serviceCriteriaKeyword)) {
            String error = String.format(
                "Criteria '%s' not allowed for verb '%s'. Must be one of %s.",
                this.serviceCriteriaKeyword,
                this.service.getVerb().getVerb(),
                allowedCriteriaTypes
            );
            compilerAnnotationHolder.add("ERR_VRB_CRT", error, this, this.getElementContext().serviceCriteriaKeyword());
        }
    }
}
