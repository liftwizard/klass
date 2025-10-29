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

package cool.klass.graphql.reladomo.operation.fetcher;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.gs.fw.common.mithra.finder.Operation;
import com.gs.fw.common.mithra.finder.RelatedFinder;
import com.gs.fw.finder.DomainList;
import cool.klass.data.store.reladomo.ReladomoDataStore;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.reladomo.tree.RootReladomoTreeNode;
import cool.klass.model.reladomo.tree.converter.graphql.ReladomoTreeGraphqlConverter;
import cool.klass.reladomo.tree.deep.fetcher.ReladomoTreeNodeDeepFetcherListener;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.liftwizard.graphql.exception.LiftwizardGraphQLException;
import io.liftwizard.model.reladomo.operation.compiler.ReladomoOperationCompiler;
import org.eclipse.collections.api.factory.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReladomoOperationDataFetcher<T> implements DataFetcher<List<T>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReladomoOperationDataFetcher.class);

    private final DomainModel domainModel;
    private final ReladomoDataStore dataStore;
    private final Klass klass;
    private final RelatedFinder<?> finder;

    public ReladomoOperationDataFetcher(
        DomainModel domainModel,
        ReladomoDataStore dataStore,
        String className,
        RelatedFinder<?> relatedFinder
    ) {
        this.domainModel = Objects.requireNonNull(domainModel);
        this.dataStore = Objects.requireNonNull(dataStore);
        this.klass = this.domainModel.getClassByName(className);
        this.finder = Objects.requireNonNull(relatedFinder);
    }

    @Timed
    @Metered
    @ExceptionMetered
    @Override
    public List<T> get(DataFetchingEnvironment environment) {
        Map<String, Object> arguments = environment.getArguments();
        String inputOperation = (String) arguments.get("operation");
        Operation operation = this.compileOperation(this.finder, inputOperation);

        LOGGER.debug("Executing operation: {}", operation);

        DomainList<T> result = (DomainList<T>) this.finder.findMany(operation);
        var treeGraphqlConverter = new ReladomoTreeGraphqlConverter(this.domainModel);

        var deepFetcher = new ReladomoTreeNodeDeepFetcherListener(this.dataStore, (DomainList) result, this.klass);
        RootReladomoTreeNode rootReladomoTreeNode = treeGraphqlConverter.convert(
            this.klass,
            environment.getSelectionSet()
        );

        deepFetcher.enterRoot(rootReladomoTreeNode);

        return result;
    }

    private Operation compileOperation(RelatedFinder<?> relatedFinder, String inputOperation) {
        try {
            var compiler = new ReladomoOperationCompiler();
            return compiler.compile(relatedFinder, inputOperation);
        } catch (RuntimeException e) {
            throw new LiftwizardGraphQLException(e.getMessage(), Lists.immutable.with(inputOperation), e);
        }
    }
}
