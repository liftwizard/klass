# Klass Project Build Commands and Coding Standards

## Build Commands

- Full build: `mise run maven`
- Skip tests: `mise run maven --flags '-DskipTests --threads 2C --quiet'`
- Run single test: `mise run maven --flags '--threads 2C --quiet -Dtest=TestClassName#testMethodName`

## Code Style

- Java imports: Alphabetical with static imports first, Eclipse Collections preferred over Java Collections
- Code format: 4 spaces indent, braces required, limited line length
- Annotations: `@Nonnull`/`@Nullable` from javax.annotation, `@AutoService` for service providers
- Logging: SLF4J with proper MDC context via `runWithMdc()` methods
- Naming: camelCase methods/variables, PascalCase classes, immutable objects with builders
- Error handling: Validate parameters, fail fast, use appropriate exceptions
- Documentation: Javadoc on all public methods/classes
- Design patterns: Favor composition over inheritance, use dependency injection

