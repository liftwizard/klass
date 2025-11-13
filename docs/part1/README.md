## Scope

This document covers long-term vision. A shorter-term, **minimal-viable-product** is discussed [here](../part5/README.md).

This covers one set of implementation choices. **Alternatives** are discussed [here](../part5/README.md).

Business value, rationale, justification are discussed [here](TODO).

**Related works** are discussed [here](../part6/README.md).

Internal design is discussed [here](TODO).

## Running example

To demonstrate the framework's capabilities, we will walk through building a realistic subset of Stack Overflow functionality.

## Getting started

First, run the maven archetype.

```bash
mvn archetype:generate \
  -DarchetypeGroupId=com.klass \
  -DarchetypeArtifactId=klass-maven-archetype \
  -DinteractiveMode=false \
  -DgroupId=com.stackoverflow \
  -DartifactId=stackoverflow \
  -Dversion=1.0.0 \
  -Dpackage=com.stackoverflow \
  -Dname=StackOverflow
```

This command creates a new project in `./stackoverflow/`. The project compiles and runs, but doesn't yet do anything interesting.

## Model

The model was generated in `./stackoverflow/stackoverflow-domain-model/src/main/resources/com/stackoverflow/stackoverflow.klass`. It is nearly empty except for the package declaration.

An Klass model consists of classes, enumerations, associations, projections, and services.

The **classes**, **enumerations**, and **associations** make up the data model. From the data model, Klass generates a database schema, POJOs, DTOs, example data, etc. We will define the `Question` and `Answer` classes, as well as the one-to-many association between questions and answers.

**Services** are traditional http or rpc services. We will define a service to get a `Question` by its id.

**Projections** map services to data. They answer how much data a service reads or writes; conceptually how far the service walks through the object graph. For example, when we fetch a `Question` by its id, should we return the title and the body, or just the title? Should we include the question's answers in the response?
