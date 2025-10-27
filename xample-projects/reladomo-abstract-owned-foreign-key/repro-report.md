When I declare a to-many, owned relationship to a sub-type, where the foreign key lives in its super-type, Reladomo generates code which won't compile.

`ConcreteChild` extends `AbstractChild`. One `Parent` owns many ConcreteChildren (relatedIsDependent=true). The foreign key `parentKey` is defined in its super class, AbstractChild.

The generated class `ParentAbstract` has two compiler errors, in `setParentKey()` and `setChildren()`. Both are because `ConcreteChildList` has no method `setParentKey()`.

```java
public void setParentKey(String newValue)
{
    MithraDataObject d = zSetString(ParentFinder.parentKey(), newValue, true, false );
    if (d == null) return;
    ParentData data = (ParentData) d;
    TransactionalBehavior _behavior = zGetTransactionalBehaviorForWriteWithWaitIfNecessary();
    if (!_behavior.isPersisted())
    {
        ConcreteChildList children =
        (ConcreteChildList ) data.getChildren();
        if (children != null)
        {
            // Compiler error here
            children.setParentKey(newValue);
        }
    }
}
```

```java
public void setChildren(ConcreteChildList children)
{
    ConcreteChildList _children = (ConcreteChildList) children;
    TransactionalBehavior _behavior = zGetTransactionalBehaviorForWriteWithWaitIfNecessary();
    ParentData _data = (ParentData) _behavior.getCurrentDataForWrite(this);
    if (_behavior.isInMemory())
    {
        if (_behavior.isDetached() && _children != null)
        {
            _children.zMakeDetached(ConcreteChildFinder.parentKey().eq(_data.getParentKey()),
                _data.getChildren());
        }

        _data.setChildren(_children);
        if (_children != null)
        {
            // Compiler error here
            _children.setParentKey(_data.getParentKey());
            _children.zSetParentContainerparent(this);
            _children.zSetAddHandler(new ChildrenAddHandlerInMemory());
        }
        else if (_behavior.isDetached())
        {
            throw new MithraBusinessException("to-many relationships cannot be set to null. Use the clear() method on the list instead.");
        }
    }
    else if (_behavior.isPersisted())
    {
        _behavior.clearTempTransaction(this);
        _children.zSetAddHandler(new ChildrenAddHandlerPersisted());
        ConcreteChildList childrenToDelete = new ConcreteChildList();
        childrenToDelete.addAll(this.getChildren());
        for(int i=0;i < _children.size(); i++)
        {
            ConcreteChild item = _children.getConcreteChildAt(i);
            if (!childrenToDelete.remove(item))
            {
                item.setParentKey(_data.getParentKey());
                item.cascadeInsert();
            }
        }

        childrenToDelete.cascadeDeleteAll();
    }
    else throw new RuntimeException("not implemented");
}
```

If I change relatedIsDependent to false, I don't get any compiler errors. I'm using this as a workaround. I thought about moving the foreign key, but I'm using table-per-class and I really do want the foreign key in the shared table if possible.

Full xmls below.

Parent.xml:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<MithraObject
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/goldmansachs/reladomo/master/reladomogen/src/main/xsd/mithraobject.xsd"
        initializePrimitivesToNull="true"
        objectType="transactional">
    <PackageName>com.repro.reladomo.abstractownedforeignkey</PackageName>
    <ClassName>Parent</ClassName>
    <DefaultTable>PARENT</DefaultTable>

    <Attribute
            name="parentKey"
            javaType="String"
            primaryKey="true"
            nullable="false"
            readonly="true"
            finalGetter="true"
            columnName="parent_key"
            trim="false" />
    <Relationship
            name="children"
            reverseRelationshipName="parent"
            relatedObject="ConcreteChild"
            relatedIsDependent="true"
            cardinality="one-to-many">
            this.parentKey = ConcreteChild.parentKey
    </Relationship>

</MithraObject>
```

AbstractChild.xml:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<MithraObject
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/goldmansachs/reladomo/master/reladomogen/src/main/xsd/mithraobject.xsd"
        initializePrimitivesToNull="true"
        objectType="transactional"
        superClassType="table-per-class">
    <PackageName>com.repro.reladomo.abstractownedforeignkey</PackageName>
    <ClassName>AbstractChild</ClassName>
    <DefaultTable>ABSTRACT_CHILD</DefaultTable>

    <Attribute
            name="parentKey"
            javaType="String"
            primaryKey="false"
            nullable="false"
            finalGetter="true"
            columnName="parent_key"
            trim="false" />
    <Attribute
            name="childKey"
            javaType="String"
            primaryKey="true"
            nullable="false"
            readonly="true"
            finalGetter="true"
            columnName="child_key"
            trim="false" />
</MithraObject>
```

ConcreteChild.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<MithraObject
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/goldmansachs/reladomo/master/reladomogen/src/main/xsd/mithraobject.xsd"
        initializePrimitivesToNull="true"
        objectType="transactional">
    <PackageName>com.repro.reladomo.abstractownedforeignkey</PackageName>
    <ClassName>ConcreteChild</ClassName>
    <SuperClass name="AbstractChild" generated="true" />
    <DefaultTable>CONCRETE_CHILD</DefaultTable>

    <Attribute
            name="childProperty"
            javaType="String"
            primaryKey="false"
            nullable="false"
            finalGetter="true"
            columnName="child_property"
            trim="false" />
</MithraObject>
```
