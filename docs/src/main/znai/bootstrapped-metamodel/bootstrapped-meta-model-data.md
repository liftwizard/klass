## Bootstrapped Metamodel Data

Since the metamodel is part of the model, the bootstrap process populates metamodel types too. This means that we can GET `/api/meta/class/Klass` for the Klass class in any application.

```json
{
	"name": "Klass",
	"packageName": "klass.model.meta.domain",
	"superInterfaces": [],
	"classifierModifiers": [],
	"dataTypeProperties": [
		{
			"__typename": "klass.model.meta.domain.PrimitiveProperty",
			"name": "superClassName",
			"optional": true,
			"minLengthValidation": null,
			"maxLengthValidation": {
				"number": 256
			},
			"propertyModifiers": [
				{
					"keyword": "private"
				}
			],
			"primitiveType": "String",
			"minValidation": null,
			"maxValidation": null
		},
		{
			"__typename": "klass.model.meta.domain.PrimitiveProperty",
			"name": "abstractClass",
			"optional": false,
			"minLengthValidation": null,
			"maxLengthValidation": null,
			"propertyModifiers": [],
			"primitiveType": "Boolean",
			"minValidation": null,
			"maxValidation": null
		}
	],
	"abstractClass": false,
	"superClass": {
		"name": "Classifier"
	},
	"associationEnds": [
		{
			"name": "subClasses"
		},
		{
			"name": "superClass"
		},
		{
			"name": "associationEnds"
		},
		{
			"name": "associationEndsResultTypeOf"
		},
		{
			"name": "memberReferencePaths"
		},
		{
			"name": "parameterizedProperties"
		},
		{
			"name": "parameterizedPropertiesResultTypeOf"
		},
		{
			"name": "serviceGroups"
		}
	],
	"parameterizedProperties": []
}
```
