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

package cool.klass.model.meta.domain.api;

import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.projection.Projection;
import cool.klass.model.meta.domain.api.service.ServiceGroup;
import org.eclipse.collections.api.list.ImmutableList;

public interface DomainModel {
    Optional<Klass> getUserClass();

    @Nonnull
    ImmutableList<TopLevelElement> getTopLevelElements();

    @Nonnull
    ImmutableList<Enumeration> getEnumerations();

    @Nonnull
    ImmutableList<Classifier> getClassifiers();

    @Nonnull
    ImmutableList<Interface> getInterfaces();

    @Nonnull
    ImmutableList<Klass> getClasses();

    @Nonnull
    ImmutableList<Association> getAssociations();

    @Nonnull
    ImmutableList<Projection> getProjections();

    @Nonnull
    ImmutableList<ServiceGroup> getServiceGroups();

    TopLevelElement getTopLevelElementByName(String topLevelElementName);

    Enumeration getEnumerationByName(String name);

    Classifier getClassifierByName(String name);

    Interface getInterfaceByName(String name);

    Klass getClassByName(String name);

    Association getAssociationByName(String name);

    Projection getProjectionByName(String name);
}
