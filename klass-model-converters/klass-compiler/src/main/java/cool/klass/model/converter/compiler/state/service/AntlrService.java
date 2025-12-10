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

package cool.klass.model.converter.compiler.state.service;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrElement;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.ReferencedPropertiesCriteriaVisitor;
import cool.klass.model.converter.compiler.state.criteria.AntlrCriteria;
import cool.klass.model.converter.compiler.state.order.AntlrOrderBy;
import cool.klass.model.converter.compiler.state.order.AntlrOrderByOwner;
import cool.klass.model.converter.compiler.state.parameter.AntlrParameter;
import cool.klass.model.converter.compiler.state.service.url.AntlrUrl;
import cool.klass.model.meta.domain.api.service.ServiceMultiplicity;
import cool.klass.model.meta.domain.api.service.Verb;
import cool.klass.model.meta.domain.order.OrderByImpl.OrderByBuilder;
import cool.klass.model.meta.domain.service.ServiceImpl.ServiceBuilder;
import cool.klass.model.meta.domain.service.ServiceProjectionDispatchImpl.ServiceProjectionDispatchBuilder;
import cool.klass.model.meta.domain.service.url.UrlImpl.UrlBuilder;
import cool.klass.model.meta.grammar.KlassParser.ServiceCriteriaDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceProjectionDispatchContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;
import org.eclipse.collections.impl.tuple.Tuples;

public class AntlrService extends AntlrElement implements AntlrOrderByOwner {

    public static final AntlrService AMBIGUOUS = new AntlrService(
        new ServiceDeclarationContext(AMBIGUOUS_PARENT, -1),
        Optional.empty(),
        AntlrUrl.AMBIGUOUS,
        AntlrVerb.AMBIGUOUS
    );

    private static final ImmutableMap<Verb, ImmutableList<String>> ALLOWED_CRITERIA_TYPES = Maps.immutable
        .<Verb, ImmutableList<String>>empty()
        .newWithKeyValue(Verb.GET, Lists.immutable.with("authorize", "criteria", "version"))
        .newWithKeyValue(Verb.POST, Lists.immutable.with("authorize"))
        .newWithKeyValue(Verb.PUT, Lists.immutable.with("authorize", "criteria", "conflict"))
        .newWithKeyValue(Verb.PATCH, Lists.immutable.with("authorize", "criteria", "conflict"))
        .newWithKeyValue(Verb.DELETE, Lists.immutable.with("authorize", "criteria", "conflict"));

    @Nonnull
    private final AntlrUrl url;

    @Nonnull
    private final AntlrVerb verb;

    private final MutableList<AntlrServiceCriteria> serviceCriterias = Lists.mutable.empty();

    private final MutableOrderedMap<ServiceCriteriaDeclarationContext, AntlrServiceCriteria> serviceCriteriaByContext =
        OrderedMapAdapter.adapt(new LinkedHashMap<>());

    @Nullable
    private AntlrServiceMultiplicity serviceMultiplicity;

    @Nonnull
    private Optional<AntlrServiceProjectionDispatch> serviceProjectionDispatch = Optional.empty();

    @Nonnull
    private Optional<AntlrOrderBy> orderBy = Optional.empty();

    private ServiceBuilder elementBuilder;

    public AntlrService(
        @Nonnull ServiceDeclarationContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        @Nonnull AntlrUrl url,
        @Nonnull AntlrVerb verb
    ) {
        super(elementContext, compilationUnit);
        this.url = Objects.requireNonNull(url);
        this.verb = Objects.requireNonNull(verb);
    }

    @Nonnull
    @Override
    public Optional<IAntlrElement> getSurroundingElement() {
        return Optional.of(this.url);
    }

    @Override
    public boolean isContext() {
        return true;
    }

    @Override
    public Pair<Token, Token> getContextBefore() {
        return Tuples.pair(this.getElementContext().getStart(), this.getElementContext().serviceBlock().getStart());
    }

    @Override
    public Pair<Token, Token> getContextAfter() {
        return Tuples.pair(
            this.getElementContext().serviceBlock().getStop(),
            this.getElementContext().serviceBlock().getStop()
        );
    }

    @Nonnull
    public AntlrUrl getUrl() {
        return this.url;
    }

    @Nullable
    public AntlrServiceMultiplicity getServiceMultiplicity() {
        return this.serviceMultiplicity;
    }

    public MutableList<AntlrServiceCriteria> getServiceCriterias() {
        return this.serviceCriterias.asUnmodifiable();
    }

    public AntlrServiceCriteria getServiceCriteriaByContext(ServiceCriteriaDeclarationContext ctx) {
        return this.serviceCriteriaByContext.get(ctx);
    }

    public void reportDuplicateVerb(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        String message = String.format("Duplicate verb: '%s'.", this.verb.getVerb());

        compilerAnnotationHolder.add("ERR_DUP_VRB", message, this, this.verb.getElementContext());
    }

    public void enterServiceCriteriaDeclaration(@Nonnull AntlrServiceCriteria serviceCriteria) {
        this.serviceCriterias.add(serviceCriteria);
        AntlrServiceCriteria duplicate = this.serviceCriteriaByContext.put(
            serviceCriteria.getElementContext(),
            serviceCriteria
        );
        if (duplicate != null) {
            throw new AssertionError();
        }
    }

    public void enterServiceProjectionDispatch(@Nonnull AntlrServiceProjectionDispatch projectionDispatch) {
        this.serviceProjectionDispatch = Optional.of(projectionDispatch);
    }

    // <editor-fold desc="Report Compiler Errors">
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        this.reportDuplicateKeywords(compilerAnnotationHolder);
        this.reportUnreferencedUrlParameters(compilerAnnotationHolder);
        this.reportInvalidProjection(compilerAnnotationHolder);

        Verb verb = this.verb.getVerb();
        ImmutableList<String> allowedCriteriaTypes = ALLOWED_CRITERIA_TYPES.get(verb);

        for (AntlrServiceCriteria serviceCriteria : this.serviceCriterias) {
            if (allowedCriteriaTypes == null) {
                throw new AssertionError(verb);
            }
            serviceCriteria.reportAllowedCriteriaTypes(compilerAnnotationHolder, allowedCriteriaTypes);
            serviceCriteria.getCriteria().reportErrors(compilerAnnotationHolder);
        }

        this.orderBy.ifPresent((orderBy) -> orderBy.reportErrors(compilerAnnotationHolder));
    }

    private void reportUnreferencedUrlParameters(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        ImmutableList<AntlrParameter> urlParameters = this.url.getFormalParametersByName().toImmutableList();

        MutableSet<AntlrParameter> referencedParameters = Sets.mutable.empty();

        for (AntlrServiceCriteria serviceCriteria : this.serviceCriterias) {
            AntlrCriteria criteria = serviceCriteria.getCriteria();
            var visitor = new ReferencedPropertiesCriteriaVisitor();
            criteria.visit(visitor);

            Set<AntlrParameter> parametersReferencedByCriteria = visitor.getParametersReferencedByCriteria();
            referencedParameters.addAll(parametersReferencedByCriteria);
        }

        for (AntlrParameter parameter : urlParameters) {
            if (parameter == AntlrParameter.AMBIGUOUS || parameter == AntlrParameter.NOT_FOUND) {
                continue;
            }
            if (!referencedParameters.contains(parameter)) {
                String message = String.format(
                    "URL parameter '%s' is not referenced in any criteria or order by.",
                    parameter.getName()
                );
                compilerAnnotationHolder.add("ERR_URL_REF", message, parameter);
            }
        }
    }

    protected void reportDuplicateKeywords(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        ImmutableBag<String> duplicateKeywords = this.serviceCriterias.collect(
                AntlrServiceCriteria::getServiceCriteriaKeyword
            )
            .toBag()
            .selectByOccurrences((occurrences) -> occurrences > 1)
            .toImmutable();

        this.serviceCriterias.select((each) ->
            duplicateKeywords.contains(each.getServiceCriteriaKeyword())
        ).forEachWith(AntlrServiceCriteria::reportDuplicateKeyword, compilerAnnotationHolder);
    }

    private void reportInvalidProjection(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        Verb verb = this.verb.getVerb();

        if (verb == Verb.GET) {
            this.serviceProjectionDispatch.ifPresentOrElse(
                (projectionDispatch) -> projectionDispatch.reportErrors(compilerAnnotationHolder),
                () -> this.reportMissingProjection(compilerAnnotationHolder)
            );
        } else if (verb == Verb.POST) {
            this.serviceProjectionDispatch.ifPresent((projectionDispatch) ->
                projectionDispatch.reportErrors(compilerAnnotationHolder)
            );
        } else {
            this.serviceProjectionDispatch.ifPresent((projectionDispatch) ->
                this.reportPresentProjection(projectionDispatch, compilerAnnotationHolder)
            );
        }
    }

    private void reportMissingProjection(CompilerAnnotationHolder compilerAnnotationHolder) {
        ParserRuleContext verbContext = this.verb.getElementContext();

        compilerAnnotationHolder.add("ERR_GET_PRJ", "GET services require a projection.", this, verbContext);
    }

    private void reportPresentProjection(
        AntlrServiceProjectionDispatch projectionDispatch,
        CompilerAnnotationHolder compilerAnnotationHolder
    ) {
        ServiceProjectionDispatchContext elementContext = projectionDispatch.getElementContext();

        compilerAnnotationHolder.add(
            "ERR_PUT_PRJ",
            String.format("%s services must not have a projection.", this.verb.getVerb().name()),
            projectionDispatch,
            elementContext
        );
    }

    // </editor-fold>

    @Nonnull
    public AntlrVerb getVerb() {
        return this.verb;
    }

    public void enterServiceMultiplicityDeclaration(@Nonnull AntlrServiceMultiplicity serviceMultiplicity) {
        this.serviceMultiplicity = Objects.requireNonNull(serviceMultiplicity);
    }

    @Override
    public void enterOrderByDeclaration(@Nonnull AntlrOrderBy orderBy) {
        if (this.orderBy.isPresent()) {
            throw new IllegalStateException();
        }
        this.orderBy = Optional.of(orderBy);
    }

    @Override
    @Nonnull
    public Optional<AntlrOrderBy> getOrderBy() {
        return this.orderBy;
    }

    public ServiceBuilder build() {
        if (this.elementBuilder != null) {
            throw new IllegalStateException();
        }

        UrlBuilder urlBuilder = this.url.getElementBuilder();
        Verb verb = this.verb.getVerb();
        ServiceMultiplicity serviceMultiplicity = this.serviceMultiplicity.getServiceMultiplicity();
        this.elementBuilder = new ServiceBuilder(
            (ServiceDeclarationContext) this.elementContext,
            this.getMacroElementBuilder(),
            this.getSourceCodeBuilder(),
            urlBuilder,
            verb,
            serviceMultiplicity
        );

        for (AntlrServiceCriteria serviceCriteria : this.serviceCriterias) {
            String serviceCriteriaKeyword = serviceCriteria.getServiceCriteriaKeyword();
            AntlrCriteria criteria = serviceCriteria.getCriteria();
            this.elementBuilder.addCriteriaBuilder(serviceCriteriaKeyword, criteria.build());
        }

        Optional<ServiceProjectionDispatchBuilder> projectionDispatchBuilder = this.serviceProjectionDispatch.map(
            AntlrServiceProjectionDispatch::build
        );
        this.elementBuilder.setProjectionDispatchBuilder(projectionDispatchBuilder);

        Optional<OrderByBuilder> orderByBuilder = this.orderBy.map(AntlrOrderBy::build);
        this.elementBuilder.setOrderByBuilder(orderByBuilder);

        return this.elementBuilder;
    }

    @Override
    @Nonnull
    public ServiceBuilder getElementBuilder() {
        return Objects.requireNonNull(this.elementBuilder);
    }

    @Nonnull
    @Override
    public ServiceDeclarationContext getElementContext() {
        return (ServiceDeclarationContext) super.getElementContext();
    }
}
