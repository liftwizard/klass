## Audited Services

Stack Overflow allows collaborative editing. Let's walk through an example where Alice creates a new question and Bob edits the text. Since all audit features are on, both versions of the question are retained and can be fetched through services.

### Create

Alice POSTs a new question to `/api/question` on December 31.

```json
{
    "title": "example title",
    "body": "example body"
}
```

She gets back 201 Created and a location header Location: http://.../api/question/1.

She can GET http://.../api/question/1 to get the full body.

```json
{
    "id": 1,
    "title": "example title",
    "body": "example body",
    "systemFrom": "2017-12-31T23:59:59.000Z",
    "systemTo": null,
    "createdById": "Alice",
    "createdOn": "2017-12-31T23:59:59.000Z",
    "lastUpdatedById": "Alice",
    "answers": [],
    "version": {
        "number": 1,
        "systemFrom": "2017-12-31T23:59:59.000Z",
        "systemTo": null,
        "createdById": "Alice",
        "createdOn": "2017-12-31T23:59:59.000Z",
        "lastUpdatedById": "Alice"
    }
}
```

- `id` was set to 1. The next created question will be 2. The next Answer will be 1. Each type gets its own sequence.
- `systemFrom` was set to December 31. `systemTo` is null, indicating that there is no phase-out time; that this data is currently active.
- `createdOn` matches `systemFrom` for now.
- `createdById` matches `lastUpdatedById` for now.
- The audit properties appear duplicated on the version.

### Update

Bob PUTs a new version to `/api/question/1` the next day, on January 1.

```json
{
    "title": "edited title",
    "body": "edited body",
    "version": {
        "number": 1
    }
}
```

The version number is present and matches, so the edit succeeds. He gets back 204 No Content.

He can GET http://.../api/question/1 to get the full body.

```json
{
    "id": 1,
    "title": "edited title",
    "body": "edited body",
    "systemFrom": "2018-01-01T23:59:59.000Z",
    "systemTo": null,
    "createdById": "Alice",
    "createdOn": "2017-12-31T23:59:59.000Z",
    "lastUpdatedById": "Bob",
    "answers": [],
    "version": {
        "number": 2,
        "systemFrom": "2018-01-01T23:59:59.000Z",
        "systemTo": null,
        "createdById": "Alice",
        "createdOn": "2017-12-31T23:59:59.000Z",
        "lastUpdatedById": "Bob"
    }
}
```

- `systemFrom` was set to January 1. `systemTo` is null again.
- Version 1 (which we'll look at next) had its `systemTo` updated to the same time, January 1.
- The version number was updated to 2.
- `lastUpdatedById` was updated to Bob.
- `createdById` and `createdOn` are unchanged.

### Read by version

To read old versions, we can enhance the read service with an _optional_ version parameter and an optional criteria.

```klass
service QuestionResource
{
    getById(id: Long[1..1]): QuestionReadProjection[0..1]
    {
        operation       : read;
        url             : /question/{id: Long[1..1]}?{version: Integer[0..1] version};
        criteria        : this.id == id;
        optionalCriteria: this.version.number == version
        format          : json;
    }
}
```

Leaving off the version query parameter would give the latest version. But now we can GET http://.../api/question/1?version=1 to see the previous version.

```json
{
    "id": 1,
    "title": "example title",
    "body": "example body",
    "systemFrom": "2017-12-31T23:59:59.000Z",
    "systemTo": "2018-01-01T23:59:59.000Z",
    "createdById": "Alice",
    "createdOn": "2017-12-31T23:59:59.000Z",
    "lastUpdatedById": "Alice",
    "answers": [],
    "version": {
        "number": 1,
        "systemFrom": "2017-12-31T23:59:59.000Z",
        "systemTo": "2018-01-01T23:59:59.000Z",
        "createdById": "Alice",
        "createdOn": "2017-12-31T23:59:59.000Z",
        "lastUpdatedById": "Alice"
    }
}
```

Everything is identical as when Alice first created it, except for `systemTo`. It started as `null` and was changed to January 1, the time version 2 was created.

`systemTemporal` turns the data store into an immutable append-only log. The **only** in-place updates are to `systemTo` and the only possible edit is to phase out a version (by changing `systemTo` from `null` to the current instant) while simultaneously phasing in a new version at the same instant. Or, in the case of a delete, not phasing in any new version.

### Read by time

While it's usually more convenient to deal with version numbers, we can also query for old versions by time.

```klass
service Question
{
    getById(id: Long[1..1]): QuestionReadProjection[0..1]
    {
        operation       : read;
        url             : /question/{id: Long[1..1]}?{version: Integer[0..1] version}&{system: TemporalRange[0..1] system};
        criteria        : this.id == id;
        optionalCriteria: this.version.number == version
        optionalCriteria: this.system == system
        format          : json;
    }
}
```

Now we can GET http://.../api/question/1?system=2018-01-01T00:00:00.000Z to see the previous version.

Time ranges are inclusive on the `from` end and exclusive on the `to` end. So any time within the range `[2017-12-31T23:59:59.000Z, 2018-01-01T23:59:59.000Z)` would return the first version.

The time range check is fairly complex at the data layer. If we're querying for the current version (the system parameter is not provided or is null) then we have to check that systemTo is also null. Otherwise we have to check that the system parameter falls between systemFrom and systemTo, also taking into account the fact that systemTo may be null. We could have defined the additional criteria like this:

```ReladomoOperation
this.systemTo == null && system == null
  || system != null
    && this.systemFrom <= system
    && (this.systemTo == null || system < this.systemTo)
```

The synthetic property `system` exists to simplify criteria like this, and to use in association criteria as described in [Audit Model](audit-data/audit-model#systemtemporal). Thus we're able to simplify the optionalCriteria to just `this.system == system`.
