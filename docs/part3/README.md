# Bootstrapped Metamodel

Klass has a metamodel; a model of models. In fact, it has several representations of the same metamodel.

- The Abstract Syntax Tree used internally by the compiler.
- The in-memory DomainModel object returned upon successful compilation.
- The bootstrapped metamodel.
- etc.

The bootstrapped metamodel is expressed like a regular model. The first few meta-types are defined like this:

```klass
package klass.model.meta.domain

interface Element
{
}

interface NamedElement
    implements Element
{
    name: String key maximumLength(256);
    ordinal: Integer private;
}

class PackageableElement
    abstract
    implements NamedElement
{
    packageName: String maximumLength(100000);
}

class Classifier
    abstract
    extends PackageableElement
{
}

class Klass
    extends Classifier
{
    superClassName: String? private maximumLength(256);
    abstractClass: Boolean;
}

enumeration PrimitiveType
{
    INTEGER("Integer"),
    LONG("Long"),
    DOUBLE("Double"),
    FLOAT("Float"),
    BOOLEAN("Boolean"),
    STRING("String"),
    INSTANT("Instant"),
    LOCAL_DATE("LocalDate"),
    TEMPORAL_INSTANT("TemporalInstant"),
    TEMPORAL_RANGE("TemporalRange"),
}

enumeration Multiplicity
{
    ZERO_TO_ONE("0..1"),
    ONE_TO_ONE("1..1"),
    ZERO_TO_MANY("0..*"),
    ONE_TO_MANY("1..*"),
}

class DataTypeProperty
    abstract
    implements NamedElement
{
    classifierName                : String key maximumLength(256);
    optional                      : Boolean;
}

class PrimitiveProperty
    extends DataTypeProperty
{
    primitiveType                 : PrimitiveType maximumLength(256);
}

association ClassifierHasDataTypeTypeProperties
{
    owningClassifier              : Classifier[1..1];
    dataTypeProperties            : DataTypeProperty[0..*] owned
        orderBy: this.ordinal ascending;
}
```

The full source is too large to include here. It defines enumeration, class, association, projection, and service, plus their parts.

The bootstrapped metamodel allows us to work with metadata like regular data. It defines some projections and services which return model data. For example, here's the service to get a class by its name:

```klass
package klass.model.meta.domain

projection KlassProjection on Klass
{
    name: "NamedElement name",
    packageName: "PackageableElement packageName",
    abstractClass: "Klass abstractClass",
    superInterfaces: ClassifierInterfaceMappingProjection,
    classifierModifiers: ClassifierModifierProjection,
    dataTypeProperties: DataTypePropertyProjection,
    superClass: {
        name: "NamedElement name",
    },
    associationEnds: {
        name: "NamedElement name",
    },
    parameterizedProperties: ParameterizedPropertyProjection,
}

service KlassResource on Klass
{
    /meta/class/{name: String[1..1]}
        GET
        {
            multiplicity: one;
            criteria    : this.name == name;
            projection  : KlassProjection;
        }
    /meta/class
        GET
        {
            multiplicity: many;
            criteria    : all;
            projection  : KlassProjection;
            orderBy     : this.ordinal ascending;
        }
}
```
