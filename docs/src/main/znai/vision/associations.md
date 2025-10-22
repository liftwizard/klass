## Associations

Let's add the one-to-many association between questions and answers.

```klass
association QuestionHasAnswer
{
    question: Question[1..1] final;
    answers: Answer[0..*];

    relationship this.id == Answer.questionId
}
```

Associations have two ends, and a direction.

The source end is named **`question`** and has type `Question`. The lower-bound of the multiplicity is `1` and the upper bound is also `1`.

The target end is named **`answers`** and has type `Answer`. The lower-bound of the multiplicity is `0` and the upper bound is `*` which means "many".

`Question[1..1]` means that every answer _requires_ a question. An optional, or nullable property has the multiplicity `[0..1]`. The syntax `[1]` is shorthand for `[1..1]`.

`Answer[0..*]` means that every question has many answers, and that the set of answers is allowed to be empty. A non-empty set has the multiplicity `[1..*]`. The syntax `[*]` is shorthand for `[0..*]`.

Colloquially, we call this a one-to-many relationship. Keep in mind that this shorthand is vague. It only refers to the _upper-bounds_ of the association ends.

Klass doesn't enforce singular and plural names. Take care to use singular names for to-one ends and plural names for to-many ends.

The `question` end has the modifier `final` which means that an answer cannot be repointed to a different question. In the data layer, this means the foreign key `questionId` is immutable. It doesn't mean that the question's text is immutable.
