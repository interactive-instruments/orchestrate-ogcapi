# OGC Web APIs as data sources to DotWebStack Orchestrate

## Scope

[DotWebStack Orchestrate](https://github.com/dotwebstack/orchestrate) is an open-source engine for model-driven orchestration.

This module adds support for OGC Web APIs implementing OGC API Features as a data source in DotWebStack Orchestrate.

## Requirements

The current implementation depends on certain characteristics that the OGC Web API must support to be a valid backend to orchestrate:

* The API must support the [OGC API Features "Core" conformance class](https://docs.ogc.org/is/17-069r4/17-069r4.html#rc_core).
* The API must support the [OGC API Features "GeoJSON" conformance class](https://docs.ogc.org/is/17-069r4/17-069r4.html#rc_geojson), i.e., support GeoJSON as a feature encoding.
* The API must support the [OGC API Features "Coordinate Reference Systems by Reference" conformance class](https://docs.ogc.org/is/18-058r1/18-058r1.html#rc_crs), i.e., coordinate reference systems in addition to WGS84 longitude/latitude. The API must support the coordinate reference system `http://www.opengis.net/def/crs/EPSG/0/28992`. This is a temporary requirement that will be removed again. 
* The name of the object type in the source model must be the `collectionId` path parameter of the collection in the OGC Web API.
* The source model must be defined with a single attribute per object type that has `identifier: true`.
* This attribute must be the `featureId` path parameter of the feature in the OGC Web API.
* For an `ObjectTypeRef` property in the source model, the value of property in the GeoJSON encoding must be the identifier if the referenced feature.
  * This implementation supports properties that are declared in the schema with `x-ogc-role: reference`. In that case, the option `supportsRelProfiles` has to be set to `true` in the source configuration.
* All feature properties that may be used to filter a collection must be declared as a queryable. 
* The name of each queryable must be the same as the path of the property in the DotWebStack Orchestrate source model.
* The API must support the [OGC API Features "Features Filter" conformance class](https://docs.ogc.org/DRAFTS/19-079r1.html#rc_features-filter) and the [OGC Common Query Language "CQL2 Text" conformance class](https://docs.ogc.org/DRAFTS/21-065.html#rc_cql2-text) 

## Hints

* If the API supports the [OGC Common Query Language "Advanced Comparison Operators" conformance class](https://docs.ogc.org/DRAFTS/21-065.html#rc_advanced-comparison-operators), the `IN` operator will be used for batch loading. Otherwise a logical `OR` expression will be used.
* If the API supports the [OGC API Features "Queryables as Query Parameters" conformance class](https://docs.ogc.org/DRAFTS/19-079r1.html#rc_queryables_param), the query parameters for the queryables will be used to filter on property values, otherwise the `filter` parameter will be used.

## Configuration

To declare an OGC Web API as a source, add the source to `orchestrate/gateway/sources` in the application configuration (`application.yml`). The `type` must be `ogcapi`.

Options:

| Option | Default | xxx |
| --- | --- | --- |
| url | - | **REQUIRED** The URL of the landing page of the OGC Web API. |
| limit | 10 | **TEMPORARY** The page size for collection and batch requests. This is a stopgap until DotWebStack Orchestrate supports paging. |
| supportsPropertySelection | false | **TEMPORARY**  Set to `true`, if the API supports the `properties` query parameter on feature queries. The value is a list of properties to return. This is a stopgap until a conformance class URI for this capability is available in the conformance declaration of the API. |
| supportsRelProfiles | false | **TEMPORARY**  Set to `true`, if the API supports the `profile` query parameter on feature queries with a value `rel-as-key`. This is a stopgap until a conformance class URI for this capability is available in the conformance declaration of the API. |

Example:

```yaml
orchestrate:
  gateway:
    sources:
      bag:
        type: ogcapi
        options:
          url: https://wau.ldproxy.net/bag
          limit: 100
          supportsPropertySelection: true
          supportsRelProfiles: true
```

## Status

This is a work in progress.
