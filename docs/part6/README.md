# Related Works

## Data mapping aggregators

Examples:

- Spark Catalyst / Spark SQL
- Hive

In Klass, the model, store, and mapping are all 1-1-1. These tools allow non-trivial mappings between the model and the store.

- Model: Clean domain model
- Store: A model still, but of the data store. Can be messy.
- Mapping: Which tables, columns, etc. map to which part of the clean model. How to perform joins, aggregations.

Systems that support non-trivial mappings tend to be read-only. The mappings are lossy transformations. For example, not every column may be mapped into the model. Or a property may be the result of an aggregation. So there's no obvious way to write through the model.

These systems are powerful analytical tools. Catalyst is a powerful query engine, with advanced planning and routing.

Read-write systems can't support complex mappings, but can rely on the data store for efficient query execution.

## OpenAPI

### Things to like

- [SwaggerHub](https://swagger.io/tools/swaggerhub/) is a site for "Hosted, Interactive API Documentation".
    - It's a great resource for developers to find and use a documented api.
    - You could imagine it as the foundation for a service discovery mechanism.

### Drawbacks

- The OpenAPI model is tightly bound to http.
    - It includes implementation details like numeric http codes.
    - It cannot represent other service types like gRPC without vendor extensions.
    - It cannot represent data layer concerns like keys, foreign keys, joins. It doesn't help with fetching data and serializing it to json.
    - It cannot represent UI concerns like display names.
- [Relationships are uni-directional](https://swagger.io/docs/specification/data-models/data-types/).
    - So a Question has answers, and Answer has a question, but nothing in the schema links these two properties.
    - This is fine for json; and after all OpenAPI doesn't help you at the data layer.
    - Bidirectional relationships are critical at the data layer, to know that a single foreign key `questionId` is used to perform the same join when navigating the relationship in either direction.
- Loose ecosystem of tools of varying quality.
    - The OpenAPI specification and core tools like the Swagger editor are maintained by core contributors.
    - The various code generators for different technologies have different maintainers and varying quality.
    - The Jersey 2 generator, which is the most popular http variant for Java, has quality problems.
        - Minor releases have produced breaking changes in both the code and the http api.
        - Exception handling isn't done in Jersey's idiomatic way.
        - Serializer plugins aren't done in Jersey's idiomatic way.
- The community's top contributors created a [hostile fork called openapi-generator](https://github.com/OpenAPITools/openapi-generator/blob/master/docs/qna.md).
    - Some of the issues above, like breaking changes, were mentioned in the rationale.

## Spring Boot / JHipster

Spring Boot builds on top of Spring. JHipster builds on top of Spring Boot.

The Spring Boot stack is probably the most spiritually similar related work. There's plenty to like about it, and some drawbacks that may justify not using it.

### Things to like

- JHipster includes JDL, a spiritually similar DSL.
- [JDL-Studio](https://start.jhipster.tech/jdl-studio/) is a web-based IDE with syntax-highlighting and live-updating read-only UML diagrams.
- Relationships in JDL are bidirectional.
- [Relationships can be annotated with a display field](https://www.jhipster.tech/jdl/#relationshipdeclaration), which is "the name of the field that should show up in select boxes." For example, in an auto-generated form to create Answers, there may be a select box for which Question it answers. It should display `title`, rather than `id`.
- Spring Data supports many data stores.

### Drawbacks

- Every entity (class) gets an `id` property, which is an auto-incrementing integer. There are no natural keys.
- Spring Data has limited support for audit-data. It can track the version number, last updated time, and last updated user. However, all previous versions are lost.
- Hibernate Envers is an extension that is meant to be non-lossy. However:
    - Envers is not integrated into Spring Boot / JHipster
    - Envers keeps audit data in separate audit tables with a completely different schema. It is difficult to recreate previous state of anything other than a single vertex in isolation.
    - Envers adds audit functionality, but doesn't disable Hibernate's destructive. You have to remember not to call update() and delete().

## GraphQL

[GraphQL](https://graphql.org/) is a query language for APIs. The ecosystem of frameworks that have developed around it can be considered related work.

GraphQL's query language is probably the closest thing in open source to Klass's projections.

### Things to like

- GraphQL's [query fields](https://graphql.org/learn/queries/#fields) are similar to Klass's read projections.
    - The nested structure exactly matches the structure of the returned json.
- [Fields can take arguments](https://graphql.org/learn/queries/#arguments).
    - This is another term for parameterized properties.
    - This is a powerful technique for refining relationships.
    - For example, we can refine the relationship between Question and Answer by declaring a field for a Question's answers by some user.

### Drawbacks

- [GraphQL's relationships](https://graphql.org/learn/schema/#object-types-and-fields) are uni-directional. So a Question has answers, and Answer has a question, but nothing in the schema links these two properties. This is fine for reads; and after all GraphQL started as just a query language. Bidirectional relationships are critical for writes. This is one reason why [dgraph's query language](https://docs.dgraph.io/master/query-language/) is GraphQL+-, which extends edge declarations with a [reverse edge](https://docs.dgraph.io/master/query-language/#reverse-edges).
- GraphQL makes it easy to express powerful queries for which there is no efficient execution plan.
    - For example, a multi-hop graph walk with predicates that narrow the walk on each hop.
    - The only possible implementation is to perform IO for every hop.
    - GraphQL was invented to stitch together microservice responses into a cohesive subgraph. It assumes that a "foreign key" in one json response will lead to a GET against another service.
- GraphQL's responses are always json. There's no notion of flattening. GraphQL cannot return csv, [GraphML](http://graphml.graphdrawing.org/), etc.
- Mutations feel second class. The mutation bodies conform to a schema, but you're on your own for performing the mutation.
- It's common to expose a /graphql/ endpoint over http. While not as dangerous as exposing a /sql/ endpoint, it gives complete control to clients by default. It requires extra effort to handle some cross-cutting concerns like recording meaningful metrics, rate limiting, data-level entitlements, etc.

## Other related works

- EdgeDB
- Relay
- dgraph
- FOAM Framework
- TextUML
