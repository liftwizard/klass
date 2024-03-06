package io.liftwizard.reladomo.graphql.operation.test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.gs.fw.common.mithra.finder.AbstractRelatedFinder;
import com.gs.fw.common.mithra.finder.Operation;
import com.gs.fw.common.mithra.finder.orderby.OrderBy;
import cool.klass.xample.coverage.OwnedNaturalOneToManySourceFinder;
import cool.klass.xample.coverage.PropertiesOptionalFinder;
import cool.klass.xample.coverage.PropertiesRequiredFinder;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.liftwizard.graphql.reladomo.finder.fetcher.ReladomoFinderDataFetcher;
import io.liftwizard.graphql.scalar.temporal.GraphQLLocalDateScalar;
import io.liftwizard.graphql.scalar.temporal.GraphQLTemporalScalar;
import io.liftwizard.junit.rule.log.marker.LogMarkerTestRule;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TestRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;

public class GraphQLQueryToOperationConverterTest
{
    @Rule
    public final TestRule logMarkerTestRule = new LogMarkerTestRule();

    @Rule
    public final ErrorCollector errorCollector = new ErrorCollector();

    @Test
    public void convertQueries()
    {
        //language=GraphQL
        String query = """
                {
                  propertiesOptionalByFinder(
                    operation: {
                      optionalBoolean: { eq: true }
                      optionalLong: { eq: 1 }
                      optionalFloat: { eq: 1.0 }
                      optionalDouble: { eq: 1.0 }
                      optionalInteger: { eq: 1 }
                      optionalString: { lower: { startsWith: "a" } }
                      version: { number: { eq: 1 } }
                    }
                    orderBy: [
                      {
                        attribute: {
                          optionalBoolean: {}
                          optionalLong: {}
                          optionalFloat: {}
                          optionalDouble: {}
                        }
                        direction: DESCENDING
                      }
                      {
                        attribute: {
                          optionalInteger: {}
                          optionalString: {}
                          version: { number: {} }
                        }
                      }
                    ]
                  ) {
                    propertiesOptionalId
                    optionalString
                    optionalInteger
                    optionalLong
                    optionalDouble
                    optionalFloat
                    optionalBoolean
                    optionalInstant
                    optionalLocalDate
                    systemFrom
                    systemTo
                    createdBy {
                        userId
                    }
                    createdOn
                    lastUpdatedBy {
                        userId
                    }
                    version {
                      number
                    }
                  }
                }
                """;

        this.assertCompiles(query);
    }

    // TODO invalidDateFormat

    @Test
    public void nullityOperation()
    {
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalBoolean: { eq: null } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInteger: { eq: null } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLong: { eq: null } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalFloat: { eq: null } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalDouble: { eq: null } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLocalDate: { eq: null } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInstant: { eq: null } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { eq: null } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { system: { eq: null } }) { propertiesOptionalId } }");

        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalBoolean: { notEq: null } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInteger: { notEq: null } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLong: { notEq: null } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalFloat: { notEq: null } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalDouble: { notEq: null } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLocalDate: { notEq: null } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInstant: { notEq: null } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { notEq: null } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { system: { notEq: null } }) { propertiesOptionalId } }");
    }

    @Test
    public void equalsEdgePointOperation()
    {
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { system: { equalsEdgePoint: {} } }) { propertiesOptionalId } }");
    }

    // TODO numberFormats()

    @Test
    public void equalityOperation()
    {
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalBoolean: { eq: true } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInteger: { eq: 4 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLong: { eq: 5000000000 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalFloat: { eq: 6.6 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalDouble: { eq: 7.7 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLocalDate: { eq: \"2010-12-31\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInstant: { eq: \"2010-12-31T23:59:00.0Z\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { eq: \"Value\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { system: { eq: \"2010-12-31T23:59:00.0Z\" } }) { propertiesOptionalId } }");

        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalBoolean: { notEq: true } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInteger: { notEq: 4 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLong: { notEq: 5000000000 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalFloat: { notEq: 6.6 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalDouble: { notEq: 7.7 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLocalDate: { notEq: \"2010-12-31\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInstant: { notEq: \"2010-12-31T23:59:00.0Z\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { notEq: \"Value\" } }) { propertiesOptionalId } }");
    }

    @Test
    public void inequalityOperation()
    {
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInteger: { greaterThan: 4 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLong: { greaterThan: 5000000000 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalFloat: { greaterThan: 6.6 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalDouble: { greaterThan: 7.7 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLocalDate: { greaterThan: \"2010-12-31\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInstant: { greaterThan: \"2010-12-31T23:59:00.0Z\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { greaterThan: \"Value\" } }) { propertiesOptionalId } }");

        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInteger: { greaterThanEquals: 4 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLong: { greaterThanEquals: 5000000000 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalFloat: { greaterThanEquals: 6.6 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalDouble: { greaterThanEquals: 7.7 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLocalDate: { greaterThanEquals: \"2010-12-31\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInstant: { greaterThanEquals: \"2010-12-31T23:59:00.0Z\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { greaterThanEquals: \"Value\" } }) { propertiesOptionalId } }");

        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInteger: { lessThan: 4 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLong: { lessThan: 5000000000 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalFloat: { lessThan: 6.6 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalDouble: { lessThan: 7.7 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLocalDate: { lessThan: \"2010-12-31\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInstant: { lessThan: \"2010-12-31T23:59:00.0Z\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { lessThan: \"Value\" } }) { propertiesOptionalId } }");

        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInteger: { lessThanEquals: 4 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLong: { lessThanEquals: 5000000000 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalFloat: { lessThanEquals: 6.6 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalDouble: { lessThanEquals: 7.7 } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLocalDate: { lessThanEquals: \"2010-12-31\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInstant: { lessThanEquals: \"2010-12-31T23:59:00.0Z\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { lessThanEquals: \"Value\" } }) { propertiesOptionalId } }");
    }

    @Test
    public void stringLikeOperations()
    {
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { endsWith: \"Value\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { contains: \"Value\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { startsWith: \"Value\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { wildCardEquals: \"Value?\" } }) { propertiesOptionalId } }");

        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { notEndsWith: \"Value\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { notContains: \"Value\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { notStartsWith: \"Value\" } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { wildCardNotEquals: \"Value?\" } }) { propertiesOptionalId } }");
    }

    @Test
    public void stringDerivedAttributes()
    {
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { lower: { eq: \"Value\" } } }) { propertiesOptionalId } }");

        // TODO substring
    }

    @Test
    public void numberDerivedAttributes()
    {
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInteger: { abs: { eq: 1 } } }) { propertiesOptionalId } }");
    }

    @Test
    public void instantDerivedAttributes()
    {
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInstant: { year: { eq: 1999 } } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInstant: { month: { eq: 12 } } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInstant: { dayOfMonth: { eq: 31 } } }) { propertiesOptionalId } }");

        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLocalDate: { year: { eq: 1999 } } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLocalDate: { month: { eq: 12 } } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLocalDate: { dayOfMonth: { eq: 31 } } }) { propertiesOptionalId } }");
    }

    @Test
    public void inOperation()
    {
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalBoolean: { in: [true, false] } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInteger: { in: [4, 5] } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLong: { in: [5000000000, 6000000000] } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalFloat: { in: [6.6, 7.7] } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalDouble: { in: [7.7, 8.8] } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLocalDate: { in: [\"2010-12-31\", \"2011-01-01\", null] } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInstant: { in: [\"2010-12-31T23:59:00.0Z\", \"2011-01-01T23:59:00.0Z\", null] } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { in: [\"Value\", \"Value2\", null] } }) { propertiesOptionalId } }");

        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalBoolean: { notIn: [true, false] } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInteger: { notIn: [4, 5] } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLong: { notIn: [5000000000, 6000000000] } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalFloat: { notIn: [6.6, 7.7] } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalDouble: { notIn: [7.7, 8.8] } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalLocalDate: { notIn: [\"2010-12-31\", \"2011-01-01\", null] } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalInstant: { notIn: [\"2010-12-31T23:59:00.0Z\", \"2011-01-01T23:59:00.0Z\", null] } }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { optionalString: { notIn: [\"Value\", \"Value2\", null] } }) { propertiesOptionalId } }");
    }

    @Test
    public void relationshipNavigation()
    {
        this.assertCompiles(
                "{ ownedNaturalOneToManySourceByFinder(operation: { targets: { exists: {} } }) { value } }");
        this.assertCompiles(
                "{ ownedNaturalOneToManySourceByFinder(operation: { targets: { notExists: {} } }) { value } }");
        this.assertCompiles(
                "{ ownedNaturalOneToManySourceByFinder(operation: { targets: { recursiveNotExists: {} } }) { value } }");

        this.assertCompiles(
                "{ ownedNaturalOneToManySourceByFinder(operation: { targets: { notExists: { source: { value: { eq: \"Value\" } } } } }) { value } }");
        this.assertCompiles(
                "{ ownedNaturalOneToManySourceByFinder(operation: { targets: { recursiveNotExists: { source: { value: { eq: \"Value\" } } } } }) { value } }");
    }

    @Test
    public void conjunctionOperations()
    {
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { AND: [{ optionalBoolean: { eq: true } }, { optionalInteger: { eq: 4 } }] }) { propertiesOptionalId } }");
        this.assertCompiles(
                "{ propertiesOptionalByFinder(operation: { OR: [{ optionalBoolean: { eq: true } }, { optionalInteger: { eq: 4 } }] }) { propertiesOptionalId } }");
    }

    private void assertCompiles(String query)
    {
        RuntimeWiring runtimeWiring = this.getRuntimeWiring();

        TypeDefinitionRegistry typeRegistry = this.getRegistry();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema   graphQLSchema   = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);

        GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema).build();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(query).build();

        ExecutionResult executionResult = graphQL.execute(executionInput);

        Object             data   = executionResult.getData();
        List<GraphQLError> errors = executionResult.getErrors();
        this.errorCollector.checkThat(Iterate.makeString(errors), errors, emptyIterable());
    }

    private RuntimeWiring getRuntimeWiring()
    {
        return RuntimeWiring.newRuntimeWiring()
                .scalar(new GraphQLTemporalScalar("Instant"))
                .scalar(new GraphQLTemporalScalar("TemporalInstant"))
                .scalar(new GraphQLTemporalScalar("TemporalRange"))
                .scalar(Scalars.GraphQLLong)
                .scalar(new GraphQLLocalDateScalar())

                .type(
                        "Query",
                        typeWiring -> typeWiring
                                .dataFetcher(
                                        "propertiesRequiredByFinder",
                                        new FakeReladomoFinderDataFetcher<>(PropertiesRequiredFinder.getFinderInstance()))
                                .dataFetcher(
                                        "propertiesOptionalByFinder",
                                        new FakeReladomoFinderDataFetcher<>(PropertiesOptionalFinder.getFinderInstance()))
                                .dataFetcher(
                                        "ownedNaturalOneToManySourceByFinder",
                                        new FakeReladomoFinderDataFetcher<>(OwnedNaturalOneToManySourceFinder.getFinderInstance())))
                .build();
    }

    @Nonnull
    private TypeDefinitionRegistry getRegistry()
    {
        var typeRegistry          = this.getRegistry("/cool/klass/xample/coverage/graphql/schema/GraphQLSchema.graphqls");
        var attributeTypeRegistry = this.getRegistry("/io/liftwizard/graphql/schema/attribute/ReladomoAttribute.graphqls");
        var queryTypeRegistry     = this.getRegistry("/cool/klass/xample/coverage/graphql/schema/query/GraphQLQuerySchema.graphqls");
        var findersTypeRegistry   = this.getRegistry("/cool/klass/xample/coverage/graphql/schema/finder/GraphQLFinders.graphqls");
        typeRegistry.merge(attributeTypeRegistry);
        typeRegistry.merge(queryTypeRegistry);
        typeRegistry.merge(findersTypeRegistry);
        return typeRegistry;
    }

    private TypeDefinitionRegistry getRegistry(String resourceName)
    {
        InputStream result = this.getClass().getResourceAsStream(resourceName);
        Objects.requireNonNull(result, resourceName);
        SchemaParser schemaParser = new SchemaParser();
        return schemaParser.parse(result);
    }

    private static final class FakeReladomoFinderDataFetcher<T>
            extends ReladomoFinderDataFetcher<T>
    {
        private FakeReladomoFinderDataFetcher(AbstractRelatedFinder<T, ?, ?, ?, ?> finder)
        {
            super(finder);
        }

        @Override
        public List<T> get(DataFetchingEnvironment environment)
        {
            Map<String, Object> arguments      = environment.getArguments();
            Object              inputOperation = arguments.get("operation");
            Object              inputOrderBy   = arguments.get("orderBy");

            Operation            operation     = this.getOperation((Map<?, ?>) inputOperation);
            List<Map<String, ?>> inputOrderByList = (List<Map<String, ?>>) inputOrderBy;
            Optional<OrderBy>    orderBy       = this.getOrderBys(inputOrderByList);
            assertThat(operation, notNullValue());
            if (!inputOrderByList.isEmpty())
            {
                assertTrue(orderBy.isPresent());
            }
            return Lists.mutable.empty();
        }
    }
}
