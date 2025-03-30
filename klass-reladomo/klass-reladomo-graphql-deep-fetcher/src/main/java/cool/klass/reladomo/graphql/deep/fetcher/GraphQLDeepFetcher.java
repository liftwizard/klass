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

package cool.klass.reladomo.graphql.deep.fetcher;

import java.util.Objects;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.gs.fw.common.mithra.finder.RelatedFinder;
import com.gs.fw.finder.DomainList;
import com.gs.fw.finder.Navigation;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.SelectedField;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;

public final class GraphQLDeepFetcher {

    private static final Converter<String, String> UPPER_TO_LOWER_CAMEL = CaseFormat.UPPER_CAMEL.converterTo(
        CaseFormat.LOWER_CAMEL
    );

    private DomainModel domainModel;

    public DomainModel getDomainModel() {
        return this.domainModel;
    }

    public void setDomainModel(DomainModel domainModel) {
        this.domainModel = Objects.requireNonNull(domainModel);
    }

    public <T> void deepFetch(
        DomainList<T> result,
        String className,
        RelatedFinder<T> finderInstance,
        DataFetchingFieldSelectionSet selectionSet
    ) {
        Klass klass = this.domainModel.getClassByName(className);
        for (SelectedField selectedField : selectionSet.getFields()) {
            this.deepFetchSelectedField(result, klass, finderInstance, selectedField);
        }
    }

    private <T> void deepFetchSelectedField(
        DomainList<T> result,
        Klass klass,
        RelatedFinder<T> finderInstance,
        SelectedField selectedField
    ) {
        Objects.requireNonNull(finderInstance);
        Objects.requireNonNull(klass);

        String qualifiedName = selectedField.getQualifiedName();
        MutableList<String> fieldNamesNames = ArrayAdapter.adapt(qualifiedName.split("/"));
        MutableList<String> navigationNames = fieldNamesNames.take(fieldNamesNames.size() - 1);
        if (navigationNames.isEmpty()) {
            return;
        }

        RelatedFinder<T> currentFinder = finderInstance;
        Klass currentClass = klass;
        for (String navigationName : navigationNames) {
            AssociationEnd associationEnd = currentClass.getDeclaredAssociationEndByName(navigationName);
            while (associationEnd == null) {
                Klass superClass = currentClass.getSuperClass().get();
                String superClassNavigationName = UPPER_TO_LOWER_CAMEL.convert(superClass.getName()) + "SuperClass";
                currentClass = superClass;
                currentFinder = (RelatedFinder<T>) currentFinder.getRelationshipFinderByName(superClassNavigationName);
                Objects.requireNonNull(currentFinder);
                associationEnd = currentClass.getDeclaredAssociationEndByName(navigationName);
            }

            currentClass = associationEnd.getType();
            currentFinder = currentFinder.getRelationshipFinderByName(navigationName);
            Objects.requireNonNull(currentFinder);
        }
        if (!(currentFinder instanceof Navigation)) {
            throw new IllegalStateException("Expected a navigation, but got: " + currentFinder);
        }
        Navigation<T> navigation = (Navigation<T>) currentFinder;
        result.deepFetch(navigation);
    }
}
