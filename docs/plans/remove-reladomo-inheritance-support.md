# Plan: Remove Reladomo Native Inheritance Support

## Executive Summary

This document outlines the plan to remove Klass's dependency on Reladomo's native inheritance support. Instead of generating `superClassType` attributes and `{ClassName}SuperClass`/`{ClassName}SubClass` relationships in Reladomo XML files, we will handle inheritance concepts at the Klass abstraction layer.

## Current State

### How Inheritance Works Today

1. **Reladomo XML Generation** (`ReladomoObjectFileGenerator.java:269-286`)
   - Generates relationships like `managerSubClass` and `employeeSuperClass`
   - Reladomo's marshaller detects these and adds `superClassType="table-per-subclass"`
   - Each class in the hierarchy gets its own table sharing primary keys

2. **ReladomoTree Abstraction** (already exists in `klass-model-reladomo-tree`)
   - `SuperClassReladomoTreeNode` and `SubClassReladomoTreeNode` represent inheritance navigation
   - Deep fetcher uses `relatedFinder.getRelationshipFinderByName("{className}SuperClass")`
   - This currently **depends on** Reladomo's generated inheritance relationships

3. **Deep Fetching** (`ReladomoTreeNodeDeepFetcherListener.java:91-126`)
   ```java
   // Currently relies on Reladomo-generated relationships
   String relationshipName = UPPER_TO_LOWER_CAMEL.convert(superClass.getName()) + "SuperClass";
   RelatedFinder<?> nextFinder = relatedFinder.getRelationshipFinderByName(relationshipName);
   ```

4. **Known Bugs** (`meta.klass:96`)
   - "Why isn't subClasses showing up in generated code? Reladomo bug?"
   - Property overrides create duplicate columns instead of reusing parent columns

### Files Currently Generating Reladomo Inheritance

| File | What It Does |
|------|--------------|
| `ReladomoObjectFileGenerator.java` | Generates `SubClass` relationships in XML |
| `AbstractReladomoGenerator.java:56` | Formats `superClassType` attribute |

## Proposed Architecture

### Core Principle

**Stop telling Reladomo about inheritance. Instead:**
- Generate each class as an independent Reladomo object (no `superClassType`)
- Handle inheritance semantics in the Klass abstraction layer
- The database schema stays the same (table-per-subclass with shared PKs)
- Klass code manages joins/queries across the inheritance hierarchy

### Inheritance Strategy: Joined Tables (Table-Per-Subclass)

Each class in the hierarchy has its own table:
```
Person (id, name)           -- base table
Employee (id, employee_id)  -- shares PK with Person
Manager (id, department)    -- shares PK with Employee
```

This is the same schema as today, but Reladomo won't know about the inheritance relationship.

## Affected Layers

### 1. Schema Generation (Low Impact)

**Current**: `TableGenerator.java` excludes superclass properties from subclass tables.

**Change**: No change needed. Continue generating separate tables with shared primary keys.

### 2. Reladomo XML Generation (Medium Impact)

**Current**: Generates `SubClass` relationships and triggers `superClassType`.

**Change**:
- Remove `convertSubClassRelationship()` method
- Remove subclass relationship generation in `convertRelationships()`
- Each class becomes a standalone Reladomo object

**Files**:
- `klass-generator-reladomo/ReladomoObjectFileGenerator.java`

### 3. DataStore (High Impact)

**Current**: `ReladomoDataStore` uses single `RelatedFinder` per class.

**Change**: Need new abstraction to:
- Query across inheritance hierarchy (find all Employees including Managers)
- Insert/update across multiple tables atomically
- Handle polymorphic references

**New Component**: `InheritanceAwareDataStore` or extend existing DataStore

**Files**:
- `klass-data-store-reladomo/ReladomoDataStore.java`

### 4. Deep Fetching (High Impact)

**Current**: Uses `getRelationshipFinderByName("{className}SuperClass")`.

**Change**:
- Can't rely on Reladomo relationship navigation for inheritance
- Must manually fetch from related tables and join in memory
- Or use separate queries per table in hierarchy

**Files**:
- `klass-reladomo-tree-deep-fetcher/ReladomoTreeNodeDeepFetcherListener.java`
- `klass-reladomo-graphql-deep-fetcher/GraphQLDeepFetcher.java`

### 5. Serialization (High Impact)

**Current**: Serializers traverse `SuperClass`/`SubClass` nodes.

**Change**:
- When serializing an object, must explicitly fetch data from all tables in hierarchy
- Must include all superclass properties in the output
- `__typeName` handling remains similar

**Files**:
- `klass-reladomo-tree-serializer/ReladomoTreeObjectToMapSerializerListener.java`
- `klass-reladomo-tree-serializer-dto/ReladomoTreeObjectToDTOSerializerListener.java`
- `klass-serialization-jackson-jsonview-reladomo/ReladomoJsonViewSerializer.java`

### 6. Persistent Synchronizer / Deserialization (High Impact)

**Current**: `PersistentSynchronizer` handles JSON to persistent objects.

**Change**:
- When creating/updating a subclass, must insert into all tables in hierarchy
- Must coordinate inserts: Person first, then Employee, then Manager
- Must handle cascading deletes across hierarchy

**Files**:
- `klass-reladomo-persistent-writer/PersistentSynchronizer.java`

### 7. ReladomoTree Abstraction (Medium Impact)

**Current**: `SuperClassReladomoTreeNode` and `SubClassReladomoTreeNode` exist.

**Change**:
- Keep these nodes - they represent inheritance navigation in our model
- Change how listeners interpret them (not via Reladomo relationships)
- May need to add "inheritance context" to carry state

**Files**:
- `klass-model-reladomo-tree/*.java` (node classes stay, listeners change)

### 8. GraphQL/Projection Converters (Low Impact)

**Current**: Build tree with SuperClass/SubClass nodes for field selection.

**Change**: Minimal - these work at the Klass model level, not Reladomo level.

**Files**:
- `klass-model-reladomo-graphql/ReladomoTreeGraphqlConverter.java`
- `klass-model-reladomo-projection/ReladomoProjectionConverter.java`

## Implementation Phases

### Phase 1: Investigate Reladomo Behavior Without Inheritance

1. Create a test project with inheritance (e.g., `Person -> Employee -> Manager`)
2. Generate Reladomo XML **without** the SubClass relationships
3. Verify:
   - Tables are still created correctly
   - Each class works independently in Reladomo
   - Understand what breaks when inheritance is removed

### Phase 2: Create Inheritance-Aware Query Layer

1. Design `InheritanceAwareDataStore` or similar abstraction
2. Implement polymorphic queries:
   - "Find all Persons" → query Person, Employee, Manager tables, union results
   - "Find Employee by ID" → query Employee + Person tables, join on PK
3. Implement efficient fetching (minimize N+1 queries)

### Phase 3: Update Deep Fetching

1. Modify `ReladomoTreeNodeDeepFetcherListener`:
   - `enterSuperClass`: Trigger fetch from superclass table
   - `exitSuperClass`: Join results with current object
2. Handle the case where object doesn't exist in superclass table (data integrity)

### Phase 4: Update Serializers

1. Modify serializer listeners to:
   - Aggregate properties from entire inheritance chain
   - Handle cases where inheritance data might be fetched lazily
2. Ensure `__typeName` correctly reflects actual runtime type

### Phase 5: Update Persistent Synchronizer

1. Implement insert across hierarchy:
   ```
   insert Person(id=1, name="Alice")
   insert Employee(id=1, employeeId="E001")
   insert Manager(id=1, department="Engineering")
   ```
2. Implement update across hierarchy (only touch tables with changed properties)
3. Implement delete cascade (delete in reverse order: Manager, Employee, Person)

### Phase 6: Remove Reladomo Inheritance Generation

1. Remove `convertSubClassRelationship()` from `ReladomoObjectFileGenerator`
2. Remove subclass iteration in `convertRelationships()`
3. Update tests to reflect new XML structure

### Phase 7: Enable Bootstrap and Sample Data

1. Re-enable sample data generation for metamodel
2. Verify bootstrap works correctly with new inheritance handling
3. Address any remaining issues

## Key Design Decisions Needed

### 1. Polymorphic Queries

**Option A**: Union queries across all concrete subclasses
```sql
SELECT * FROM Person
UNION ALL
SELECT * FROM Employee JOIN Person ON ...
UNION ALL
SELECT * FROM Manager JOIN Employee ON ... JOIN Person ON ...
```

**Option B**: Query each table separately, merge in memory

**Option C**: Denormalize into a single table (changes schema strategy)

### 2. Lazy vs Eager Inheritance Loading

**Option A**: Always load full inheritance chain (simpler, potentially wasteful)

**Option B**: Lazy load superclass data on demand (complex, potential N+1)

**Option C**: Projection-driven (only fetch what's requested - current approach)

### 3. Type Discrimination

How to determine the actual type of a polymorphic object:

**Option A**: Query all potential subclass tables to find which ones have the ID

**Option B**: Add discriminator column to base table (schema change)

**Option C**: Maintain type registry/cache (complexity)

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Performance degradation from multiple queries | Batch queries, use joins where possible |
| Complexity in transaction management | Leverage Reladomo's transaction support |
| Breaking existing applications | Extensive test coverage before migration |
| Edge cases in deep inheritance hierarchies | Limit to 2-3 levels in practice |

## Questions to Resolve Before Implementation

1. **Schema change acceptable?** Adding discriminator column would simplify type detection
2. **Performance requirements?** How many objects with inheritance are queried at once?
3. **Existing data migration?** Any production data using the current inheritance model?
4. **Reladomo version constraints?** Any Reladomo features we can't use without inheritance?

## Appendix: Affected Modules Summary

**Must Change:**
- `klass-generator-reladomo` - Stop generating inheritance relationships
- `klass-data-store-reladomo` - Add inheritance-aware querying
- `klass-reladomo-tree-deep-fetcher` - Handle inheritance without Reladomo navigation
- `klass-reladomo-tree-serializer` - Aggregate properties across hierarchy
- `klass-reladomo-tree-serializer-dto` - Same as above
- `klass-reladomo-persistent-writer` - Multi-table insert/update/delete

**Minimal Changes:**
- `klass-model-reladomo-tree` - Keep nodes, update semantics
- `klass-model-reladomo-projection` - Keep nodes
- `klass-model-reladomo-graphql` - Keep converter logic
- `klass-generator-liquibase-schema` - No change (schema stays same)

**No Changes:**
- `klass-compiler` - Inheritance model unchanged
- `klass-model-meta-interface` - API unchanged
- DTO generators, GraphQL schema generators - Use Klass model, not Reladomo
