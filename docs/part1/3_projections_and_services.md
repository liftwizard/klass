## Projections and Services

Let's add the service to fetch a question by its id.

### Service

```klass
service QuestionResource
{
    read getById(questionId: Long[1..1] id path): QuestionReadProjection[1]
    {
        url      : /question/{questionId: Long[1..1] id};
        criteria : this.id == questionId;
        format   : json;
    }
}
```

- Read operations translate to the `GET` verb when using http.
- We'll be able to `GET` `/api/question/1` to fetch the Question with id 1.
- The response will be json.
- The return multiplicity is one, so the response body will be a json object, not a json array.
- The criterion matches `Question.id` against the path parameter `questionId`, like the sql `where q.id = ?`
- The projection defines how much data will get serialized in the response body.

### Projection

The service referred to the projection `QuestionReadProjection` without defining it. Let's do that now.

```klass
projection QuestionReadProjection on Question
{
    id     : "Question id",
    title  : "Question title",
    body   : "Question body",

    answers:
    {
        id  : "Answer id",
        body: "Answer body",
    },
}
```

This projection includes everything; all of the question's properties including its answers, and all of the answers' properties. We do want everything included in the service's json response, but if we didn't we could remove parts to shrink the response.

### JSON Format

Since the service definition includes `format: json`, the response body will match the nested structure of the projection, and the projection's header names will be ignored.

```json
{
    "id": 1,
    "title": "Question title 1",
    "body": "Question body 1",
    "answers": [
        {
            "id": 1,
            "body": "Answer body 1"
        },
        {
            "id": 2,
            "body": "Answer body 2"
        }
    ]
}
```

### CSV Format

If the service definition included `format: csv` instead, then the nested structure would be flattened and the header names would become column headers in the response. The order of the columns would be defined by the order they appear in the projection definition from top-to-bottom, which is depth-first.

```csv
"Question id", "Question title"  , "Question body"  , "Answer id", "Answer body"
            1, "Question title 1", "Question body 1",           1, "Answer body 1"
            1, "Question title 1", "Question body 1",           2, "Answer body 2"
```

Note that to-many association ends get flattened, so the data for question 1 appears twice.
