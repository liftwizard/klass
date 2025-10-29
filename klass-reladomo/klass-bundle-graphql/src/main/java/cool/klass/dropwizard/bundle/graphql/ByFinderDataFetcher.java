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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.gs.fw.common.mithra.MithraList;
import com.gs.fw.common.mithra.finder.AbstractRelatedFinder;
import com.gs.fw.common.mithra.finder.Operation;
import com.gs.fw.common.mithra.finder.RelatedFinder;
import com.gs.fw.common.mithra.finder.orderby.OrderBy;
import cool.klass.data.store.reladomo.ReladomoDataStore;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.reladomo.tree.RootReladomoTreeNode;
import cool.klass.model.reladomo.tree.converter.graphql.ReladomoTreeGraphqlConverter;
import cool.klass.reladomo.tree.deep.fetcher.ReladomoTreeNodeDeepFetcherListener;
import cool.klass.reladomo.tree.serializer.ReladomoTreeObjectToDTOSerializerListener;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import io.liftwizard.graphql.exception.LiftwizardGraphQLException;
import io.liftwizard.reladomo.graphql.operation.GraphQLQueryToOperationConverter;
import io.liftwizard.reladomo.graphql.operation.LiftwizardGraphQLContextException;
import io.liftwizard.reladomo.graphql.orderby.GraphQLQueryToOrderByConverter;

public class ByFinderDataFetcher implements DataFetcher<Object> {

    private final Klass klass;
    private final ReladomoDataStore dataStore;
    private final ReladomoTreeGraphqlConverter reladomoTreeGraphqlConverter;

    private final RelatedFinder<?> finder;

    public ByFinderDataFetcher(
        Klass klass,
        ReladomoDataStore dataStore,
        ReladomoTreeGraphqlConverter reladomoTreeGraphqlConverter
    ) {
        this.klass = Objects.requireNonNull(klass);
        this.dataStore = Objects.requireNonNull(dataStore);
        this.reladomoTreeGraphqlConverter = Objects.requireNonNull(reladomoTreeGraphqlConverter);

        this.finder = this.dataStore.getRelatedFinder(klass);
    }

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        Map<String, Object> arguments = environment.getArguments();
        Object inputOperation = arguments.get("operation");
        Operation operation = this.getOperation((Map<?, ?>) inputOperation);
        Object inputOrderBy = arguments.get("orderBy");
        Optional<OrderBy> orderBys = this.getOrderBys((List<Map<String, ?>>) inputOrderBy);
        MithraList<?> result = this.finder.findMany(operation);
        orderBys.ifPresent(result::setOrderBy);

        DataFetchingFieldSelectionSet selectionSet = environment.getSelectionSet();
        RootReladomoTreeNode rootReladomoTreeNode = this.reladomoTreeGraphqlConverter.convert(this.klass, selectionSet);

        var deepFetcherListener = new ReladomoTreeNodeDeepFetcherListener(this.dataStore, result, this.klass);
        rootReladomoTreeNode.walk(deepFetcherListener);

        var serializerVisitor = new ReladomoTreeObjectToDTOSerializerListener(this.dataStore, result, this.klass);
        rootReladomoTreeNode.toManyAwareWalk(serializerVisitor);

        return serializerVisitor.getResult();
    }

    public Operation getOperation(Map<?, ?> inputOperation) {
        try {
            var converter = new GraphQLQueryToOperationConverter();
            return converter.convert((AbstractRelatedFinder) this.finder, inputOperation);
        } catch (LiftwizardGraphQLContextException e) {
            throw new LiftwizardGraphQLException(e.getMessage(), e.getContext(), e);
        }
    }

    public Optional<OrderBy> getOrderBys(List<Map<String, ?>> inputOrderBy) {
        try {
            return GraphQLQueryToOrderByConverter.convertOrderByList(this.finder, inputOrderBy);
        } catch (LiftwizardGraphQLContextException e) {
            throw new LiftwizardGraphQLException(e.getMessage(), e.getContext(), e);
        }
    }
}
