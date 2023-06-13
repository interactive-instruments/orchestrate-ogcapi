package de.ii.orchestrate.ogcapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.com.google.common.collect.ImmutableList;
import graphql.com.google.common.collect.ImmutableMap;
import graphql.com.google.common.collect.ImmutableSet;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dotwebstack.orchestrate.ext.spatial.GeometryType;
import org.dotwebstack.orchestrate.model.Attribute;
import org.dotwebstack.orchestrate.model.Model;
import org.dotwebstack.orchestrate.model.ObjectType;
import org.dotwebstack.orchestrate.model.Relation;
import org.dotwebstack.orchestrate.source.BatchRequest;
import org.dotwebstack.orchestrate.source.CollectionRequest;
import org.dotwebstack.orchestrate.source.DataRepository;
import org.dotwebstack.orchestrate.source.ObjectRequest;
import org.dotwebstack.orchestrate.source.SelectedProperty;
import org.dotwebstack.orchestrate.source.SourceException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

class OgcApiFeaturesDataRepository implements DataRepository {

  final static HttpClient CLIENT = HttpClient.create().headers(
      h -> h.set(HttpHeaderNames.ACCEPT, "application/geo+json,application/problem+json;q=0.8,application/json;q=0.7"));

  final static String ONE_TEMPLATE = "{apiLandingPage}/collections/{collectionId}/items/{featureId}";

  // limit to 10 features for now until there is paging support
  final static String COLLECTION_TEMPLATE = "{apiLandingPage}/collections/{collectionId}/items?limit={limit}";
  final static String SEARCH_TEMPLATE = "{apiLandingPage}/search";

  final static ObjectMapper MAPPER = new ObjectMapper();
  public static final String PATH_SEPARATOR = ".";
  public static final int MAX_URI_LENGTH = 8_000;
  public static final String AD_HOC_QUERY_TEMPLATE = "{\"collections\": [\"{%s}\"], " +
      "\"filter\": { \"op\": \"in\", \"args\": [ { \"property\": \"%s\" }, [ \"%s\" ] ] }%s}";
  public static final String PROPERTIES = "properties";
  public static final String GEOMETRY = "geometry";
  public static final String ID = "id";
  public static final String FEATURES = "features";

  private final String apiLandingPage;
  private final int limit;
  private final boolean supportsPropertySelection;
  private final boolean supportsBatchLoading;
  private final boolean supportsCql2InOperator;
  private final boolean supportsAdHocQuery;
  private final boolean supportsRelProfiles;
  private final boolean supportsIntersects;

  private final Model model;

  public OgcApiFeaturesDataRepository(OgcApiFeaturesConfiguration configuration) {
    this.apiLandingPage = configuration.getApiLandingPage();
    this.limit = configuration.getLimit();
    this.supportsPropertySelection = configuration.isSupportsPropertySelection();
    this.supportsBatchLoading = configuration.isSupportsBatchLoading();
    this.supportsCql2InOperator = configuration.isSupportsCql2InOperator();
    this.supportsAdHocQuery = configuration.isSupportsAdHocQuery();
    this.supportsRelProfiles = configuration.isSupportsRelProfiles();
    this.supportsIntersects = configuration.isSupportsIntersects();
    this.model = configuration.getModel();
  }

  @Override
  public boolean supportsBatchLoading(ObjectType objectType) {
    return supportsBatchLoading;
  }

  @Override
  public Mono<Map<String, Object>> findOne(ObjectRequest objectRequest) {
    var collectionId = getCollectionId(objectRequest.getObjectType());
    if (collectionId == null) {
      throw new RuntimeException(
          String.format("Invalid object request: no object type has been provided. Request: %s", objectRequest));
    }
    var objectType = model.getObjectType(collectionId);
    if (objectType == null) {
      throw new RuntimeException(
          String.format("Invalid object request: object type is not present in the model. Request: %s", objectRequest));
    }
    var idProperty = getIdentityProperty(objectType);
    if (idProperty == null) {
      throw new RuntimeException(
          String.format("Invalid object request: object type has no id property. Request: %s", objectRequest));
    }
    var featureId = (String) objectRequest.getObjectKey().get(idProperty);
    if (featureId == null) {
      throw new RuntimeException(
          String.format("Invalid object request: no object id has been provided. Request: %s", objectRequest));
    }

    var properties = supportsPropertySelection ? "?properties=" +
        getPropertiesParameterString(objectType, objectRequest.getSelectedProperties(), ImmutableList.of()) : "";
    var profile = supportsRelProfiles ? (properties.isEmpty() ? "?" : "&") + "profile=rel-as-key" : "";
    var uri = ONE_TEMPLATE.replace("{apiLandingPage}", apiLandingPage).replace("{collectionId}", collectionId)
        .replace("{featureId}", featureId) + properties + profile;

    return CLIENT.get().uri(uri)
        .responseSingle((response, content) -> {
          if (response.status() != HttpResponseStatus.OK && response.status() != HttpResponseStatus.NOT_FOUND) {
            throw new RuntimeException(
                String.format("Object request returned a status different than 200: %d. URI: %s",
                    response.status().code(), uri));
          }
          return response.status() == HttpResponseStatus.OK ? content.asByteArray() : Mono.empty();
        })
        .map(geojsonFeatureAsByteArray -> {
          try {
            //noinspection unchecked
            return (Map<String, Object>) getFeature(objectRequest.getSelectedProperties(),
                MAPPER.readValue(geojsonFeatureAsByteArray, Map.class));
          } catch (IOException e) {
            throw new RuntimeException("Received invalid feature response.", e);
          }
        });
  }

  @Override
  public Flux<Map<String, Object>> find(CollectionRequest collectionRequest) {
    var collectionId = getCollectionId(collectionRequest.getObjectType());
    if (collectionId == null) {
      throw new RuntimeException(
          String.format("Invalid collection request: no object type has been provided. Request: %s",
              collectionRequest));
    }
    var objectType = model.getObjectType(collectionId);
    if (objectType == null) {
      throw new RuntimeException(
          String.format("Invalid collection request: object type is not present in the model. Request: %s",
              collectionRequest));
    }
    var filterExpression = collectionRequest.getFilter();
    var filter = "";
    if (filterExpression != null) {
      var basePath = String.join(PATH_SEPARATOR, filterExpression.getPath().getSegments());
      if (filterExpression.getValue() instanceof Map) {
        filter = ((Map<?, ?>) filterExpression.getValue()).values().stream()
            .map(v -> String.format("&%s=%s", basePath, v))
            .collect(Collectors.joining());
      } else if (filterExpression.getValue() instanceof Geometry) {
        var srid = ((Geometry) filterExpression.getValue()).getSRID();
        var wkt = new WKTWriter().write((Geometry) filterExpression.getValue());
        filter = String.format("&filter=s_intersects(%s,%s)%s", basePath, wkt,
            srid != 4326 ? "&filter-crs=http://www.opengis.net/def/crs/EPSG/0/" + srid : "");
      }
    }
    var properties = supportsPropertySelection ? String.format("&properties=%s",
        getPropertiesParameterString(objectType, collectionRequest.getSelectedProperties(), ImmutableList.of())) : "";
    var profile = supportsRelProfiles ? "&profile=rel-as-key" : "";
    var uri = COLLECTION_TEMPLATE.replace("{apiLandingPage}", apiLandingPage).replace("{collectionId}", collectionId)
        .replace("{limit}", String.valueOf(limit)) + filter + properties + profile;

    return CLIENT.get().uri(uri)
        .responseSingle((response, content) -> {
          if (response.status() != HttpResponseStatus.OK && response.status() != HttpResponseStatus.NOT_FOUND) {
            throw new RuntimeException(
                String.format("Collection request returned a status different than 200: %d. URI: %s",
                    response.status().code(), uri));
          }
          return response.status() == HttpResponseStatus.OK ? content.asByteArray() : Mono.empty();
        })
        .map(geojsonFeatureCollectionAsByteArray -> {
          Map<String, Object> geojsonFeatureCollection;
          try {
            //noinspection unchecked
            geojsonFeatureCollection = MAPPER.readValue(geojsonFeatureCollectionAsByteArray, Map.class);
          } catch (IOException e) {
            throw new RuntimeException("Received invalid feature collection response.", e);
          }

          //noinspection unchecked
          return ((List<Map<String, Object>>) geojsonFeatureCollection.get(FEATURES)).stream()
              .map(geojsonFeature -> getFeature(collectionRequest.getSelectedProperties(), geojsonFeature)).toList();
        }).flatMapMany(Flux::fromIterable);
  }

  @Override
  public Flux<Map<String, Object>> findBatch(BatchRequest batchRequest) {
    var collectionId = getCollectionId(batchRequest.getObjectType());
    if (collectionId == null) {
      throw new RuntimeException(
          String.format("Invalid batch request: no object type has been provided. Request: %s", batchRequest));
    }
    var objectType = model.getObjectType(collectionId);
    if (objectType == null) {
      throw new RuntimeException(
          String.format("Invalid batch request: object type is not present in the model. Request: %s", batchRequest));
    }
    var idProperty = getIdentityProperty(objectType);
    if (idProperty == null) {
      throw new RuntimeException(
          String.format("Invalid batch request: object type has no id property. Request: %s", batchRequest));
    }

    var propertyList = supportsPropertySelection ?
        getPropertiesParameter(objectType, batchRequest.getSelectedProperties(), ImmutableList.of()) :
        ImmutableList.<String>of();
    if (!propertyList.isEmpty() && !propertyList.contains(idProperty)) {
      propertyList = Stream.concat(propertyList.stream(), Stream.of(idProperty)).toList();
    }
    var properties = propertyList.isEmpty() ? "" : String.format("&properties=%s", String.join(",", propertyList));
    var objectKeys =
        batchRequest.getObjectKeys().stream().map(id -> (String) id.get(idProperty)).filter(Objects::nonNull).toList();
    var filter = supportsCql2InOperator ?
        String.format("&filter=%s%%20in%%20('%s')", idProperty, String.join("','", objectKeys)) :
        String.format("&filter=%s", String.join("%%20OR%%20",
            objectKeys.stream().map(key -> String.format("%s='%s'", idProperty, key)).toList()));
    var profile = supportsRelProfiles ? "&profile=rel-as-key" : "";
    var uri = COLLECTION_TEMPLATE.replace("{apiLandingPage}", apiLandingPage).replace("{collectionId}", collectionId)
        .replace("{limit}", String.valueOf(objectKeys.size())) + filter + properties + profile;

    Mono<byte[]> responseContent;
    if (uri.length() <= MAX_URI_LENGTH) {
      responseContent = CLIENT.get().uri(uri)
          .responseSingle((response, content) -> {
            if (response.status() != HttpResponseStatus.OK && response.status() != HttpResponseStatus.NOT_FOUND) {
              throw new RuntimeException(
                  String.format("Collection request returned a status different than 200: %d. URI: %s",
                      response.status().code(), uri));
            }
            return response.status() == HttpResponseStatus.OK ? content.asByteArray() : Mono.empty();
          });
    } else if (supportsAdHocQuery) {
      properties = propertyList.isEmpty() ? "" :
          String.format(", \"properties\": [ \"%s\" ]", String.join("\", \"", propertyList));
      var requestContent =
          String.format(AD_HOC_QUERY_TEMPLATE, collectionId, idProperty, String.join("\", \"", objectKeys), properties);
      responseContent = CLIENT.post().uri(SEARCH_TEMPLATE.replace("{apiLandingPage}", apiLandingPage))
          .send(ByteBufFlux.fromString(Flux.just(requestContent)))
          .responseSingle((response, content) -> {
            if (response.status() != HttpResponseStatus.OK && response.status() != HttpResponseStatus.NOT_FOUND) {
              throw new RuntimeException(
                  String.format("Collection request returned a status different than 200: %d. Request: %s",
                      response.status().code(), batchRequest));
            }
            return response.status() == HttpResponseStatus.OK ? content.asByteArray() : Mono.empty();
          });
    } else {
      throw new RuntimeException(
          "Batch loading failed, too many identifiers, the resulting URI is too long and Ad-hoc Queries using POST are not supported.");
    }

    return responseContent.map(geojsonFeatureCollectionAsByteArray -> {
      Map<String, Object> geojsonFeatureCollection;
      try {
        //noinspection unchecked
        geojsonFeatureCollection = MAPPER.readValue(geojsonFeatureCollectionAsByteArray, Map.class);
      } catch (IOException e) {
        throw new RuntimeException("Received invalid feature collection response.", e);
      }

      //noinspection unchecked
      return ((List<Map<String, Object>>) geojsonFeatureCollection.get(FEATURES)).stream()
          .map(geojsonFeature -> getFeature(batchRequest.getSelectedProperties(), geojsonFeature)).toList();
    }).flatMapMany(Flux::fromIterable);
  }

  private String getCollectionId(ObjectType objectType) {
    return objectType.getName();
  }

  private String getIdentityProperty(ObjectType objectType) {
    if (objectType.getIdentityProperties().size() != 1) {
      throw new SourceException(
          String.format("Source models using an OGC Web API must have exactly one identity property. Found: %s.",
              objectType.getIdentityProperties().stream().map(Object::toString).collect(Collectors.joining(", "))));
    }

    return objectType.getIdentityProperties().get(0).getName();
  }

  private List<String> getPropertiesParameter(ObjectType objectType, List<SelectedProperty> selectedProperties,
                                              List<String> parentPath) {
    return selectedProperties.stream().filter(selectedProperty -> {
      var property = objectType.getProperty(selectedProperty.getProperty().getName());
      return !(property instanceof Attribute) || !(((Attribute) property).getType() instanceof GeometryType);
    }).map(selectedProperty -> {
      var propertyName = selectedProperty.getProperty().getName();
      var subProperties = selectedProperty.getProperty() instanceof Relation
          ? ImmutableSet.<SelectedProperty>of()
          : selectedProperty.getSelectedProperties();
      var path = ImmutableList.<String>builder().addAll(parentPath).add(propertyName).build();
      if (subProperties.isEmpty()) {
        return ImmutableList.of(String.join(PATH_SEPARATOR, path));
      }
      return getPropertiesParameter(objectType, subProperties.stream().toList(), path);
    }).flatMap(List::stream).toList();
  }

  private String getPropertiesParameterString(ObjectType objectType, List<SelectedProperty> selectedProperties,
                                              List<String> parentPath) {

    return String.join(",", getPropertiesParameter(objectType, selectedProperties, parentPath));
  }

  private Map<String, Object> getFeature(List<SelectedProperty> selectedProperties,
                                         Map<String, Object> geojsonFeature) {
    //noinspection unchecked
    var featureProperties =
        Objects.nonNull(geojsonFeature.get(PROPERTIES)) ? (Map<String, Object>) geojsonFeature.get(PROPERTIES) :
            ImmutableMap.<String, Object>of();

    //noinspection unchecked
    var featureGeometry =
        Objects.nonNull(geojsonFeature.get(GEOMETRY)) ? (Map<String, Object>) geojsonFeature.get(GEOMETRY) :
            ImmutableMap.<String, Object>of();

    // ignoring geometry for now
    return getObject(selectedProperties, geojsonFeature.get(ID), featureProperties, featureGeometry);
  }

  private Map<String, Object> getObject(List<SelectedProperty> selectedProperties, Object featureId,
                                        Map<String, Object> featureProperties, Map<String, Object> featureGeometry) {
    var builder = ImmutableMap.<String, Object>builder();
    selectedProperties.forEach(
        selectedProperty -> processProperty(builder, selectedProperty, featureId, featureProperties, featureGeometry));
    return builder.build();
  }

  private void processProperty(ImmutableMap.Builder<String, Object> builder, SelectedProperty selectedProperty,
                               Object featureId, Map<String, Object> featureProperties,
                               Map<String, Object> featureGeometry) {
    var key = selectedProperty.getProperty().getName();
    if (selectedProperty.getProperty().isIdentifier() && featureId != null) {
      builder.put(key, featureId);
    } else if (selectedProperty.getProperty() instanceof Attribute attribute) {
      processAttribute(builder, selectedProperty, featureProperties, key, attribute, featureGeometry);
    } else if (selectedProperty.getProperty() instanceof Relation) {
      processAssociationRole(builder, selectedProperty, featureProperties, key);
    }
  }

  private void processAssociationRole(ImmutableMap.Builder<String, Object> builder, SelectedProperty selectedProperty,
                                      Map<String, Object> featureProperties, String key) {
    var value = featureProperties.get(key);
    if (value != null) {
      if (value instanceof List) {
        //noinspection unchecked
        var valueAsList = (List<Map<String, Object>>) value;
        var listBuilder = ImmutableList.<Map<String, Object>>builder();
        valueAsList.forEach(refProperties -> listBuilder.add(
            getObject(selectedProperty.getSelectedProperties().stream().toList(), null, refProperties,
                ImmutableMap.of())));
        builder.put(key, listBuilder.build());
      } else if (value instanceof Map) {
        //noinspection unchecked
        builder.put(key,
            getObject(selectedProperty.getSelectedProperties().stream().toList(), null, (Map<String, Object>) value,
                ImmutableMap.of()));
      } else if (value instanceof String) {
        builder.put(key,
            getObject(selectedProperty.getSelectedProperties().stream().toList(), null, ImmutableMap.of("identificatie",
                value), ImmutableMap.of()));
      } else {
        throw new RuntimeException(String.format("Unsupported value type: %s", value.getClass().getSimpleName()));
      }
    }
  }

  private void processAttribute(ImmutableMap.Builder<String, Object> builder, SelectedProperty sp,
                                Map<String, Object> featureProperties, String key, Attribute attribute,
                                Map<String, Object> featureGeometry) {
    var value = featureProperties.get(key);
    if (value != null) {
      switch (attribute.getType().getName()) {
        case "Boolean", "Double", "Float", "Integer", "Long", "String" -> builder.put(key, value);
        default -> throw new RuntimeException(
            String.format("Unsupported attribute type: %s", ((Attribute) sp.getProperty()).getType().getName()));
      }
    } else if (attribute.getType() instanceof GeometryType && !featureGeometry.isEmpty()) {
      // use the primary geometry as fallback
      builder.put(key, featureGeometry);
    }
  }
}
