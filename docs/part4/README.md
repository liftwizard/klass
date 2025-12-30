# Additional Features

In this section, we'll continue the Stack Overflow example and implement up/down votes on Questions.

We'll explore enumerations, parameterized properties, parameterized projections, and aggregations.

## Enumerations

We'll start with an enumeration to indicate the direction of a vote.

```klass
enumeration VoteDirection
{
    UP("up"),
    DOWN("down"),
}
```

The upper-case literal names are used within the model. The quoted strings are pretty names that will appear in service request and response bodies.

## Partial inference, composite keys

Next we'll define the vote class.

```klass
class QuestionVote
    systemTemporal
    audited
{
    questionId : Long key;
    createdById: String key createdBy;
    direction  : VoteDirection;
}
```

Stack Overflow [restricts voting](https://meta.stackexchange.com/a/5213/147927) based on how many times you've voted and whether the post was edited since the last vote. So votes must be `systemTemporal`.

Votes must be `audited`, to keep track of who cast the vote.

A user can only cast one vote, so the key should be (questionId, createdById). There is no auto-generated `id` property. `QuestionVote` has a natural, composite key.

Normally, we'd allow the `createdById` property to be inferred based on the `audited` keyword, but then it would be inferred without the `key` modifier.

A user cannot upvote and downvote the same question, so `direction` is not part of the key.

## Associations

```klass
association QuestionHasVotes
{
    question: Question[1..1] final;
    votes: QuestionVote[0..*];

    relationship this.id == QuestionVote.questionId
}

association UserHasQuestionVotes
{
    user: User[1..1] final;
    votes: QuestionVote[0..*];

    relationship this.userId == QuestionVote.createdById
}
```

A vote is like many-to-many mapping from Question to User.

## Services

We'll create an upsert service for creating and editing votes. No create service is necessary because there's no auto-incrementing id.

```klass
service QuestionVoteResource
{
    upsert upsertQuestionVote(
        questionId: Long[1..1] path,
        user: User[1..1] user,
        questionVote: QuestionVote[1] body)
    {
        // service : http;

        url     : /question/vote/{questionId: Long[1..1]};

        criteria: this.question.id == questionId && this.createdById == user.userId;

        after   : this.question.createdById != user.userId
            && (this.createdOn > now - (5 * 60 * 1000)
                || this.systemFrom < this.question.systemFrom);
    }

    upsert upsertQuestionVote(
        questionVote: QuestionVote[1] body,
        user: User[1..1] user)
    {
        // service : rpc;

        url     : /question/vote;

        criteria: this.question.id == questionVote.question.id && this.createdById == user.userId;

        after   : this.question.createdById != user.userId
            && (this.createdOn > now - (5 * 60 * 1000)
                || this.systemFrom < this.question.systemFrom);
    }

    /question/vote/{questionId: Long[1..1]}
        PUT
        {
            multiplicity: one;
            criteria    : this.questionId == questionId && this.createdById == userId;
            validate    : this.question.createdById != userId
                && (this == null
                || this.createdOn > now - (5 * 60 * 1000)
                || this.systemFrom < this.question.systemFrom);
            projection  : QuestionVoteWriteProjection;
        }
}
```

The main criteria uses the global `userId` which is the name of the currently logged in principal.

The validation criteria uses the global `now`, less 5 minutes in milliseconds, to check whether the vote was created within the last 5 minutes.

```klass
service QuestionVote
{
    /question/vote/{questionId: Long[1..1]}
        // PUT ...
        DELETE
        {
            multiplicity: one;
            criteria    : this.questionId == questionId && this.createdById == userId;
            validate    : this.createdOn > now - (5 * 60 * 1000)
                    || this.systemFrom < this.question.systemFrom);
            projection  : QuestionVoteWriteProjection;
        }
}
```

The DELETE service is very similar. It just doesn't need to validate that the vote exists, and it doesn't need to validate that the voter and the question author are different users.

## Parameterized properties

When viewing a question on Stack Overflow, you can see your own vote if you've cast one.

```klass
class Question
    systemTemporal
    versioned
    audited
{
    // ...

    myVote(): QuestionVote[0..1]
    {
        this.votes.createdById == userId
    }
}
```

We add `myVote()` as a parameterized property, though there are no parameters in this example. This criteria is another example using the global `userId`. Once the parameterized property is defined, it can be included in a projection using the same syntax as an association end. In fact, a parameterized property without any parameters can be thought of as a uni-directional association.

## Parameterized projections

The definition of `myVote()` above is fine, and fairly similar to the `upvoted` and `downvoted` properties in [the real StackOverflow api](https://api.stackexchange.com/docs/types/question).

However, let's add some perhaps unrealistic variants that take parameters, just to see what parameterized properties can do.

```klass
class Question
    systemTemporal
    versioned
    audited
{
    // ...

    votesByDirection(direction: VoteDirection[1..1]): QuestionVote[0..*]
    {
        this.votes.direction == direction
    }

    voteByUser(userId: String[1..1] userId): QuestionVote[0..1]
    {
        // userId refers to the parameter, not the global
        this.votes.createdById == userId
    }
}
```

Parameterized properties can be included in read projections. There are two ways to provide the parameter, as a constant or as a projection parameter.

Here we define a projection that parameterizes `votesByDirection` twice, once for each `VoteDirection`.

```klass
projection ProjectionWithConstant on Question
{
    id             : "Question id",
    title          : "Question title",
    body           : "Question body",

    votesByDirection("up"):
    {
        // This would work, but the information is redundant
        // direction: "Upvote direction",
        user: {
            userId: "Upvote user id",
        },
    },

    votesByDirection("down"):
    {
        // This would work, but the information is redundant
        // direction: "Upvote direction",
        user: {
            userId: "Upvote user id",
        },
    },
}
```

Here we define a parameterized projection, that passes its parameter through to `voteByUser`.

```klass
projection ProjectionWithParameter(userId: String[1..1] userId) on Question
{
    id             : "Question id",
    title          : "Question title",
    body           : "Question body",

    voteByUser(userId):
    {
        direction: "Vote direction",
        // This would work, but the information is redundant
        /*
        user: {
            userId: "Vote user id",
        },
        */
    }
}
```

### Parameterized service

When using a parameterized projection, the service must also be parameterized.

```klass
service Question
{
    /question/{id: Long[1..1]}/{userId: String[1..1] userId}
        GET
        {
            format      : json;
            multiplicity: one;
            criteria    : this.id == id;
            projection  : ProjectionWithParameter(userId);
        }
}
```

### Parameterized responses

When a json response includes parameterized properties, the parameters become part of the field names.

```json
{
	"id": 1,
	"title": "Question title 1",
	"body": "Question body 1",
	"votesByDirection(up)": [
		{
			"user": {
				"userId": "Example user id"
			}
		}
	],
	"votesByDirection(down)": [],
	"voteByUser(Example user id)": {
		"direction": "up"
	}
}
```

## Aggregations

[StackOverflow's documentation](https://stackoverflow.com/help/privileges/established-user) states:

> Voting scores, as displayed, are the sum of the up and down votes on a post. Vote counts are the individual up and down votes that make up the score.
>
> You can view the vote counts by clicking on the score of a post. This will break the score into upvotes and downvotes.

We add two aggregation properties to Question; `upvotes` and `downvotes`.

```klass
class Question
    systemTemporal
    versioned
    audited
{
    // ...

    upvotes: Integer
    {
        count(this.votes.direction == VoteDirection.UP)
    }

    downvotes: Integer
    {
        count(this.votes.direction == VoteDirection.DOWN)
    }
}
```

Associations and parameterized properties are defined using criteria. Aggregation properties are defined using a fuller expression language. These aggregations use the `count()` function. Other available functions include `sum()`, `min()`, `max()`, and `average()`.

Once these aggregation properties are defined in a class, they can be included in read projections like normal properties. They cannot be included in write projections.
