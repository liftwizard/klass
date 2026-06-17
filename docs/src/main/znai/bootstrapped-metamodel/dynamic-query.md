## Dynamic Query

In a production application, we only enable modeled services. In development, it can be useful to allow dynamic queries.

The dynamic query endpoint is a service that accepts a dynamic projection and criteria as a request body. The format is very similar to the serialized form of a projection or service from the bootstrapped metamodel.

Say we wanted to query for all questions that start with the words "Why do". We could POST `/api/meta/query/json`:

```json
{
	"className": "Question",
	"multiplicity": "many",
	"criteria": "this.title startsWith \"Why do\"",
	"projection": {
		"title": "Question title"
	}
}
```

There's also a form that uses GET, to allow easier experimentation from the browser. The same query can be encoded as query parameters.

`/api/meta/query/json/Question?multiplicity=many&criteria=this.title startsWith "Why do"&include=this.title`

If there's an existing projection that you want to reuse, you can just refer to it by name.

```json
{
	"multiplicity": "many",
	"criteria": "this.title startsWith \"Why do\"",
	"projection": "QuestionReadProjection"
}
```
