## Classes

Let's add the classes `Question` and `Answer` to the model in stackoverflow.klass.

### Question

```klass
class Question
{
    id        : Long key id;
    title     : String minLength(15) maxLength(150);
    body      : String maxLength(30000);
}
```

**`id`** has the primitive type `Long`. The `key` modifier is similar to a database's primary key. The `id` modifier on the `id` property means that it's an auto-incrementing number, not a natural key.

**`title`** has the primitive type `String` with max length 150.

**`body`** has the primitive type `String` with max length 30000.

So far, this exactly matches the real Stack Overflow.

### Answer

```klass
class Answer
{
    id        : Long key id;
    body      : String(30000);
    questionId: Long private;
}
```

`Answer` also has **`id`** and **`body`** but no `title`.

`Answer` has **`questionId`** with the `private` modifier. It's private to the data-layer; it will be used as a foreign key. Private properties cannot appear in projections, so `questionId` won't be part of service bodies; neither request bodies nor response bodies.
