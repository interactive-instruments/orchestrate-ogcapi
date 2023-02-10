# orchestrate-ogcapi

## Scope

This module adds support for OGC Web APIs implementing OGC API Features as a source in [DotWebStack Orchestrate](https://github.com/dotwebstack/orchestrate).

## Requirements

* The OGC Web API must support GeoJSON as a feature encoding.
* The name of the object type in the source model must be the `collectionId` path parameter of the collection in the OGC Web API.
* The source model must be defined with a single attribute that has `identifier: true`.
* This attribute must be the `featureId` path parameter of the feature in the OGC Web API.
* For an `ObjectTypeRef`property in the source model, the value of property in the GeoJSON encoding must be an object with a member with the name of the identifier attribute as the key and the identifier as the value.

## Status

This is a work in progress.

## Limitations

* Since the Gateway module in orchestrate is currently not configurable and the configuration is hard-coded, a modified copy exists for the moment in this module for testing.
* The handling of references should be made more flexible as different APIs may encode links/references in different ways. At least the following approaches should be supported:
  * the value of the property is a link object with a `href` key and a value that is the URI of the referenced feature in an OGC Web API (last step of the path is the local identifier);
  * the value of the property is the URI of the referenced feature in an OGC Web API (last step of the path is the local identifier);
  * the value of the property is an object with a key that is the local identifier of the referenced feature;
  * the value of the property is the local identifier of the reference feature.
* The latest changes in orchestrate break the code. Use `git checkout 34312606e097f3a17426e7cf8a1014f87dc0f4f4` followed by `mvn clean install` to get a version that works with the current code.




