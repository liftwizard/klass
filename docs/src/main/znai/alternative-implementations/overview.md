# Alternative implementation choices

## One-time generation

Instead of a maven archetype, initial project creation could be done with:

- [Yeoman](http://yeoman.io/).
- A webapp that produces a zip for download, like [Spring Initializr](https://start.spring.io/) for Spring Boot.
- A custom CLI, possibly in python.

## Metamodel

Instead of a new metamodel, Klass could use an existing metamodel like [Eclipse Modeling Framework's ECore](https://www.eclipse.org/modeling/emf/).

ECore is a fairly complete implementation of UML, with all its warts. ECore also has enhancements to allow it to represent anything expressible in Java, including Generics.

Klass's metamodel is a fairly narrow subset of UML. It addresses some weaknesses in UML, such as the lack of a type hierarchy between the various types of properties.

Klass's metamodel also includes projections and services.

## Metamodel language

Instead of a new DSL, the model could be expressed using:

- [OpenAPI vendor extensions](https://swagger.io/docs/specification/openapi-extensions/) (maybe, unproven)
- A data format that's not a DSL, like JSON, YML, or XML, conforming to a JSON schema or XSD.
- Annotations inside a Java or Python program.
- A graphical language like UML.

## Data Store

Rather than a relational database, the data could be stored in:

- A specific non-relational store, like a documentation database or a graph database.
- An abstract DataStore interface, with multiple implementations.

## Audit Data

Rather than storing phased-out data in the same tables as active data, they could be stored in separate audit tables.

This would improve the performance of queries for active data. It would complicate querying for previous versions, especially when those versions have relationships to data that is still active.

## Relationship criteria

The docs show a syntax for relationship criteria that is similar to a sql join.

```klass
relationship this.id == Answer.questionId
```

It's possible to use a syntax where a property is annotated as a foreign key instead.

It's possible to use a syntax where a foreign key tuple is listed instead.

However, parameterized properties would need a more powerful syntax.

## Service syntax

The service syntax is url-centric. If we want to support both http and rpc, we may want a more flexible syntax.
