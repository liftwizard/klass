/*
 * Copyright 2026 Craig Motlin
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

package cool.klass.graphql.reladomo.finder.fetcher;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.gs.fw.common.mithra.finder.AbstractRelatedFinder;
import com.gs.fw.common.mithra.finder.Operation;
import com.gs.fw.common.mithra.finder.orderby.OrderBy;
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
import io.liftwizard.reladomo.graphql.operation.GraphQLQueryToOperationConverter;
import io.liftwizard.reladomo.graphql.operation.LiftwizardGraphQLContextException;
import io.liftwizard.reladomo.graphql.orderby.GraphQLQueryToOrderByConverter;

public class ReladomoFinderDataFetcher<T> implements DataFetcher<List<T>> {

	private final DomainModel domainModel;
	private final ReladomoDataStore dataStore;
	private final AbstractRelatedFinder<T, ?, ?, ?, ?> finder;

	public ReladomoFinderDataFetcher(
		DomainModel domainModel,
		ReladomoDataStore dataStore,
		AbstractRelatedFinder<T, ?, ?, ?, ?> finder
	) {
		this.domainModel = Objects.requireNonNull(domainModel);
		this.dataStore = Objects.requireNonNull(dataStore);
		this.finder = Objects.requireNonNull(finder);
	}

	@Timed
	@Metered
	@ExceptionMetered
	@Override
	public List<T> get(DataFetchingEnvironment environment) {
		Map<String, Object> arguments = environment.getArguments();
		Object inputOperation = arguments.get("operation");
		Operation operation = this.getOperation((Map<?, ?>) inputOperation);
		Object inputOrderBy = arguments.get("orderBy");
		Optional<OrderBy> orderBys = this.getOrderBys((List<Map<String, ?>>) inputOrderBy);
		DomainList<T> result = (DomainList<T>) this.finder.findMany(operation);
		orderBys.ifPresent(result::setOrderBy);

		Klass klass = this.domainModel.getClassByName("Enumeration");
		var treeGraphqlConverter = new ReladomoTreeGraphqlConverter(this.domainModel);

		var deepFetcher = new ReladomoTreeNodeDeepFetcherListener(this.dataStore, (DomainList) result, klass);
		RootReladomoTreeNode rootReladomoTreeNode = treeGraphqlConverter.convert(klass, environment.getSelectionSet());

		deepFetcher.enterRoot(rootReladomoTreeNode);

		return result;
	}

	public Operation getOperation(Map<?, ?> inputOperation) {
		try {
			var converter = new GraphQLQueryToOperationConverter();
			return converter.convert(this.finder, inputOperation);
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
