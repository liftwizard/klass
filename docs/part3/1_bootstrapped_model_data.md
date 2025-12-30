## Bootstrapped Model Data

The Klass metamodel is part of every Klass model. When a Klass application starts, it "bootstraps" the metamodel by populating model data into the data store. This means that in the Stack Overflow application, we can GET `/api/meta/class/Question` to get the Question class as data.

```json
{
	"name": "Question",
	"inferred": false,
	"packageName": "com.stackoverflow",
	"ordinal": 1,
	"sourceCode": "class Question\n    systemTemporal\n    versioned\n    audited\n{\n    id                : Long key id;\n    title             : String;\n    body              : String;\n    system            : TemporalRange system;\n    systemFrom        : TemporalInstant system from;\n    systemTo          : TemporalInstant system to;\n    createdById       : String private createdBy;\n    createdOn         : Instant createdOn;\n    lastUpdatedById   : String private lastUpdatedBy;\n    answers: Answer[0..*]\n        orderBy: this.id ascending;\n    version: QuestionVersion[1..1] version;\n}\n",
	"classifierModifiers": [
		{
			"name": "systemTemporal",
			"inferred": false,
			"ordinal": 1
		},
		{
			"name": "versioned",
			"inferred": false,
			"ordinal": 2
		},
		{
			"name": "audited",
			"inferred": false,
			"ordinal": 3
		}
	],
	"primitiveProperties": [
		{
			"name": "id",
			"inferred": false,
			"primitiveType": "Long",
			"optional": false,
			"key": true,
			"id": true,
			"ordinal": 1,
			"primitivePropertyModifiers": [
				{
					"name": "key",
					"inferred": false,
					"ordinal": 1
				},
				{
					"name": "id",
					"inferred": false,
					"ordinal": 2
				}
			]
		},
		{
			"name": "title",
			"inferred": false,
			"primitiveType": "String",
			"optional": false,
			"key": false,
			"id": false,
			"ordinal": 2,
			"primitivePropertyModifiers": []
		},
		{
			"name": "body",
			"inferred": false,
			"primitiveType": "String",
			"optional": false,
			"key": false,
			"id": false,
			"ordinal": 3,
			"primitivePropertyModifiers": []
		},
		{
			"name": "system",
			"inferred": false,
			"primitiveType": "TemporalRange",
			"optional": false,
			"key": false,
			"id": false,
			"ordinal": 4,
			"primitivePropertyModifiers": [
				{
					"name": "system",
					"inferred": false,
					"ordinal": 1
				}
			]
		},
		{
			"name": "systemFrom",
			"inferred": false,
			"primitiveType": "TemporalInstant",
			"optional": false,
			"key": false,
			"id": false,
			"ordinal": 5,
			"primitivePropertyModifiers": [
				{
					"name": "system",
					"inferred": false,
					"ordinal": 1
				},
				{
					"name": "from",
					"inferred": false,
					"ordinal": 2
				}
			]
		},
		{
			"name": "systemTo",
			"inferred": false,
			"primitiveType": "TemporalInstant",
			"optional": false,
			"key": false,
			"id": false,
			"ordinal": 6,
			"primitivePropertyModifiers": [
				{
					"name": "system",
					"inferred": false,
					"ordinal": 1
				},
				{
					"name": "to",
					"inferred": false,
					"ordinal": 2
				}
			]
		},
		{
			"name": "createdById",
			"inferred": false,
			"primitiveType": "String",
			"optional": false,
			"key": false,
			"id": false,
			"ordinal": 7,
			"primitivePropertyModifiers": [
				{
					"name": "private",
					"inferred": false,
					"ordinal": 1
				},
				{
					"name": "createdBy",
					"inferred": false,
					"ordinal": 2
				}
			]
		},
		{
			"name": "createdOn",
			"inferred": false,
			"primitiveType": "Instant",
			"optional": false,
			"key": false,
			"id": false,
			"ordinal": 8,
			"primitivePropertyModifiers": [
				{
					"name": "createdOn",
					"inferred": false,
					"ordinal": 1
				}
			]
		},
		{
			"name": "lastUpdatedById",
			"inferred": false,
			"primitiveType": "String",
			"optional": false,
			"key": false,
			"id": false,
			"ordinal": 9,
			"primitivePropertyModifiers": [
				{
					"name": "private",
					"inferred": false,
					"ordinal": 1
				},
				{
					"name": "lastUpdatedBy",
					"inferred": false,
					"ordinal": 2
				}
			]
		}
	],
	"enumerationProperties": [],
	"associationEnds": [
		{
			"name": "answers",
			"inferred": false,
			"direction": "target",
			"multiplicity": "0..*",
			"resultType": {
				"name": "Answer"
			},
			"owningAssociation": {
				"name": "QuestionHasAnswer"
			},
			"associationEndModifiers": []
		},
		{
			"name": "version",
			"inferred": true,
			"direction": "target",
			"multiplicity": "1..1",
			"resultType": {
				"name": "QuestionVersion"
			},
			"owningAssociation": {
				"name": "QuestionHasVersion"
			},
			"associationEndModifiers": [
				{
					"name": "version",
					"inferred": true,
					"ordinal": 1
				}
			]
		}
	]
}
```
