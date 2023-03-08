package de.ii.orchestrate.ogcapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.com.google.common.collect.ImmutableList;
import graphql.com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpHeaderNames;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

  private final String apiLandingPage;
  private final int limit;
  private final boolean supportsPropertySelection;
  private final boolean supportsBatchLoading;
  private final boolean supportsCql2InOperator;
  private final boolean supportsAdHocQuery;
  private final Model model;

  public OgcApiFeaturesDataRepository(OgcApiFeaturesConfiguration configuration) {
    this.apiLandingPage = configuration.getApiLandingPage();
    this.limit = configuration.getLimit();
    this.supportsPropertySelection = configuration.isSupportsPropertySelection();
    this.supportsBatchLoading = configuration.isSupportsBatchLoading();
    this.supportsCql2InOperator = configuration.isSupportsCql2InOperator();
    this.supportsAdHocQuery = configuration.isSupportsAdHocQuery();
    this.model = configuration.getModel();
  }

  @Override
  public boolean supportsBatchLoading(ObjectType objectType) {
    return supportsBatchLoading;
  }

  @Override
  public Mono<Map<String, Object>> findOne(ObjectRequest objectRequest) {
    var collectionId = getCollectionId(objectRequest.getObjectType());
    var idProperty = getIdentityProperty(model.getObjectType(collectionId));
    var featureId = (String) objectRequest.getObjectKey().get(idProperty);
    var properties = supportsPropertySelection ?
        "?properties=" + getPropertiesParameterString(objectRequest.getSelectedProperties(), ImmutableList.of()) : "";
    return CLIENT.get().uri(
            ONE_TEMPLATE.replace("{apiLandingPage}", apiLandingPage).replace("{collectionId}", collectionId)
                .replace("{featureId}", featureId) + properties).responseContent().aggregate().asString()
        .map(geojsonFeatureAsString -> {
          try {
            //noinspection unchecked
            return (Map<String, Object>) getFeature(objectRequest.getSelectedProperties(),
                MAPPER.readValue(geojsonFeatureAsString, Map.class));
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        });
  }

  @Override
  public Flux<Map<String, Object>> find(CollectionRequest collectionRequest) {
    var collectionId = getCollectionId(collectionRequest.getObjectType());
    var filterExpression = collectionRequest.getFilter();
    var filter = filterExpression != null ?
        String.format("&%s=%s", String.join(PATH_SEPARATOR, filterExpression.getPropertyPath().getSegments()),
            filterExpression.getValue()) : "";
    var properties = supportsPropertySelection ? String.format("&properties=%s",
        getPropertiesParameterString(collectionRequest.getSelectedProperties(), ImmutableList.of())) : "";
    return CLIENT.get().uri(
            COLLECTION_TEMPLATE.replace("{apiLandingPage}", apiLandingPage).replace("{collectionId}", collectionId)
                .replace("{limit}", String.valueOf(limit)) + filter + properties).responseContent().aggregate().asString()
        .map(geojsonFeatureCollectionAsString -> {
          Map<String, Object> geojsonFeatureCollection;
          try {
            //noinspection unchecked
            geojsonFeatureCollection = MAPPER.readValue(geojsonFeatureCollectionAsString, Map.class);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }

          //noinspection unchecked
          return ((List<Map<String, Object>>) geojsonFeatureCollection.get("features")).stream()
              .map(geojsonFeature -> getFeature(collectionRequest.getSelectedProperties(), geojsonFeature)).toList();
        }).flatMapMany(Flux::fromIterable);
  }

  @Override
  public Flux<Map<String, Object>> findBatch(BatchRequest batchRequest) {
    var collectionId = getCollectionId(batchRequest.getObjectType());
    var idProperty = getIdentityProperty(model.getObjectType(collectionId));
    var propertyList =
        supportsPropertySelection ? getPropertiesParameter(batchRequest.getSelectedProperties(), ImmutableList.of()) :
            ImmutableList.<String>of();
    if (!propertyList.isEmpty() && !propertyList.contains(idProperty)) {
      propertyList = Stream.concat(propertyList.stream(), Stream.of(idProperty)).toList();
    }
    var properties = propertyList.isEmpty() ? "" : String.format("&properties=%s", String.join(",", propertyList));
    var objectKeys =
        batchRequest.getObjectKeys().stream().map(id -> (String) id.get(idProperty)).filter(Objects::nonNull).toList();
    var filter = supportsCql2InOperator ?
        String.format("&filter=%s%%20in%%20['%s']", idProperty, String.join("', '", objectKeys)) :
        String.format("&filter=%s", String.join("%%20OR%%20",
            objectKeys.stream().map(key -> String.format("%s='%s'", idProperty, key)).toList()));
    var uri = COLLECTION_TEMPLATE.replace("{apiLandingPage}", apiLandingPage).replace("{collectionId}", collectionId)
        .replace("{limit}", String.valueOf(objectKeys.size())) + filter + properties;
    ByteBufFlux response;
    if (uri.length() <= MAX_URI_LENGTH) {
      response =
          CLIENT.get().uri(uri).responseContent();
    } else if (supportsAdHocQuery) {
      properties = propertyList.isEmpty() ? "" :
          String.format(", \"properties\": [ \"%s\" ]", String.join("\", \"", propertyList));
      var content =
          String.format(AD_HOC_QUERY_TEMPLATE, collectionId, idProperty, String.join("\", \"", objectKeys), properties);
      response = CLIENT.post()
          .uri(SEARCH_TEMPLATE.replace("{apiLandingPage}", apiLandingPage))
          .send(ByteBufFlux.fromString(Flux.just(content))).responseContent();
    } else {
      throw new RuntimeException(
          "Batch loading failed, too many identifiers, the resulting URI is too long and Ad-hoc Queries using POST are not supported.");
    }

    return response.aggregate().asString().map(geojsonFeatureCollectionAsString -> {
      Map<String, Object> geojsonFeatureCollection;
      try {
        //noinspection unchecked
        geojsonFeatureCollection = MAPPER.readValue(geojsonFeatureCollectionAsString, Map.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }

      //noinspection unchecked
      return ((List<Map<String, Object>>) geojsonFeatureCollection.get("features")).stream()
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

  private List<String> getPropertiesParameter(List<SelectedProperty> selectedProperties, List<String> parentPath) {
    return selectedProperties.stream().map(selectedProperty -> {
      var propertyName = selectedProperty.getProperty().getName();
      var subProperties = selectedProperty.getSelectedProperties();
      var path = ImmutableList.<String>builder().addAll(parentPath).add(propertyName).build();
      if (subProperties.isEmpty()) {
        return ImmutableList.of(String.join(PATH_SEPARATOR, path));
      }
      return getPropertiesParameter(subProperties, path);
    }).flatMap(List::stream).toList();
  }

  private String getPropertiesParameterString(List<SelectedProperty> selectedProperties, List<String> parentPath) {
    return String.join(",", getPropertiesParameter(selectedProperties, parentPath));
  }

  private Map<String, Object> getFeature(List<SelectedProperty> selectedProperties,
                                         Map<String, Object> geojsonFeature) {
    //noinspection unchecked
    var featureProperties =
        geojsonFeature.containsKey("properties") ? (Map<String, Object>) geojsonFeature.get("properties") :
            ImmutableMap.<String, Object>of();

    // ignoring geometry for now
    return getObject(selectedProperties, geojsonFeature.get("id"), featureProperties);
  }

  private Map<String, Object> getObject(List<SelectedProperty> selectedProperties, Object featureId,
                                        Map<String, Object> featureProperties) {
    var builder = ImmutableMap.<String, Object>builder();
    selectedProperties.forEach(
        selectedProperty -> processProperty(builder, selectedProperty, featureId, featureProperties));
    return builder.build();
  }

  private void processProperty(ImmutableMap.Builder<String, Object> builder, SelectedProperty selectedProperty,
                               Object featureId, Map<String, Object> featureProperties) {
    var key = selectedProperty.getProperty().getName();
    if (selectedProperty.getProperty().isIdentifier() && featureId != null) {
      builder.put(key, featureId);
    } else if (selectedProperty.getProperty() instanceof Attribute attribute) {
      processAttribute(builder, selectedProperty, featureProperties, key, attribute);
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
        valueAsList.forEach(
            refProperties -> listBuilder.add(getObject(selectedProperty.getSelectedProperties(), null, refProperties)));
        builder.put(key, listBuilder.build());
      } else if (value instanceof Map) {
        //noinspection unchecked
        builder.put(key, getObject(selectedProperty.getSelectedProperties(), null, (Map<String, Object>) value));
      } else {
        throw new RuntimeException(String.format("Unsupported value type: %s", value.getClass().getSimpleName()));
      }
    }
  }

  private void processAttribute(ImmutableMap.Builder<String, Object> builder, SelectedProperty sp,
                                Map<String, Object> featureProperties, String key, Attribute attribute) {
    var value = featureProperties.get(key);
    if (value != null) {
      switch (attribute.getType().getName()) {
        case "Boolean", "Double", "Float", "Integer", "Long", "String" -> builder.put(key, value);
        default -> throw new RuntimeException(
            String.format("Unsupported attribute type: %s", ((Attribute) sp.getProperty()).getType().getName()));
      }
    }
  }
}
