# orchestrate-ogcapi

## Scope

This module adds support for OGC Web APIs implementing OGC API Features as a source in [DotWebStack Orchestrate](https://github.com/dotwebstack/orchestrate).

## Requirements

The current implementation depends on certain characteristics that the OGC Web API must support to be a valid backend to orchestrate:

* The OGC Web API must support the OGC API Features Core conformance class.
* The OGC Web API must support GeoJSON as a feature encoding.
* The source model must be defined with a single attribute per object type that has `identifier: true`.
* The name of the object type in the source model must be the `collectionId` path parameter of the collection in the OGC Web API.
* This attribute must be the `featureId` path parameter of the feature in the OGC Web API.
* For an `ObjectTypeRef`property in the source model, the value of property in the GeoJSON encoding must be an object with a member with the name of the identifier attribute as the key and the identifier as the value.
* To support the filter in the collection request of the orchestration engine, the API must support a [filtering query parameter](http://docs.opengeospatial.org/is/17-069r4/17-069r4.html#_parameters_for_filtering_on_feature_properties) that has the same name as the property path of the filter with "/" replaced by ".".

## Status

This is a work in progress.

## Limitations

* Paging is not yet supported by orchestrate, so for now a fixed limit of `10` is used when fetching features from the OGC Web API.
* The handling of references should be made more flexible as different APIs may encode links/references in different ways. At least the following approaches should be supported:
  * the value of the property is a link object with a `href` key and a value that is the URI of the referenced feature in an OGC Web API (last step of the path is the local identifier);
  * the value of the property is the URI of the referenced feature in an OGC Web API (last step of the path is the local identifier);
  * the value of the property is an object with a key that is the local identifier of the referenced feature;
  * the value of the property is the local identifier of the reference feature.
* Since the Gateway module in orchestrate is currently not configurable and the configuration is hard-coded, a modified copy exists for the moment in this module for testing.




