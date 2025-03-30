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

package cool.klass.model.meta.domain.service;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.AbstractElement;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.criteria.Criteria;
import cool.klass.model.meta.domain.api.order.OrderBy;
import cool.klass.model.meta.domain.api.service.Service;
import cool.klass.model.meta.domain.api.service.ServiceMultiplicity;
import cool.klass.model.meta.domain.api.service.ServiceProjectionDispatch;
import cool.klass.model.meta.domain.api.service.Verb;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.criteria.AbstractCriteria.AbstractCriteriaBuilder;
import cool.klass.model.meta.domain.order.OrderByImpl.OrderByBuilder;
import cool.klass.model.meta.domain.service.ServiceProjectionDispatchImpl.ServiceProjectionDispatchBuilder;
import cool.klass.model.meta.domain.service.url.UrlImpl;
import cool.klass.model.meta.domain.service.url.UrlImpl.UrlBuilder;
import cool.klass.model.meta.grammar.KlassParser.ServiceDeclarationContext;

public final class ServiceImpl extends AbstractElement implements Service {

    @Nonnull
    private final UrlImpl url;

    @Nonnull
    private final Verb verb;

    @Nonnull
    private final ServiceMultiplicity serviceMultiplicity;

    private Optional<Criteria> queryCriteria;
    private Optional<Criteria> authorizeCriteria;
    private Optional<Criteria> validateCriteria;
    private Optional<Criteria> conflictCriteria;

    private Optional<ServiceProjectionDispatch> projectionDispatch;

    @Nonnull
    private Optional<OrderBy> orderBy = Optional.empty();

    private ServiceImpl(
        @Nonnull ServiceDeclarationContext elementContext,
        @Nonnull Optional<Element> macroElement,
        @Nullable SourceCode sourceCode,
        @Nonnull UrlImpl url,
        @Nonnull Verb verb,
        @Nonnull ServiceMultiplicity serviceMultiplicity
    ) {
        super(elementContext, macroElement, sourceCode);
        this.url = Objects.requireNonNull(url);
        this.verb = Objects.requireNonNull(verb);
        this.serviceMultiplicity = Objects.requireNonNull(serviceMultiplicity);
    }

    @Nonnull
    @Override
    public ServiceDeclarationContext getElementContext() {
        return (ServiceDeclarationContext) super.getElementContext();
    }

    @Override
    @Nonnull
    public UrlImpl getUrl() {
        return this.url;
    }

    @Override
    @Nonnull
    public Verb getVerb() {
        return this.verb;
    }

    @Override
    @Nonnull
    public ServiceMultiplicity getServiceMultiplicity() {
        return this.serviceMultiplicity;
    }

    @Nonnull
    @Override
    public Optional<Criteria> getQueryCriteria() {
        return this.queryCriteria;
    }

    private void setQueryCriteria(@Nonnull Optional<Criteria> queryCriteria) {
        if (this.queryCriteria != null) {
            throw new IllegalStateException();
        }
        this.queryCriteria = Objects.requireNonNull(queryCriteria);
    }

    @Nonnull
    @Override
    public Optional<Criteria> getAuthorizeCriteria() {
        return this.authorizeCriteria;
    }

    private void setAuthorizeCriteria(@Nonnull Optional<Criteria> authorizeCriteria) {
        if (this.authorizeCriteria != null) {
            throw new IllegalStateException();
        }
        this.authorizeCriteria = Objects.requireNonNull(authorizeCriteria);
    }

    @Nonnull
    @Override
    public Optional<Criteria> getValidateCriteria() {
        return this.validateCriteria;
    }

    private void setValidateCriteria(@Nonnull Optional<Criteria> validateCriteria) {
        if (this.validateCriteria != null) {
            throw new IllegalStateException();
        }
        this.validateCriteria = Objects.requireNonNull(validateCriteria);
    }

    @Nonnull
    @Override
    public Optional<Criteria> getConflictCriteria() {
        return this.conflictCriteria;
    }

    private void setConflictCriteria(@Nonnull Optional<Criteria> conflictCriteria) {
        if (this.conflictCriteria != null) {
            throw new IllegalStateException();
        }
        this.conflictCriteria = Objects.requireNonNull(conflictCriteria);
    }

    @Override
    public Optional<ServiceProjectionDispatch> getProjectionDispatch() {
        return Objects.requireNonNull(this.projectionDispatch);
    }

    private void setProjectionDispatch(Optional<ServiceProjectionDispatch> projectionDispatch) {
        if (this.projectionDispatch != null) {
            throw new IllegalStateException();
        }
        this.projectionDispatch = Objects.requireNonNull(projectionDispatch);
    }

    @Override
    @Nonnull
    public Optional<OrderBy> getOrderBy() {
        return Objects.requireNonNull(this.orderBy);
    }

    private void setOrderBy(@Nonnull Optional<OrderBy> orderBy) {
        this.orderBy = Objects.requireNonNull(orderBy);
    }

    @Override
    public int getNumParameters() {
        int numUrlParameters = this.url.getParameters().size();
        int numVersionParameters = this.isVersionClauseRequired() ? 1 : 0;
        int numAuthorizeParameters = this.isAuthorizeClauseRequired() ? 1 : 0;
        return numUrlParameters + numVersionParameters + numAuthorizeParameters;
    }

    @Override
    public boolean isVersionClauseRequired() {
        return (
            this.serviceMultiplicity == ServiceMultiplicity.ONE &&
            this.url.getServiceGroup().getKlass().getVersionProperty().isPresent()
        );
    }

    @Override
    public boolean isAuthorizeClauseRequired() {
        return this.authorizeCriteria.isPresent();
    }

    public static final class ServiceBuilder extends ElementBuilder<ServiceImpl> {

        @Nonnull
        private final UrlBuilder urlBuilder;

        @Nonnull
        private final Verb verb;

        @Nonnull
        private final ServiceMultiplicity serviceMultiplicity;

        private Optional<ServiceProjectionDispatchBuilder> projectionDispatchBuilder;

        @Nonnull
        private Optional<OrderByBuilder> orderByBuilder = Optional.empty();

        private Optional<AbstractCriteriaBuilder<?>> criteria = Optional.empty();
        private Optional<AbstractCriteriaBuilder<?>> authorize = Optional.empty();
        private Optional<AbstractCriteriaBuilder<?>> validate = Optional.empty();
        private Optional<AbstractCriteriaBuilder<?>> conflict = Optional.empty();

        public ServiceBuilder(
            @Nonnull ServiceDeclarationContext elementContext,
            @Nonnull Optional<ElementBuilder<?>> macroElement,
            @Nullable SourceCodeBuilder sourceCode,
            @Nonnull UrlBuilder urlBuilder,
            @Nonnull Verb verb,
            @Nonnull ServiceMultiplicity serviceMultiplicity
        ) {
            super(elementContext, macroElement, sourceCode);
            this.urlBuilder = Objects.requireNonNull(urlBuilder);
            this.verb = Objects.requireNonNull(verb);
            this.serviceMultiplicity = Objects.requireNonNull(serviceMultiplicity);
        }

        public void addCriteriaBuilder(
            @Nonnull String criteriaKeyword,
            @Nonnull AbstractCriteriaBuilder<?> criteriaBuilder
        ) {
            Objects.requireNonNull(criteriaKeyword);
            Objects.requireNonNull(criteriaBuilder);

            switch (criteriaKeyword) {
                case "criteria": {
                    if (this.criteria.isPresent()) {
                        throw new IllegalStateException();
                    }
                    this.criteria = Optional.of(criteriaBuilder);
                    return;
                }
                case "authorize": {
                    if (this.authorize.isPresent()) {
                        throw new IllegalStateException();
                    }
                    this.authorize = Optional.of(criteriaBuilder);
                    return;
                }
                case "validate": {
                    if (this.validate.isPresent()) {
                        throw new IllegalStateException();
                    }
                    this.validate = Optional.of(criteriaBuilder);
                    return;
                }
                case "conflict": {
                    if (this.conflict.isPresent()) {
                        throw new IllegalStateException();
                    }
                    this.conflict = Optional.of(criteriaBuilder);
                    return;
                }
                default: {
                    throw new AssertionError();
                }
            }
        }

        public void setProjectionDispatchBuilder(
            @Nonnull Optional<ServiceProjectionDispatchBuilder> projectionDispatchBuilder
        ) {
            this.projectionDispatchBuilder = Objects.requireNonNull(projectionDispatchBuilder);
        }

        public void setOrderByBuilder(@Nonnull Optional<OrderByBuilder> orderByBuilder) {
            this.orderByBuilder = Objects.requireNonNull(orderByBuilder);
        }

        @Nonnull
        @Override
        protected ServiceImpl buildUnsafe() {
            ServiceImpl service = new ServiceImpl(
                (ServiceDeclarationContext) this.elementContext,
                this.macroElement.map(ElementBuilder::getElement),
                this.sourceCode.build(),
                this.urlBuilder.getElement(),
                this.verb,
                this.serviceMultiplicity
            );

            Optional<ServiceProjectionDispatch> projectionDispatch =
                this.projectionDispatchBuilder.map(ElementBuilder::build);
            service.setProjectionDispatch(projectionDispatch);

            Optional<Criteria> queryCriteria = this.criteria.map(AbstractCriteriaBuilder::build);
            Optional<Criteria> authorizeCriteria = this.authorize.map(AbstractCriteriaBuilder::build);
            Optional<Criteria> validateCriteria = this.validate.map(AbstractCriteriaBuilder::build);
            Optional<Criteria> conflictCriteria = this.conflict.map(AbstractCriteriaBuilder::build);

            service.setQueryCriteria(queryCriteria);
            service.setAuthorizeCriteria(authorizeCriteria);
            service.setValidateCriteria(validateCriteria);
            service.setConflictCriteria(conflictCriteria);

            return service;
        }

        @Override
        protected void buildChildren() {
            Optional<OrderBy> orderBy = this.orderByBuilder.map(OrderByBuilder::build);
            this.element.setOrderBy(orderBy);
        }
    }
}
