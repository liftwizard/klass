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

package cool.klass.model.meta.domain.service.url;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.AbstractElement;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.parameter.Parameter;
import cool.klass.model.meta.domain.api.service.Service;
import cool.klass.model.meta.domain.api.service.url.Url;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.parameter.ParameterImpl.ParameterBuilder;
import cool.klass.model.meta.domain.service.ServiceGroupImpl;
import cool.klass.model.meta.domain.service.ServiceGroupImpl.ServiceGroupBuilder;
import cool.klass.model.meta.domain.service.ServiceImpl.ServiceBuilder;
import cool.klass.model.meta.grammar.KlassParser.UrlDeclarationContext;
import org.eclipse.collections.api.list.ImmutableList;

public final class UrlImpl extends AbstractElement implements Url {

    @Nonnull
    private final ServiceGroupImpl serviceGroup;

    private ImmutableList<Element> urlPathSegments;
    private ImmutableList<Parameter> parameters;
    private ImmutableList<Parameter> queryParameters;
    private ImmutableList<Parameter> pathParameters;
    private ImmutableList<Service> services;

    private UrlImpl(
        @Nonnull UrlDeclarationContext elementContext,
        @Nonnull Optional<Element> macroElement,
        @Nullable SourceCode sourceCode,
        @Nonnull ServiceGroupImpl serviceGroup
    ) {
        super(elementContext, macroElement, sourceCode);
        this.serviceGroup = Objects.requireNonNull(serviceGroup);
    }

    @Nonnull
    @Override
    public UrlDeclarationContext getElementContext() {
        return (UrlDeclarationContext) super.getElementContext();
    }

    @Override
    @Nonnull
    public ServiceGroupImpl getServiceGroup() {
        return Objects.requireNonNull(this.serviceGroup);
    }

    @Override
    public ImmutableList<Element> getUrlPathSegments() {
        return Objects.requireNonNull(this.urlPathSegments);
    }

    private void setUrlPathSegments(@Nonnull ImmutableList<Element> urlPathSegments) {
        if (this.urlPathSegments != null) {
            throw new IllegalStateException();
        }
        this.urlPathSegments = Objects.requireNonNull(urlPathSegments);
    }

    @Override
    public ImmutableList<Parameter> getParameters() {
        return Objects.requireNonNull(this.parameters);
    }

    private void setParameters(@Nonnull ImmutableList<Parameter> urlParameters) {
        if (this.parameters != null) {
            throw new IllegalStateException();
        }
        this.parameters = Objects.requireNonNull(urlParameters);
    }

    @Override
    public ImmutableList<Parameter> getQueryParameters() {
        return Objects.requireNonNull(this.queryParameters);
    }

    private void setQueryParameters(@Nonnull ImmutableList<Parameter> queryParameters) {
        if (this.queryParameters != null) {
            throw new IllegalStateException();
        }
        this.queryParameters = Objects.requireNonNull(queryParameters);
    }

    @Override
    public ImmutableList<Parameter> getPathParameters() {
        return Objects.requireNonNull(this.pathParameters);
    }

    private void setPathParameters(@Nonnull ImmutableList<Parameter> pathParameters) {
        if (this.pathParameters != null) {
            throw new IllegalStateException();
        }
        this.pathParameters = Objects.requireNonNull(pathParameters);
    }

    @Override
    public ImmutableList<Service> getServices() {
        return Objects.requireNonNull(this.services);
    }

    private void setServices(@Nonnull ImmutableList<Service> services) {
        if (this.services != null) {
            throw new IllegalStateException();
        }
        this.services = Objects.requireNonNull(services);
    }

    public static final class UrlBuilder extends ElementBuilder<UrlImpl> {

        @Nonnull
        private final ServiceGroupBuilder serviceGroupBuilder;

        private ImmutableList<ElementBuilder<?>> pathSegmentBuilders;
        private ImmutableList<ParameterBuilder> parameterBuilders;
        private ImmutableList<ParameterBuilder> queryParameterBuilders;
        private ImmutableList<ParameterBuilder> pathParameterBuilders;
        private ImmutableList<ServiceBuilder> serviceBuilders;

        public UrlBuilder(
            @Nonnull UrlDeclarationContext elementContext,
            @Nonnull Optional<ElementBuilder<?>> macroElement,
            @Nullable SourceCodeBuilder sourceCode,
            @Nonnull ServiceGroupBuilder serviceGroupBuilder
        ) {
            super(elementContext, macroElement, sourceCode);
            this.serviceGroupBuilder = Objects.requireNonNull(serviceGroupBuilder);
        }

        public void setPathSegmentBuilders(@Nonnull ImmutableList<ElementBuilder<?>> pathSegmentBuilders) {
            this.pathSegmentBuilders = Objects.requireNonNull(pathSegmentBuilders);
        }

        public void setParameterBuilders(@Nonnull ImmutableList<ParameterBuilder> parameterBuilders) {
            this.parameterBuilders = Objects.requireNonNull(parameterBuilders);
        }

        public void setQueryParameterBuilders(@Nonnull ImmutableList<ParameterBuilder> queryParameterBuilders) {
            this.queryParameterBuilders = Objects.requireNonNull(queryParameterBuilders);
        }

        public void setPathParameterBuilders(@Nonnull ImmutableList<ParameterBuilder> pathParameterBuilders) {
            this.pathParameterBuilders = Objects.requireNonNull(pathParameterBuilders);
        }

        public void setServiceBuilders(@Nonnull ImmutableList<ServiceBuilder> serviceBuilders) {
            this.serviceBuilders = Objects.requireNonNull(serviceBuilders);
        }

        @Override
        @Nonnull
        protected UrlImpl buildUnsafe() {
            return new UrlImpl(
                (UrlDeclarationContext) this.elementContext,
                this.macroElement.map(ElementBuilder::getElement),
                this.sourceCode.build(),
                this.serviceGroupBuilder.getElement()
            );
        }

        @Override
        protected void buildChildren() {
            this.element.setUrlPathSegments(this.pathSegmentBuilders.collect(ElementBuilder::build));
            this.element.setQueryParameters(this.queryParameterBuilders.collect(ParameterBuilder::build));
            this.element.setPathParameters(this.pathParameterBuilders.collect(ParameterBuilder::getElement));
            this.element.setParameters(this.parameterBuilders.collect(ParameterBuilder::getElement));

            ImmutableList<Service> services = this.serviceBuilders.collect(ServiceBuilder::build);
            this.element.setServices(services);
        }
    }
}
