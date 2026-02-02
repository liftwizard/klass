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

package cool.klass.dropwizard.bundle.graphql;

import java.util.Map;
import java.util.Objects;

import com.gs.fw.common.mithra.MithraList;
import com.gs.fw.common.mithra.finder.Operation;
import com.gs.fw.common.mithra.finder.RelatedFinder;
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
import io.liftwizard.model.reladomo.operation.compiler.ReladomoOperationCompiler;
import org.eclipse.collections.api.factory.Lists;

public class ByOperationDataFetcher implements DataFetcher<Object> {

	private final Klass klass;
	private final ReladomoDataStore dataStore;
	private final ReladomoTreeGraphqlConverter reladomoTreeGraphqlConverter;

	private final RelatedFinder<?> finder;

	public ByOperationDataFetcher(
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
		String inputOperation = (String) arguments.get("operation");
		Operation operation = this.compileOperation(this.finder, inputOperation);
		MithraList<?> result = this.finder.findMany(operation);

		DataFetchingFieldSelectionSet selectionSet = environment.getSelectionSet();
		RootReladomoTreeNode rootReladomoTreeNode = this.reladomoTreeGraphqlConverter.convert(this.klass, selectionSet);

		var deepFetcherListener = new ReladomoTreeNodeDeepFetcherListener(this.dataStore, result, this.klass);
		rootReladomoTreeNode.walk(deepFetcherListener);

		var serializerVisitor = new ReladomoTreeObjectToDTOSerializerListener(this.dataStore, result, this.klass);
		rootReladomoTreeNode.toManyAwareWalk(serializerVisitor);

		return serializerVisitor.getResult();
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
