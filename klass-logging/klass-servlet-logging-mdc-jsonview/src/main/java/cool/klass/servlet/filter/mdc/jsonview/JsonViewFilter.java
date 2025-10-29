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

package cool.klass.servlet.filter.mdc.jsonview;

import java.util.Objects;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import cool.klass.model.meta.domain.api.projection.Projection;
import io.liftwizard.logging.slf4j.mdc.MultiMDCCloseable;

@Provider
@Priority(Priorities.USER - 10)
public class JsonViewFilter implements ContainerRequestFilter {

    private final Projection projection;

    public JsonViewFilter(Projection projection) {
        this.projection = Objects.requireNonNull(projection);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        MultiMDCCloseable mdc = (MultiMDCCloseable) requestContext.getProperty("mdc");

        mdc.put("klass.jsonView.projectionName", String.valueOf(this.projection));
        mdc.put("klass.jsonView.projectionClassifier", this.projection.getClassifier().getFullyQualifiedName());
    }
}
