I've defined an abstract class that also implements an interface. When I use superClassType="table-per-subclass", the generated code is missing the `implements` clause. When I use superClassType="table-per-class", I'm not seeing this problem.

I've put together a small repro.

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!--MyInterface.xml-->
<MithraInterface
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/goldmansachs/reladomo/master/reladomogen/src/main/xsd/mithraobject.xsd">
    <PackageName>com.repro.reladomo.abstractimplementsinterface</PackageName>
    <ClassName>MyInterface</ClassName>
    <Attribute
            name="stringAttribute"
            javaType="String" />
</MithraInterface>
```

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!--MyAbstractClass.xml-->
<MithraObject
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/goldmansachs/reladomo/master/reladomogen/src/main/xsd/mithraobject.xsd"
        initializePrimitivesToNull="true"
        objectType="transactional"
        superClassType="table-per-subclass">
    <PackageName>com.repro.reladomo.abstractimplementsinterface</PackageName>
    <ClassName>MyAbstractClass</ClassName>
    <MithraInterface>MyInterface</MithraInterface>
    <Attribute
            name="id"
            javaType="long"
            primaryKey="true"
            primaryKeyGeneratorStrategy="SimulatedSequence"
            nullable="false"
            columnName="id">
        <SimulatedSequence
            sequenceName="MyAbstractClass"
            sequenceObjectFactoryName="io.liftwizard.reladomo.simseq.ObjectSequenceObjectFactory"
            hasSourceAttribute="false"
            batchSize="10"
            initialValue="1"
            incrementSize="1" />
    </Attribute>
    <Attribute
            name="stringAttribute"
            javaType="String"
            primaryKey="false"
            nullable="false"
            columnName="string_attribute"
            trim="false" />
</MithraObject>
```

The generated class MyAbstractClassAbstract is missing `implements MyInterface`.

```java
/**
 * This file was automatically generated using Mithra 17.0.2. Please do not modify it.
 * Add custom logic to its subclass instead.
 */
// Generated from templates/transactional/superclass/Abstract.jsp
public abstract class MyAbstractClassAbstract
	extends com.gs.fw.common.mithra.superclassimpl.MithraTransactionalObjectImpl {
	// ...
}
```
