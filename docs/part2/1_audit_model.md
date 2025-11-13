## Audit Model

All four features are activated using classifier modifiers.

```klass
class Question
    systemTemporal
    versioned
    audited
{
    // ...
}
```

Most classifier modifiers are compiler macros that are shorthand for a longer syntax that you could write out if desired.

### systemTemporal

The modifier `systemTemporal` is shorthand for adding three properties.

```klass
class Question
{
    // ...
    system    : TemporalRange system;
    systemFrom: TemporalInstant system from;
    systemTo  : TemporalInstant system to;
}
```

`systemFrom` and `systemTo` become part of the data layer. `system` is a synthetic property used in queries and criteria.

Relationships between two `systemTemporal` classes also implicitly include an equality clause between their `system` properties.

```klass
association QuestionHasAnswer
{
    question: Question[1..1] final;
    answers: Answer[0..*];

    relationship (this.id == Answer.questionId)
        && this.system == Answer.system // The additional clause
}
```

### versioned

The modifier `versioned` is shorthand for adding a new version class and an association to it.

```klass
class QuestionVersion
    systemTemporal
{
    id    : Long key;
    // version modifier indicates the property holding the version number
    number: Integer version;
}

association QuestionHasVersion
{
    question: Question[1..1];
    version : QuestionVersion[1..1] owned version;

    relationship this.id == QuestionVersion.id
}
```

### audited

Auditing means keeping track of who made which modifications. To use auditing, there must be a `user` class. There may only be a single `user` class in the model.

```klass
// keyword `user` instead of `class`
user User
    systemTemporal
{
    // user class must have a single String key userId
    userId   : String key userId;
    // All other properties must be optional
    firstName: String?;
    lastName : String?;
    email    : String?;
}
```

The modifier `audited` is shorthand for adding three properties and two parameterized properties. If the class is `versioned`, the version class gets the same properties. See the section on [composite writes](TODO) to learn why.

```klass
class Question
    systemTemporal
{
    // ...
    createdById       : String private createdBy;
    createdOn         : Instant createdOn;
    lastUpdatedById   : String private lastUpdatedBy;

    // A parameterized-property. Gives the User instead of the userId
    createdBy(): User[1..1] createdBy
    {
        this.createdById == User.userId
    }

    lastUpdatedBy(): User[1..1] lastUpdatedBy
    {
        this.lastUpdatedById == User.userId
    }
}

class QuestionVersion
    systemTemporal
{
    id             : Long key;
    number         : Integer version;
    createdById    : String private createdBy;
    createdOn      : Instant createdOn;
    lastUpdatedById: String private lastUpdatedBy;

    createdBy(): User[1..1] createdBy
    {
        this.createdById == User.userId
    }

    lastUpdatedBy(): User[1..1] lastUpdatedBy
    {
        this.lastUpdatedById == User.userId
    }
}

```
