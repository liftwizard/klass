# Klass: Technical Documentation

## 1. Introduction: What is Klass?

Klass is a rapid application development framework for Java with first-class, built-in support for temporal data. It enables developers to design, build, and deploy data-centric applications with minimal boilerplate.

The core philosophy of Klass is **model-driven design**. Developers describe their application's domain model in a dedicated Domain-Specific Language (DSL). The Klass compiler then uses this model to generate:

- A database schema with temporal milestoning.

- An Object-Relational Mapping (ORM) layer.

- Data Transfer Objects (DTOs).

- JSON serializers and deserializers.

- A full-featured RESTful API that includes:
    - Endpoints generated directly from `service` definitions in the DSL.

    - Automatic support for temporal parameters like `?asof={time}`.

    - Special developer endpoints, including a dynamic query service for data exploration and meta-services for model introspection.

### Motivation

The motivation for Klass is to provide robust support for temporal data by eliminating destructive writes. With traditional CRUD frameworks, `UPDATE` and `DELETE` operations destroy historical records. Some frameworks have add-ons to enable temporal features, but don’t eliminate destructive writes, making a true temporal model impossible to implement reliably. Eliminating destructive writes enables "as-of" queries for any point in time.

Inspired by years of experience with closed-source temporal frameworks, Klass was created to make these powerful features accessible.

## 2. The Developer Workflow

To develop an application with Klass:

1. **Scaffold the Project**: A developer begins by using the Klass project generator to create a new application. This template includes a minimal domain model with `User` and `Example` types.

2. **Define the Domain**: The developer replaces the `Example` type with their own domain model using the Klass DSL in `.klass` files. This model describes the application's enumerations, classes, associations, projections, and services.

3. **Compile and Generate**: The Klass compiler processes the `.klass` files. It validates the model and generates the complete application, from the database schema to the REST API.

4. **Extend and Customize**: The developer can then add custom logic or selectively override generated components.

## 3. The Klass Domain-Specific Language (DSL)

The heart of the framework is the Klass language. It allows developers to define not only their data structures but also the shape of their APIs (`projections`) and the behavior of their endpoints (`services`).

### Example: A Stack Overflow Model

To illustrate the language, here is a simplified domain model for an application like Stack Overflow.

```klass
/*
 * Simplified StackOverflow domain model. One question has many answers.
 */
package com.stackoverflow

// 'user' is a special class that represents logged-in users.
user User
    read
    systemTemporal
{
    userId                 : String key userId;
    firstName              : String?;
    lastName               : String?;
    email                  : String?;
}

enumeration Status
{
    OPEN("Open"),
    ON_HOLD("On hold"),
    CLOSED("Closed"),
}

interface Document
{
    id                     : Long key id;
    body                   : String;
}

class Question
    implements Document
    read(QuestionReadProjection)
    systemTemporal
    versioned
    audited
{
    id                     : Long key id;
    title                  : String;
    status                 : Status;
    deleted                : Boolean;
}

class Answer
    implements Document
    systemTemporal
    versioned
    audited
{
    id                     : Long key id;
    questionId             : Long private final;
    deleted                : Boolean;
}

association QuestionHasAnswer
{
    question               : Question[1..1] final;
    answers                : Answer[0..*]
        orderBy: this.id ascending;
}

// A Projection defines the shape of the data returned by an API.
projection AnswerProjection on Answer
{
    id: "Answer id",
    body: "Document body",
    deleted: "Answer deleted",
}

projection QuestionReadProjection on Question
{
    id             : "Question id",
    title          : "Question title",
    body           : "Question body",
    status         : "Question status",
    deleted        : "Question is deleted",
    answers        : AnswerProjection,
}

// A Service defines a RESTful API endpoint.
service QuestionResource on Question
{
    /question/{id: Long[1..1]}
        GET
        {
            multiplicity: one;
            criteria    : this.id == id;
            projection  : QuestionReadProjection;
        }

    /question
        POST
        {
            multiplicity: one;
        }

    /question/{id: Long[1..1]}
        PUT
        {
            multiplicity: one;
            criteria    : this.id == id;
        }
        DELETE
        {
            multiplicity: one;
            criteria    : this.id == id;
            authorize   : this.createdById == user;
        }
}

```

### Why a DSL?

While Java annotations can handle basic entity mapping, a dedicated DSL provides several key advantages:

1. **Expressiveness**: Concepts like `associations`, `projections`, and `services` are first-class citizens, making the model more clear and concise.

2. **Single Source of Truth**: The entire domain, including API shapes and endpoint logic, is defined in one place.

3. **Temporal by Design**: The language enforces a non-destructive, temporal data model. This contrasts with other DSL-based frameworks like JHipster, which are built on ORMs that perform destructive database writes by default.

### Key Concepts in the DSL

- **Projections**: Inspired by GraphQL queries, projections define the tree-like shape of the JSON data returned by a read endpoint. This solves the common problem of over-fetching or under-fetching data and provides a powerful alternative to maintaining separate DTO classes.

- **Services**: Services declaratively define REST endpoints.
    - **Read (GET)** services are linked to a projection to control the response shape.

    - **Write (POST, PUT, PATCH)** services use the domain model to infer allowed fields, types, and transactional boundaries. Ownership in associations dictates which related objects are written together.

## 4. The Klass Compiler

### Advanced Error Reporting

The compiler provides helpful feedback when it encounters errors.

- **Contextual Snippets**: The compiler shows the line with an error, its surrounding structural context (like the class declaration), and uses ASCII art to pinpoint the offending tokens.

- **Syntax Highlighting**: Errors are printed to the console with ANSI color codes to provide syntax highlighting.

Here is an example of a compiler error, which identifies that an association name should be plural:

```text
════════════════════════════════════════ ERR_ASS_PLU ════════════════════════════════════════
Error: Expected to-many association end 'Question.answer' to have a plural name, but name exactly matched type association end type 'Answer'.

At (stackoverflow.klass:158)

  6║ package com.stackoverflow
155║ association QuestionHasAnswer
156║ {
158║     answer                : Answer[0..*]
   ║     ^^^^^^
159║         orderBy: this.id ascending;
160║ }

Location:  stackoverflow.klass:158
File:      file:/.../stackoverflow.klass
Line:      158
Character: 5
═════════════════════════════════════════════════════════════════════════════════════════════

```

### The Macro System

Klass achieves its conciseness through a macro system. Keywords like `systemTemporal`, `versioned`, and `audited` are not annotations; they are macros that expand into more detailed Klass code during compilation.

- **How it Works**: The compiler runs in phases. Certain phases are responsible for finding macro keywords and generating the corresponding Klass code (e.g., `versioned` generates a full `QuestionVersion` class). This generated code is then fed back into the compiler for further processing.

- **Guaranteed Termination**: The order of compiler phases is carefully managed as a Directed Acyclic Graph (DAG), ensuring the compilation process is predictable and always terminates.

- **Macro Generation Trace**: If an error occurs in code generated by a macro, the compiler provides a full generation trace. The report traces the error from the generated code back through any intermediate macros to the original source code written by the developer. For example:

    ```text
    ════════════════════════════════════════ ERR_PRP_MOD ════════════════════════════════════════
    Error: Multiple properties on 'Question' with modifiers [version].

    At (Version association macro)

    1║ package com.stackoverflow
    3║ association QuestionHasVersion
    4║ {
    6║     version: QuestionVersion[1..1] owned version;
     ║                                          ^^^^^^^
    9║ }

    Which was generated by macro at location (stackoverflow.klass:31)
     1║ package com.stackoverflow
    ...
    28║ class Question
    ...
    31║     versioned
      ║     ^^^^^^^^^
    ...
    38║ }

    Location:  stackoverflow.klass:31
    File:      stackoverflow.klass
    Line:      31
    Character: 5
    ═════════════════════════════════════════════════════════════════════════════════════════════

    ```

## 5. Architecture and Extensibility

Klass is highly opinionated but avoids being a "black box" by providing escape hatches for developers.

### Ejectable Philosophy and Selective Overrides

Inspired by `create-react-app`, Klass is designed to be "ejectable." It relies on extensive code generation rather than a heavy runtime library, allowing a developer to take ownership of the generated code at any level of granularity.

While a developer could eject the entire application—taking ownership of all generated code and eventually removing the Klass compiler dependency—the far more common and practical approach is to **selectively override** individual components. This pattern of **selective ownership** is used when custom logic is needed that can't be expressed in the DSL. For example, to add a custom validation to a REST service that calls an external API, a developer can:

1. Copy the last generated version of the service from `target/` as a starting point and place it in the corresponding `src/main/java` directory.

2. Remove the `service` definition from the `.klass` file. This prevents the compiler from re-generating the component and causing a build conflict.

3. The developer can now modify the manually-owned file in `src/main/java` to add any custom logic needed.

Crucially, this manually-owned service can still leverage all the _other_ generated components (ORM, DTOs, serializers, etc.). This provides a balance, offering the productivity of the framework for most of the application while providing control and flexibility where it's needed most.

## 6. Under the Hood: Performance and Reladomo

Klass achieves high performance and solves classic data-fetching problems like the "N+1 query" by building on top of the open-source **Reladomo** ORM.

When the Klass compiler processes a tree-like `projection`, it acts as a translation layer. It analyzes the projection and generates the precise set of linear "deep fetch" instructions that Reladomo needs to fetch the entire data graph efficiently, often in a minimal number of SQL queries. This allows developers to work with the high-level, declarative projection model while the framework handles the low-level query optimization details.
