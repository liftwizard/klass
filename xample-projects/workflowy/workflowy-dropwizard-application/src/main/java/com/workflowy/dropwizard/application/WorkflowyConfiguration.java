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

package com.workflowy.dropwizard.application;

import javax.annotation.Nonnull;
import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smoketurner.dropwizard.graphql.GraphQLFactory;
import cool.klass.dropwizard.configuration.AbstractKlassConfiguration;
import io.liftwizard.dropwizard.configuration.graphql.GraphQLFactoryProvider;

public class WorkflowyConfiguration extends AbstractKlassConfiguration implements GraphQLFactoryProvider {

    @Nonnull
    private @Valid GraphQLFactory graphQL = new GraphQLFactory();

    @Override
    @Nonnull
    @JsonProperty("graphQL")
    public GraphQLFactory getGraphQLFactory() {
        return this.graphQL;
    }

    @JsonProperty("graphQL")
    public void setGraphQLFactory(@Nonnull GraphQLFactory factory) {
        this.graphQL = factory;
    }
    // TODO: implement service configuration
}
