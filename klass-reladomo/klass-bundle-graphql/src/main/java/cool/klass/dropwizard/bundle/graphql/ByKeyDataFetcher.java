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

package cool.klass.dropwizard.bundle.graphql;

import java.util.List;
import java.util.Objects;

import com.gs.fw.finder.DomainList;
import cool.klass.data.store.reladomo.ReladomoDataStore;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.reladomo.tree.RootReladomoTreeNode;
import cool.klass.model.reladomo.tree.converter.graphql.ReladomoTreeGraphqlConverter;
import cool.klass.reladomo.tree.deep.fetcher.ReladomoTreeNodeDeepFetcherListener;
import cool.klass.reladomo.tree.serializer.ReladomoTreeObjectToDTOSerializerListener;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;

public class ByKeyDataFetcher implements DataFetcher<Object> {

    private final Klass klass;
    private final ReladomoDataStore dataStore;
    private final ReladomoTreeGraphqlConverter reladomoTreeGraphqlConverter;

    public ByKeyDataFetcher(
        Klass klass,
        ReladomoDataStore dataStore,
        ReladomoTreeGraphqlConverter reladomoTreeGraphqlConverter
    ) {
        this.klass = Objects.requireNonNull(klass);
        this.dataStore = Objects.requireNonNull(dataStore);
        this.reladomoTreeGraphqlConverter = Objects.requireNonNull(reladomoTreeGraphqlConverter);
    }

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        MutableMap<DataTypeProperty, Object> keys = this.klass.getKeyProperties()
            .tap((keyProperty) -> this.assertEnvironmentContains(environment, keyProperty))
            .toMap((each) -> each, (keyProperty) -> environment.getArgument(keyProperty.getName()));

        List<Object> result = this.dataStore.findByKeyReturningList(this.klass, keys);

        DataFetchingFieldSelectionSet selectionSet = environment.getSelectionSet();
        RootReladomoTreeNode rootReladomoTreeNode = this.reladomoTreeGraphqlConverter.convert(this.klass, selectionSet);

        var deepFetcherListener = new ReladomoTreeNodeDeepFetcherListener(
            this.dataStore,
            (DomainList) result,
            this.klass
        );
        rootReladomoTreeNode.walk(deepFetcherListener);

        var serializerVisitor = new ReladomoTreeObjectToDTOSerializerListener(
            this.dataStore,
            (DomainList) result,
            this.klass
        );
        rootReladomoTreeNode.toManyAwareWalk(serializerVisitor);

        MutableList<Object> resultList = serializerVisitor.getResult();
        if (resultList.isEmpty()) {
            return null;
        }
        if (resultList.size() > 1) {
            throw new IllegalStateException("Expected 1 result, but got " + resultList.size());
        }
        return resultList.getOnly();
    }

    private void assertEnvironmentContains(DataFetchingEnvironment environment, DataTypeProperty keyProperty) {
        if (environment.containsArgument(keyProperty.getName())) {
            return;
        }

        String detailMessage = "Argument " + keyProperty.getName() + " is required for " + this.klass.getName();
        throw new IllegalArgumentException(detailMessage);
    }
}
