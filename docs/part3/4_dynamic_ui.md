## Dynamic UI

Production UI components are code-generated, but the dynamic UI used in development is static JavaScript.

The UI fetches the domain model from the meta-services. It renders dynamic forms for querying and editing based on classes and their data-type properties.

The dynamic projection builder uses classes and all their properties, including association ends. The root node represents a class. Leaf nodes are data-type properties or association ends that are not included in the projection. Inner nodes are association ends that _are_ included in the projection.
